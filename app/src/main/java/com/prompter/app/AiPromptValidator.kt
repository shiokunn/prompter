package com.prompter.app

import java.util.Locale

data class AiValidation(
    val prompt: String,
    val tags: List<String>,
    val removedTags: List<String>,
    val wordCount: Int,
    val wordCountOk: Boolean,
)

object AiPromptValidator {
    const val MIN_WORDS = 80
    const val MAX_WORDS = 150

    fun validate(
        result: AiPromptResult,
        minWords: Int = MIN_WORDS,
        maxWords: Int = MAX_WORDS,
        isKnownTag: (String) -> Boolean,
    ): AiValidation {
        val prompt = result.pixaiPrompt.trim()
        val words = countWords(prompt)
        val seen = mutableSetOf<String>()
        val valid = mutableListOf<String>()
        val removed = mutableListOf<String>()

        for (raw in result.tags) {
            val tag = normalizeTag(raw)
            if (tag.isEmpty() || !seen.add(tag)) continue
            if (isKnownTag(tag)) {
                valid.add(tag)
            } else {
                removed.add(tag)
            }
        }

        return AiValidation(
            prompt = prompt,
            tags = valid,
            removedTags = removed,
            wordCount = words,
            wordCountOk = words in minWords..maxWords,
        )
    }

    fun normalizeTag(raw: String): String {
        var tag = raw.trim()
            .removeSurrounding("`")
            .removeSurrounding("\"")
            .lowercase(Locale.ROOT)
        WEIGHTED_TAG.matchEntire(tag)?.let { tag = it.groupValues[1] }
        tag = tag.replace(Regex("\\s+"), "_")
        return tag.trim('_', ',', '.', '#')
    }

    fun countWords(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    private val WEIGHTED_TAG = Regex("^\\((.*):[0-9]+(?:\\.[0-9]+)?\\)$")
}
