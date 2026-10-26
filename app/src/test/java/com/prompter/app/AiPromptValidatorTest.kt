package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptValidatorTest {
    private val known = setOf("1girl", "long_hair", "smile", "bow_(weapon)")

    @Test
    fun normalizesAiTagsToDanbooruStyle() {
        assertEquals("long_hair", AiPromptValidator.normalizeTag(" Long Hair "))
        assertEquals("smile", AiPromptValidator.normalizeTag("(smile:1.2)"))
        assertEquals("bow_(weapon)", AiPromptValidator.normalizeTag("bow (weapon)"))
        assertEquals("1girl", AiPromptValidator.normalizeTag("#1girl,"))
    }

    @Test
    fun removesUnknownAndDuplicateTags() {
        val result = AiPromptResult(
            pixaiPrompt = "word ".repeat(200).trim(),
            tags = listOf("1girl", "Long Hair", "made_up_tag", "long_hair", ""),
        )
        val validation = AiPromptValidator.validate(result) { it in known }
        assertEquals(listOf("1girl", "long_hair"), validation.tags)
        assertEquals(listOf("made_up_tag"), validation.removedTags)
        assertEquals(200, validation.wordCount)
        assertTrue(validation.wordCountOk)
    }

    @Test
    fun flagsShortPromptsAndAcceptsLongCloudPrompts() {
        val short = AiPromptValidator.validate(
            AiPromptResult("short prompt", listOf("1girl")),
        ) { it in known }
        assertFalse(short.wordCountOk)

        val belowMinimum = AiPromptValidator.validate(
            AiPromptResult("word ".repeat(150).trim(), listOf("1girl")),
        ) { it in known }
        assertFalse(belowMinimum.wordCountOk)

        val long = AiPromptValidator.validate(
            AiPromptResult("word ".repeat(500).trim(), listOf("1girl")),
        ) { it in known }
        assertTrue(long.wordCountOk)
    }

    @Test
    fun acceptsLongLocalPromptsUpToTheLocalLimit() {
        val validation = AiPromptValidator.validate(
            AiPromptResult("word ".repeat(220).trim(), listOf("1girl")),
            minWords = LocalLlmPrompt.MIN_WORDS,
            maxWords = LocalLlmPrompt.MAX_WORDS,
        ) { it in known }
        assertEquals(220, validation.wordCount)
        assertTrue(validation.wordCountOk)

        val shortForLocal = AiPromptValidator.validate(
            AiPromptResult("word ".repeat(150).trim(), listOf("1girl")),
            minWords = LocalLlmPrompt.MIN_WORDS,
            maxWords = LocalLlmPrompt.MAX_WORDS,
        ) { it in known }
        assertFalse(shortForLocal.wordCountOk)
    }

    @Test
    fun keepsAdultTagsWhenTheyExistInTheDictionary() {
        val adultKnown = known + setOf("nude", "nipples")
        val validation = AiPromptValidator.validate(
            AiPromptResult("word ".repeat(80).trim(), listOf("nude", "nipples")),
        ) { it in adultKnown }
        assertEquals(listOf("nude", "nipples"), validation.tags)
        assertTrue(validation.removedTags.isEmpty())
    }
}
