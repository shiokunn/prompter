package com.prompter.app

import android.content.Context

data class ConversionResult(
    val pixaiPrompt: String,
    val tagText: String,
    val tags: List<String>,
    val unmatched: String,
)

class PromptEngine(context: Context) {
    private val db = PromptDb.open(context)
    private val updates = DictionaryUpdates.snapshot(context)
    private val matcher = M1Matcher.load(db, updates.aliases)
    private val englishNormalizer = EnglishTagNormalizer.load(db, matcher.allTagNames)

    val tagCount: Int = matcher.tagCount
    val aliasCount: Int = matcher.aliasCount
    val dictionaryVersion: Long = updates.version
    val dictionaryAliasCount: Int = updates.aliasCount
    val dictionarySource: String = updates.sourceTag

    fun isKnownTag(tag: String): Boolean = matcher.isKnownTag(tag)

    fun convert(input: String): ConversionResult {
        val conversion = matcher.convert(input)
        val tags = FidelityGuard.correctTags(
            input = input,
            tags = conversion.tags,
            isKnownTag = matcher::isKnownTag,
            preserveStatedColors = false,
        ).tags
        val tagText = tags.joinToString(", ")
        val pixai = if (tags.isEmpty()) "" else PromptComposer.compose(tags)
        return ConversionResult(
            pixaiPrompt = pixai,
            tagText = tagText,
            tags = tags,
            unmatched = conversion.unmatched,
        )
    }

    /**
     * Finds canonical tags in English text returned by ML Kit. This is the
     * English-to-tag normalization step; unknown words are ignored.
     */
    fun normalizeEnglishTags(text: String): List<String> = englishNormalizer.normalize(text)

    /**
     * Merges the translated unmatched fragment into the offline result. New
     * tags found by the English normalizer are prepended to the existing tags,
     * and the translation is kept as extra natural-language context.
     */
    fun applyTranslation(
        offline: ConversionResult,
        translatedText: String,
        normalizedTags: List<String>,
    ): ConversionResult {
        if (translatedText.isBlank()) return offline
        val tags = (normalizedTags + offline.tags).distinct()
        return ConversionResult(
            pixaiPrompt = PromptComposer.compose(tags, translatedText),
            tagText = tags.joinToString(", "),
            tags = tags,
            unmatched = offline.unmatched,
        )
    }

    /**
     * Returns short Japanese aliases for the tag chips. The query is limited to
     * the tags currently on screen, so it stays cheap even with a 5,000-tag DB.
     */
    fun japaneseMeanings(tags: List<String>): Map<String, String> {
        val unique = tags.distinct()
        if (unique.isEmpty()) return emptyMap()
        val placeholders = unique.joinToString(",") { "?" }
        val found = LinkedHashMap<String, MutableList<String>>()
        db.rawQuery(
            "SELECT t.name, a.surface FROM aliases a JOIN tags t ON t.id=a.tag_id " +
                "WHERE t.name IN ($placeholders) AND a.lang='ja' " +
                "ORDER BY a.priority DESC, a.confidence DESC, a.surface",
            unique.toTypedArray(),
        ).use { c ->
            while (c.moveToNext()) {
                val tag = c.getString(0)
                val surface = c.getString(1)
                val list = found.getOrPut(tag) { mutableListOf() }
                if (list.size < 2 && surface !in list) list.add(surface)
            }
        }
        return found.mapValues { (_, surfaces) -> surfaces.joinToString(" / ") }
    }

    fun close() {
        db.close()
    }
}
