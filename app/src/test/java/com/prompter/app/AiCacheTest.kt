package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AiCacheTest {
    @Test
    fun storesAndClearsAiResults() {
        val cache = AiCache(RuntimeEnvironment.getApplication())
        cache.clear()
        try {
            val key = cache.key(
                baseUrl = AiDefaults.BASE_URL,
                model = AiDefaults.MODEL,
                input = "長い黒髪の少女",
                offlineTags = listOf("1girl", "long_hair"),
                unmatched = "",
            )
            assertNull(cache.get(key))

            val result = AiPromptResult(
                pixaiPrompt = "A girl with long black hair.",
                tags = listOf("1girl", "long_hair"),
            )
            cache.put(key, result)
            assertEquals(result, cache.get(key))

            val otherKey = cache.key(
                baseUrl = AiDefaults.BASE_URL,
                model = AiDefaults.MODEL,
                input = "別の入力",
                offlineTags = listOf("1girl"),
                unmatched = "",
            )
            assertNotEquals(key, otherKey)
            assertNull(cache.get(otherKey))
        } finally {
            cache.clear()
        }
    }
}
