package com.prompter.app

import android.database.sqlite.SQLiteDatabase
import java.text.Normalizer
import java.util.Locale

/**
 * Maps English text produced by ML Kit back to canonical Danbooru tags.
 *
 * The dictionary contains both canonical tag names and English aliases. The
 * matcher uses a trie with word-boundary checks so "hair" does not match inside
 * "chair" and "long hair" wins over the shorter "hair".
 */
class EnglishTagNormalizer private constructor(
    private val root: Node,
) {
    private class Node {
        val children = HashMap<Char, Node>()
        var entries: MutableList<Entry>? = null
    }

    private data class Entry(
        val tag: String,
        val surface: String,
        val priority: Int,
        val confidence: Double,
        val postCount: Long,
    )

    fun normalize(text: String): List<String> {
        val normalized = normalizeEnglish(text)
        if (normalized.isBlank()) return emptyList()

        val found = LinkedHashMap<String, Int>()
        var i = 0
        while (i < normalized.length) {
            var node = root
            var j = i
            var bestEnd = -1
            var bestEntries: MutableList<Entry>? = null

            while (j < normalized.length) {
                val next = node.children[normalized[j]] ?: break
                node = next
                j += 1
                val entries = node.entries
                if (entries != null && (j == normalized.length || normalized[j] == ' ')) {
                    bestEnd = j
                    bestEntries = entries
                }
            }

            if (bestEntries != null && bestEnd > i) {
                val best = bestEntries.maxWithOrNull(
                    compareBy<Entry> { it.priority }
                        .thenBy { it.confidence }
                        .thenBy { it.postCount }
                        .thenBy { it.surface.length },
                )
                if (best != null) {
                    if (best.tag !in found) found[best.tag] = i
                    i = bestEnd
                    while (i < normalized.length && normalized[i] == ' ') i++
                    continue
                }
            }

            while (i < normalized.length && normalized[i] != ' ') i++
            while (i < normalized.length && normalized[i] == ' ') i++
        }
        return found.entries.sortedBy { it.value }.map { it.key }
    }

    companion object {
        fun load(db: SQLiteDatabase, knownTags: Set<String>): EnglishTagNormalizer {
            val root = Node()

            fun insert(
                surface: String,
                tag: String,
                priority: Int,
                confidence: Double,
                postCount: Long,
            ) {
                val norm = normalizeEnglish(surface)
                if (norm.isBlank()) return
                var node = root
                for (ch in norm) {
                    node = node.children.getOrPut(ch) { Node() }
                }
                val list = node.entries ?: mutableListOf<Entry>().also { node.entries = it }
                list.add(Entry(tag, norm, priority, confidence, postCount))
            }

            for (tag in knownTags) {
                insert(
                    surface = tag.replace('_', ' '),
                    tag = tag,
                    priority = 10,
                    confidence = 0.9,
                    postCount = 0L,
                )
            }

            db.rawQuery(
                "SELECT t.name, a.surface, a.confidence, a.priority, t.post_count " +
                    "FROM aliases a JOIN tags t ON t.id=a.tag_id WHERE a.lang='en'",
                null,
            ).use { c ->
                while (c.moveToNext()) {
                    insert(
                        surface = c.getString(1),
                        tag = c.getString(0),
                        priority = c.getInt(3),
                        confidence = c.getDouble(2),
                        postCount = c.getLong(4),
                    )
                }
            }
            return EnglishTagNormalizer(root)
        }

        private val NON_WORD = Regex("[^a-z0-9]+")

        private fun normalizeEnglish(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace(NON_WORD, " ")
                .trim()
                .replace(Regex("\\s+"), " ")
    }
}
