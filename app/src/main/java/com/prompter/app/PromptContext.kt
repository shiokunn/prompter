package com.prompter.app

/** Discard grammar-only gaps without removing unknown visual details. */
internal object PromptContext {
    private val japaneseGrammar = Regex(
        "(?:の|が|は|を|に|で|と|へ|も|や|から|まで|より|です|ます|いる|ある|た|て)+",
    )
    private val englishGrammar = setOf(
        "of", "in", "on", "at", "to", "from", "by", "with", "for",
        "and", "or", "the", "a", "an", "is", "are", "was", "were", "be",
    )

    fun cleanUnmatched(text: String): String = text.split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() && !japaneseGrammar.matches(it) }
        .joinToString(" / ")

    fun cleanTranslation(text: String): String = text.split('/')
        .map { it.trim() }
        .filter { fragment ->
            val words = Regex("[\\p{L}\\p{N}]+").findAll(fragment).map { it.value }.toList()
            words.any { it.lowercase(java.util.Locale.ROOT) !in englishGrammar }
        }
        .joinToString(" / ")
}
