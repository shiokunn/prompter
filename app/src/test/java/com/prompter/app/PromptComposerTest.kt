package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptComposerTest {
    @Test
    fun woodenChairConsumesGenericChairAndGrammarOnlyContext() {
        val prompt = PromptComposer.compose(
            listOf("1girl", "sitting", "on_chair", "chair", "wooden_chair", "night", "city"),
            "Of / in / / / / / /.",
        )
        assertTrue(prompt.contains("on a wooden chair"))
        assertTrue(!prompt.contains("on a chair"))
        assertTrue(!prompt.contains("Additional context"))
        assertTrue(!prompt.contains("Additional visual details: chair"))
    }

    @Test
    fun keepsThePlannedWordRange() {
        val prompt = PromptComposer.compose(
            listOf(
                "1girl", "solo", "long_hair", "black_hair", "blue_dress",
                "dress", "on_chair", "chair", "sitting", "looking_at_viewer",
                "indoors", "light_smile",
            ),
        )
        val words = prompt.split(Regex("\\s+")).count { it.isNotBlank() }
        assertTrue("word count was $words", words in 80..150)
        assertTrue(prompt.contains("a blue dress"))
        assertTrue(prompt.contains("The scene is set indoors and on a chair."))
    }

    @Test
    fun usesPluralSubjectGrammar() {
        val prompt = PromptComposer.compose(
            listOf(
                "2girls", "multiple_girls", "long_hair", "black_hair",
                "twintails", "on_bed", "bed", "sitting", "light_smile", "blush",
            ),
        )
        assertTrue(prompt.startsWith("An anime-style illustration of two girls"))
        assertTrue(prompt.contains("They are sitting."))
        assertTrue(prompt.contains("They have a light smile and a blush."))
    }

    @Test
    fun emitsAdultTagsUnfiltered() {
        val prompt = PromptComposer.compose(
            listOf("1girl", "nude", "nipples", "pussy", "sex", "censored"),
        )
        assertTrue(prompt.contains("nude"))
        assertTrue(prompt.contains("nipples"))
        assertTrue(prompt.contains("pussy"))
        assertTrue(prompt.contains("sex"))
        assertTrue(prompt.contains("censored"))
    }

    @Test
    fun doesNotRepeatRepresentedParentsAsExtras() {
        val prompt = PromptComposer.compose(
            listOf("1girl", "blue_dress", "dress", "on_chair", "chair", "sitting"),
        )
        assertTrue(!prompt.contains("Additional visual details: dress"))
        assertTrue(!prompt.contains("Additional visual details: chair"))
    }

    @Test
    fun emptyInputProducesEmptyPrompt() {
        assertEquals("", PromptComposer.compose(emptyList()))
    }

    @Test
    fun includesTranslatedUnmatchedContext() {
        val prompt = PromptComposer.compose(
            tags = listOf("1girl", "long_hair", "blue_dress"),
            extraContext = "a girl standing on a rooftop at sunset",
        )
        assertTrue(prompt.contains("Additional context from the input: a girl standing on a rooftop at sunset."))
        val words = prompt.split(Regex("\\s+")).count { it.isNotBlank() }
        assertTrue("word count was $words", words in 80..150)
    }

    @Test
    fun contextOnlyProducesAPrompt() {
        val prompt = PromptComposer.compose(
            tags = emptyList(),
            extraContext = "a quiet shrine in the rain",
        )
        assertTrue(prompt.startsWith("An anime-style illustration."))
        assertTrue(prompt.contains("Additional context from the input: a quiet shrine in the rain."))
        val words = prompt.split(Regex("\\s+")).count { it.isNotBlank() }
        assertTrue("word count was $words", words in 80..150)
    }

    @Test
    fun truncatesOverlongContextToTheWordLimit() {
        val prompt = PromptComposer.compose(
            tags = listOf("1girl"),
            extraContext = (1..200).joinToString(" ") { "detail$it" },
        )
        val words = prompt.split(Regex("\\s+")).count { it.isNotBlank() }
        assertTrue("word count was $words", words <= 150)
    }
}
