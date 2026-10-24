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
    private val knownTags: Set<String>,
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

        val attributes = attributeTags(normalized)
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
        val matched = found.entries.sortedBy { it.value }.map { it.key }
        return (attributes + matched).distinct()
    }

    /**
     * Natural English puts adjectives between the attribute and the noun
     * ("long black hair"), while Danbooru uses separate tags. These targeted
     * rules recover the common hair/eye attributes without fuzzy-matching the
     * whole dictionary.
     */
    private fun attributeTags(normalized: String): List<String> {
        val tokens = normalized.split(' ')
        val out = LinkedHashSet<String>()
        for (i in tokens.indices) {
            when (tokens[i]) {
                "hair" -> {
                    val before = tokens.subList(maxOf(0, i - 3), i)
                    when {
                        "very" in before && "long" in before -> addIfKnown(out, "very_long_hair")
                        "long" in before -> addIfKnown(out, "long_hair")
                        "medium" in before -> addIfKnown(out, "medium_hair")
                        "short" in before -> addIfKnown(out, "short_hair")
                    }
                    for (word in before) {
                        val color = HAIR_COLORS[word] ?: continue
                        addIfKnown(out, "${color}_hair")
                    }
                }
                "eyes" -> {
                    for (word in tokens.subList(maxOf(0, i - 2), i)) {
                        val color = EYE_COLORS[word] ?: continue
                        addIfKnown(out, "${color}_eyes")
                    }
                }
            }
        }
        return out.toList()
    }

    private fun addIfKnown(out: MutableSet<String>, tag: String) {
        if (tag in knownTags) out.add(tag)
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
            return EnglishTagNormalizer(root, knownTags)
        }

        private val HAIR_COLORS = mapOf(
            "black" to "black",
            "brown" to "brown",
            "blonde" to "blonde",
            "blue" to "blue",
            "pink" to "pink",
            "red" to "red",
            "white" to "white",
            "silver" to "silver",
            "grey" to "grey",
            "gray" to "grey",
            "green" to "green",
            "purple" to "purple",
            "orange" to "orange",
            "aqua" to "aqua",
            "gold" to "gold",
            "yellow" to "yellow",
        )

        private val EYE_COLORS = mapOf(
            "black" to "black",
            "brown" to "brown",
            "blue" to "blue",
            "pink" to "pink",
            "red" to "red",
            "white" to "white",
            "grey" to "grey",
            "gray" to "grey",
            "green" to "green",
            "purple" to "purple",
            "orange" to "orange",
            "aqua" to "aqua",
            "gold" to "gold",
            "yellow" to "yellow",
        )

        private val NON_WORD = Regex("[^a-z0-9]+")

        private fun normalizeEnglish(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace(NON_WORD, " ")
                .trim()
                .replace(Regex("\\s+"), " ")
    }
}
