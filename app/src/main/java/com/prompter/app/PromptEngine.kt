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
    private val matcher = M1Matcher.load(db)

    val tagCount: Int = matcher.tagCount
    val aliasCount: Int = matcher.aliasCount

    fun convert(input: String): ConversionResult {
        val conversion = matcher.convert(input)
        val tagText = conversion.tags.joinToString(", ")
        val pixai = if (conversion.tags.isEmpty()) "" else PromptComposer.compose(conversion.tags)
        return ConversionResult(
            pixaiPrompt = pixai,
            tagText = tagText,
            tags = conversion.tags,
            unmatched = conversion.unmatched,
        )
    }

    fun close() {
        db.close()
    }
}
