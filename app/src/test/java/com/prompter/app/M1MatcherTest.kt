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
    @Test
    fun woodenChairRetainsMaterialWithoutAddingATreeOrParticleContext() {
        val engine = engine()
        try {
            assertTrue(engine.tagCount == 10000)
            for (chair in listOf("木の椅子", "木製の椅子", "木製椅子", "木のいす", "木製のいす", "木製いす")) {
                val result = engine.convert("銀髪の少女が、赤いドレスで、夜の街の${chair}に座っている")
                assertTrue(result.tags.containsAll(listOf("wooden_chair", "chair", "sitting", "night", "city", "red_dress")))
                assertTrue("$chair: ${result.tags}", "tree" !in result.tags)
                assertTrue(result.pixaiPrompt.contains("on a wooden chair"))
                assertTrue("$chair: ${result.unmatched}", result.unmatched.isBlank())
                assertTrue(!result.pixaiPrompt.contains("Additional context"))
                val translated = engine.applyTranslation(result, "Of / in / / / / / /.", listOf("tree"))
                assertTrue(translated == result)
            }
            assertTrue("tree" in engine.convert("大きな木と木の椅子に座っている").tags)
        } finally {
            engine.close()
        }
    }

    private fun engine(): PromptEngine {
        val context = RuntimeEnvironment.getApplication() as Context
        return onBackgroundThread { PromptEngine(context) }
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

    @Test
    fun splitCompositionHasTwoGirls() {
        val engine = engine()
        try {
            val result = engine.convert(
                "画面を真ん中で縦半分にした構図で、割った部分の線は壁として描く。" +
                    "右半分には壁に寄りかかっているような姿勢で、学生服を着ている" +
                    "白いボブカットの女の子を描く。左半分にはメイド服を着て、" +
                    "左手でトレーを持って、ジュースを乗せて持っている" +
                    "白いボブカットの女の子を描く。",
            )
            assertTrue(result.tags.contains("2girls"))
            assertTrue(result.tags.contains("multiple_girls"))
            assertTrue(!result.tags.contains("solo"))
            assertTrue(!result.tags.contains("1girl"))
            assertTrue(!result.tags.contains("half_updo"))
        } finally {
            engine.close()
        }
    }

    @Test
    fun upturnedGazeAndCoveredChestUseCorrectTags() {
        val engine = engine()
        try {
            val result = engine.convert(
                "黒ボブ、水色インカラー、胸元を隠す、上目遣い",
            )
            assertTrue(result.tags.contains("looking_up"))
            assertTrue(result.tags.contains("covering_breasts"))
            assertTrue(!result.tags.contains("aiming"))
            assertTrue(!result.tags.contains("cleavage"))
        } finally {
            engine.close()
        }
    }
}
