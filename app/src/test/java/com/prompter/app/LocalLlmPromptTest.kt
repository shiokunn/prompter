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
    fun modelMetadataIsPinnedForReproducibleDownloads() {
        assertEquals(2_588_147_712L, LocalModelSpec.SIZE_BYTES)
        assertEquals(64, LocalModelSpec.SHA256.length)
        assertTrue(LocalModelSpec.DOWNLOAD_URL.contains(LocalModelSpec.REVISION))
        assertTrue(LocalModelSpec.DOWNLOAD_URL.endsWith(LocalModelSpec.FILE_NAME))
    }
}
