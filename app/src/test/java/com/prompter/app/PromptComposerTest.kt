package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptComposerTest {
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
}
