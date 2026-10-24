package com.prompter.app

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnglishTagNormalizerTest {
    private fun engine(): PromptEngine {
        val context = RuntimeEnvironment.getApplication() as Context
        return PromptEngine(context)
    }

    @Test
    fun mapsEnglishPhrasesToCanonicalTags() {
        val engine = engine()
        try {
            val tags = engine.normalizeEnglishTags(
                "a girl with long black hair wearing a blue dress sitting on a chair on a rooftop at sunset",
            )
            assertTrue(tags.contains("long_hair"))
            assertTrue(tags.contains("black_hair"))
            assertTrue(tags.contains("blue_dress"))
            assertTrue(tags.contains("sitting"))
            assertTrue(tags.contains("chair"))
            assertTrue(tags.contains("rooftop"))
            assertTrue(tags.contains("sunset"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun prefersVeryLongHairOverLongHair() {
        val engine = engine()
        try {
            val tags = engine.normalizeEnglishTags("very long black hair")
            assertTrue(tags.contains("very_long_hair"))
            assertTrue(tags.contains("black_hair"))
            assertTrue(tags.none { it == "long_hair" })
        } finally {
            engine.close()
        }
    }

    @Test
    fun keepsCanonicalTagsWithStopwordEndings() {
        val engine = engine()
        try {
            assertTrue(engine.normalizeEnglishTags("shirt tucked in").contains("shirt_tucked_in"))
            assertTrue(engine.normalizeEnglishTags("k-on!").contains("k-on!"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun mergesTranslationIntoPromptAndTags() {
        val engine = engine()
        try {
            val offline = ConversionResult(
                pixaiPrompt = "",
                tagText = "",
                tags = listOf("1girl"),
                unmatched = "屋上で風に髪がなびく",
            )
            val translated = "a girl on a rooftop with her hair blowing in the wind"
            val normalized = engine.normalizeEnglishTags(translated)
            val merged = engine.applyTranslation(offline, translated, normalized)
            assertTrue(merged.tags.contains("1girl"))
            assertTrue(merged.tags.contains("rooftop"))
            assertTrue(merged.pixaiPrompt.contains("Additional context from the input:"))
            assertTrue(merged.pixaiPrompt.contains("rooftop"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun returnsJapaneseAliasesForDisplay() {
        val engine = engine()
        try {
            val meanings = engine.japaneseMeanings(listOf("long_hair", "blue_dress", "1girl"))
            assertTrue(meanings["long_hair"].orEmpty().isNotBlank())
            assertTrue(meanings["blue_dress"].orEmpty().isNotBlank())
            assertTrue(meanings["1girl"].orEmpty().isNotBlank())
            assertEquals(3, meanings.size)
        } finally {
            engine.close()
        }
    }
}
