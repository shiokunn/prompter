package com.prompter.app

import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class M1MatcherTest {
    private fun engine(): PromptEngine {
        val context = RuntimeEnvironment.getApplication() as Context
        return PromptEngine(context)
    }

    @Test
    fun matchesASimpleOfflineSentence() {
        val engine = engine()
        try {
            val result = engine.convert("長い黒髪の少女が青いドレスを着て、椅子に座っている")
            assertTrue(result.tags.contains("1girl"))
            assertTrue(result.tags.contains("long_hair"))
            assertTrue(result.tags.contains("black_hair"))
            assertTrue(result.tags.contains("blue_dress"))
            assertTrue(result.tags.contains("dress"))
            assertTrue(result.tags.contains("on_chair"))
            assertTrue(result.tags.contains("chair"))
            assertTrue(result.tags.contains("sitting"))
            assertTrue(result.pixaiPrompt.contains("a blue dress"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun infersMultipleGirls() {
        val engine = engine()
        try {
            val result = engine.convert("二人の少女がベッドに座っている")
            assertTrue(result.tags.contains("2girls"))
            assertTrue(result.tags.contains("multiple_girls"))
            assertTrue(result.tags.contains("on_bed"))
            assertTrue(result.tags.contains("sitting"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun keepsAdultTags() {
        val engine = engine()
        try {
            val result = engine.convert("裸の少女")
            assertTrue(result.tags.contains("1girl"))
            assertTrue(result.tags.contains("nude"))
        } finally {
            engine.close()
        }
    }
}
