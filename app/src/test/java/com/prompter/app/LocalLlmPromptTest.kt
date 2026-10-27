package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLlmPromptTest {
    @Test
    fun parsesMarkersAndTags() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            A girl with long black hair sits by a window, holding a white cup.
            PROMPT_END
            TAGS: 1girl, long_hair, black_hair, window, cup
            """.trimIndent(),
        )

        assertEquals(
            "A girl with long black hair sits by a window, holding a white cup.",
            result.pixaiPrompt,
        )
        assertEquals(
            listOf("1girl", "long_hair", "black_hair", "window", "cup"),
            result.tags,
        )
    }

    @Test
    fun parsesWithoutMarkersAndKeepsPrompt() {
        val result = LocalLlmPrompt.parse(
            """
            Prompt: A quiet shrine in the rain, with a girl in a white dress.
            TAGS: 1girl, white_dress, shrine, rain
            """.trimIndent(),
        )

        assertEquals(
            "A quiet shrine in the rain, with a girl in a white dress.",
            result.pixaiPrompt,
        )
        assertEquals(listOf("1girl", "white_dress", "shrine", "rain"), result.tags)
    }

    @Test
    fun stripsMarkdownFence() {
        val result = LocalLlmPrompt.parse(
            """
            ```text
            PROMPT_BEGIN
            An anime-style portrait of a girl under a streetlight.
            PROMPT_END
            TAGS: 1girl, streetlight
            ```
            """.trimIndent(),
        )

        assertEquals(
            "An anime-style portrait of a girl under a streetlight.",
            result.pixaiPrompt,
        )
        assertEquals(listOf("1girl", "streetlight"), result.tags)
    }

    @Test
    fun parsesTagsOnTheNextLine() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            A girl stands on a rooftop at sunset.
            PROMPT_END
            TAGS:
            1girl, rooftop, sunset
            """.trimIndent(),
        )

        assertEquals("A girl stands on a rooftop at sunset.", result.pixaiPrompt)
        assertEquals(listOf("1girl", "rooftop", "sunset"), result.tags)
    }

    @Test
    fun rejectsJapaneseInTheGeneratedPrompt() {
        assertThrows(LocalLlmException::class.java) {
            LocalLlmPrompt.parse(
                """
                PROMPT_BEGIN
                黒髪の少女が窓のそばに座っている。
                PROMPT_END
                TAGS: 1girl, black_hair
                """.trimIndent(),
            )
        }
    }

    @Test
    fun rejectsARefusal() {
        assertThrows(LocalLlmException::class.java) {
            LocalLlmPrompt.parse(
                """
                PROMPT_BEGIN
                I'm sorry, but I cannot create that prompt.
                PROMPT_END
                TAGS: 1girl
                """.trimIndent(),
            )
        }
    }

    @Test
    fun rejectsAnUnfilledTemplate() {
        assertThrows(LocalLlmException::class.java) {
            LocalLlmPrompt.parse(
                """
                PROMPT_BEGIN
                <English prompt>
                PROMPT_END
                TAGS: 1girl
                """.trimIndent(),
            )
        }
    }

    @Test
    fun rejectsLeftoverControlMarkers() {
        assertThrows(LocalLlmException::class.java) {
            LocalLlmPrompt.parse(
                """
                PROMPT_BEGIN
                A girl sits by a window. PROMPT_BEGIN
                PROMPT_END
                TAGS: 1girl, window
                """.trimIndent(),
            )
        }
    }

    @Test
    fun dropsJapaneseAndBracketedTags() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            A girl sits by a window.
            PROMPT_END
            TAGS: [1girl, long_hair], 黒髪, window
            """.trimIndent(),
        )

        assertEquals(listOf("1girl", "long_hair", "window"), result.tags)
    }

    @Test
    fun rejectsAnEmptyResponse() {
        assertThrows(LocalLlmException::class.java) {
            LocalLlmPrompt.parse("   ")
        }
    }

    @Test
    fun userPromptCarriesInputTagsAndUnmatchedText() {
        val prompt = LocalLlmPrompt.buildUserPrompt(
            input = "黒髪の少女が窓のそばに座っている",
            offlineTags = listOf("1girl", "long_hair", "window"),
            unmatched = "窓のそばに座っている",
        )

        assertTrue(prompt.contains("黒髪の少女が窓のそばに座っている"))
        assertTrue(prompt.contains("1girl, long_hair, window"))
        assertTrue(prompt.contains("窓のそばに座っている"))
        assertFalse(LocalLlmPrompt.containsJapanese("A girl with long black hair."))
    }

    @Test
    fun systemPromptRequiresExpansionRatherThanTranslation() {
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Do not merely translate"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("200 to 240 words"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("15 to 25 words"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("10 to 12 sentences"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Never return fewer than 180 words"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Setting and background objects"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Art style, rendering, and finish"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Do not add a clothing color"))
    }

    @Test
    fun systemPromptPinsFluentRewritesForKnownWeakSpots() {
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("inner-color"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("a faint smile"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Never write \"mixed with\""))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("center of her face"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("never a word-for-word translation"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("When one trait applies to several people"))
    }

    @Test
    fun systemPromptForbidsInventingColors() {
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Do not add a clothing color"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Do not add an eye color"))
        assertTrue(LocalLlmPrompt.SYSTEM_PROMPT.contains("Never drop a stated detail"))
        assertFalse(LocalLlmPrompt.SYSTEM_PROMPT.contains("light blue"))
    }

    @Test
    fun userPromptRepeatsTheLengthAndFidelityRules() {
        val prompt = LocalLlmPrompt.buildUserPrompt(
            input = "白いドレスを着た少女が野原に立って、こちらを見ている",
            offlineTags = listOf("1girl", "white_dress", "field"),
            unmatched = "",
        )

        assertTrue(prompt.contains("200 to 240 words"))
        assertTrue(prompt.contains("10 to 12 sentences"))
        assertTrue(prompt.contains("Vary sentence length"))
        assertTrue(prompt.contains("Do not add any detail not stated there"))
        assertTrue(prompt.contains("Do not invent hair length, eye color, clothing colors"))
    }

    @Test
    fun collapsesATwoStateExpressionIntoOneNaturalSmile() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            She has a gentle smile mixed with a neutral expression in the center of her face.
            PROMPT_END
            TAGS: 1girl
            """.trimIndent(),
        )

        assertEquals("She has a faint smile.", result.pixaiPrompt)
    }

    @Test
    fun collapsesReverseTwoStateExpressions() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            She wears a gentle, neutral expression mixed with a subtle smile on her face.
            PROMPT_END
            TAGS: 1girl
            """.trimIndent(),
        )

        assertEquals("She wears a faint smile on her face.", result.pixaiPrompt)
    }

    @Test
    fun collapsesLongerTwoStateExpressions() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            Her expression is a bright smile mixed with a neutral, gentle demeanor while looking back.
            PROMPT_END
            TAGS: 1girl
            """.trimIndent(),
        )

        assertEquals("Her expression is a faint smile while looking back.", result.pixaiPrompt)
    }

    @Test
    fun leavesAnOrdinarySmileAlone() {
        val result = LocalLlmPrompt.parse(
            """
            PROMPT_BEGIN
            A girl with a soft smile stands by the window.
            PROMPT_END
            TAGS: 1girl, window
            """.trimIndent(),
        )

        assertEquals("A girl with a soft smile stands by the window.", result.pixaiPrompt)
    }

    @Test
    fun promptVersionIsBumpedWhenExpansionRulesChange() {
        assertEquals("local:gemma-4-E2B-v11", LocalModelSpec.CACHE_KEY)
    }

    @Test
    fun modelMetadataIsPinnedForReproducibleDownloads() {
        assertEquals(2_588_147_712L, LocalModelSpec.SIZE_BYTES)
        assertEquals(64, LocalModelSpec.SHA256.length)
        assertTrue(LocalModelSpec.DOWNLOAD_URL.contains(LocalModelSpec.REVISION))
        assertTrue(LocalModelSpec.DOWNLOAD_URL.endsWith(LocalModelSpec.FILE_NAME))
    }
}
