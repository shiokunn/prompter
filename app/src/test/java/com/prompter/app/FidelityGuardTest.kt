package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FidelityGuardTest {
    private fun known(tags: Iterable<String>): (String) -> Boolean {
        val all = tags.toSet()
        return { it in all }
    }

    @Test
    fun splitCompositionBecomesTwoGirls() {
        val input = "画面を真ん中で縦半分にした構図で、割った部分の線は壁として描く。" +
            "右半分には壁に寄りかかっているような姿勢で、学生服を着ている" +
            "白いボブカットの女の子を描く。左半分にはメイド服を着て、" +
            "左手でトレーを持って、ジュースを乗せて持っている" +
            "白いボブカットの女の子を描く。"
        val tags = listOf(
            "solo", "1girl", "school_uniform", "bob_cut", "maid", "holding", "half_updo",
        )
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags + listOf("2girls", "multiple_girls")),
        )

        assertFalse(result.tags.contains("solo"))
        assertFalse(result.tags.contains("1girl"))
        assertFalse(result.tags.contains("half_updo"))
        assertTrue(result.tags.contains("2girls"))
        assertTrue(result.tags.contains("multiple_girls"))
    }

    @Test
    fun threePanelSingleCharacterDoesNotBecomeMultipleGirls() {
        val input = "白髪赤目、メイド、オムライス、3分割、料理工程"
        val tags = listOf(
            "white_hair", "red_eyes", "maid", "food", "cooking", "multiple_girls",
        )
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags + listOf("1girl", "solo")),
        )

        assertFalse(result.tags.contains("multiple_girls"))
        assertTrue(result.tags.contains("1girl"))
        assertTrue(result.tags.contains("solo"))
    }

    @Test
    fun coveredChestRemovesCleavage() {
        val input = "白髪赤目、メイド、オムライス、3分割、胸元を隠す、中央提示"
        val tags = listOf("1girl", "maid", "cleavage", "hiding", "breasts")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags + listOf("covering_breasts", "solo")),
        )

        assertFalse(result.tags.contains("cleavage"))
        assertTrue(result.tags.contains("covering_breasts"))
        assertTrue(result.tags.contains("breasts"))
    }

    @Test
    fun halfAndUpturnedGazeAreNotMisread() {
        val input = "黒ボブ、水色インカラー、上目遣い"
        val tags = listOf("1girl", "solo", "aiming", "half_updo")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags),
        )

        assertFalse(result.tags.contains("aiming"))
        assertFalse(result.tags.contains("half_updo"))
    }

    @Test
    fun realHalfUpdoAndAimingCueAreKept() {
        val input = "黒髪をハーフアップにした少女が弓を構えている"
        val tags = listOf("1girl", "solo", "aiming", "half_updo")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags),
        )

        assertTrue(result.tags.contains("aiming"))
        assertTrue(result.tags.contains("half_updo"))
    }

    @Test
    fun sideViewDoesNotKeepHighAngleTag() {
        val input = "左側から見た構図、上目遣い"
        val tags = listOf("1girl", "solo", "from_above")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags + listOf("from_side")),
        )

        assertFalse(result.tags.contains("from_above"))
        assertTrue(result.tags.contains("from_side"))
    }

    @Test
    fun unstatedClothingColorTagsAreRemoved() {
        val input = "学生服を着た少女"
        val tags = listOf("1girl", "school_uniform", "white_shirt", "black_pants")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags),
            statedTags = listOf("1girl", "school_uniform"),
        )

        assertFalse(result.tags.contains("white_shirt"))
        assertFalse(result.tags.contains("black_pants"))
    }

    @Test
    fun statedClothingColorTagIsKept() {
        val input = "白いシャツを着た少女"
        val tags = listOf("1girl", "white_shirt")
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags),
            statedTags = listOf("1girl"),
        )

        assertTrue(result.tags.contains("white_shirt"))
    }

    @Test
    fun featureOnlyInputIsSingleAndHasNoInventedClothingTags() {
        val input = "明るい黄色の髪の毛、紫の目、ロングヘアー、"
        val tags = listOf(
            "blonde_hair", "purple_eyes", "long_hair", "school_uniform", "multiple_girls",
        )
        val result = FidelityGuard.correctTags(
            input = input,
            tags = tags,
            isKnownTag = known(tags + listOf("1girl", "solo")),
            statedTags = listOf("blonde_hair", "purple_eyes"),
        )

        assertFalse(result.tags.contains("multiple_girls"))
        assertFalse(result.tags.contains("school_uniform"))
        assertTrue(result.tags.contains("1girl"))
        assertTrue(result.tags.contains("solo"))
        assertTrue(result.tags.contains("long_hair"))
    }

    @Test
    fun promptFeatureOnlyInputDoesNotBecomeTwoGirlsOrAWearerOfUniforms() {
        val input = "明るい黄色の髪の毛、紫の目、ロングヘアー、"
        val prompt = "Two girls are featured in this scene. They are dressed in their " +
            "respective school uniforms. Their eyes are a vivid purple."
        val result = FidelityGuard.sanitizePrompt(
            input = input,
            prompt = prompt,
            offlineTags = listOf("blonde_hair", "purple_eyes"),
        )

        assertFalse(result.contains("Two girls"))
        assertFalse(result.contains("school uniform"))
        assertFalse(result.contains("They are"))
        assertFalse(result.contains("Their"))
        assertTrue(result.contains("A girl is featured"))
        assertTrue(result.contains("Her eyes are a vivid purple"))
    }

    @Test
    fun promptDoesNotInventHairEyesOrClothing() {
        val input = "白いドレスを着た少女が野原に立っている"
        val prompt = "A girl with long, straight blonde hair and blue eyes wears a white " +
            "dress in a field. She also has a white apron and black trousers."
        val result = FidelityGuard.sanitizePrompt(
            input = input,
            prompt = prompt,
            offlineTags = listOf("1girl", "white_dress", "dress", "field", "standing"),
        )

        assertFalse(result.contains("long"))
        assertFalse(result.contains("straight"))
        assertFalse(result.contains("blonde"))
        assertFalse(result.contains("blue eyes"))
        assertTrue(result.contains("white dress"))
        assertFalse(result.contains("white apron"))
        assertFalse(result.contains("black trousers"))
    }

    @Test
    fun promptChestIsMadeConsistent() {
        val input = "メイド、胸元を隠す"
        val prompt = "She wears a maid outfit with a deep cleavage and exposed breasts."
        val result = FidelityGuard.sanitizePrompt(
            input = input,
            prompt = prompt,
            offlineTags = listOf("1girl", "maid", "covering_breasts"),
        )

        assertFalse(result.contains("cleavage"))
        assertFalse(result.contains("exposed breasts"))
        assertTrue(result.contains("modest neckline"))
        assertTrue(result.contains("covered chest"))
    }

    @Test
    fun promptDropsUnstatedOutfitSentence() {
        val input = "二人の少女がベッドに座って、軽く微笑んでいる"
        val prompt = "Two girls sit on a bed. They are both wearing their respective " +
            "outfits, which are simple and comfortable. Their smiles are gentle."
        val result = FidelityGuard.sanitizePrompt(
            input = input,
            prompt = prompt,
            offlineTags = listOf("2girls", "multiple_girls", "bed", "sitting", "smile"),
        )

        assertFalse(result.contains("outfits"))
        assertFalse(result.contains("wearing"))
        assertTrue(result.contains("Two girls sit on a bed"))
        assertTrue(result.contains("smiles are gentle"))
    }

    @Test
    fun promptRemovesNotSpecifiedMetaCommentary() {
        val input = "白いドレスを着た少女が野原に立って、こちらを見ている"
        val prompt = "Her hair is not specified, so it is described simply as her hair, " +
            "falling naturally around her shoulders. Her eyes are not specified, so she " +
            "is left as a neutral gaze directed straight out of the frame. Her hands are " +
            "not specified, so she hang naturally by her sides."
        val result = FidelityGuard.sanitizePrompt(
            input = input,
            prompt = prompt,
            offlineTags = listOf("1girl", "white_dress", "dress", "field", "standing"),
        )

        assertFalse(result.contains("not specified"))
        assertTrue(result.contains("Her hair falls naturally"))
        assertTrue(result.contains("Her gaze is directed"))
        assertTrue(result.contains("Her hands hang naturally"))
    }

    @Test
    fun tagsRemainInOrderWhenNothingChanges() {
        val tags = listOf("1girl", "solo", "blue_dress")
        val result = FidelityGuard.correctTags(
            input = "青いドレスを着た少女",
            tags = tags,
            isKnownTag = known(tags + listOf("covering_breasts")),
        )

        assertEquals(tags, result.tags)
    }
}
