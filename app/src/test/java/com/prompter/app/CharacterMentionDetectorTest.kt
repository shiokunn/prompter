package com.prompter.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharacterMentionDetectorTest {
    private val context get() = RuntimeEnvironment.getApplication()

    private fun detect(text: String) = CharacterMentionDetector.detect(context, text)

    @Test
    fun detectsAFullJapaneseName() {
        val results = detect("桜の下で神里綾華が舞う")
        assertTrue(results.any { it.nameJa == "神里綾華" && it.series == "原神" })
    }

    @Test
    fun ignoresGenericShortWordsWithoutAWorkName() {
        val results = detect("夜の街で少女が花火を見上げ、胡桃を食べた。")
        assertTrue(results.isEmpty())
    }

    @Test
    fun oneCharacterNamesAreNeverSuggested() {
        assertTrue(detect("原神の空が飛ぶ").none { it.nameJa == "空" })
    }

    @Test
    fun twoCharacterNamesRequireTheirWork() {
        assertTrue(detect("胡桃を食べた").none { it.nameJa == "胡桃" })
        assertTrue(detect("原神の胡桃が笑う").any { it.nameJa == "胡桃" })
        assertTrue(detect("原神：胡桃が笑う").any { it.nameJa == "胡桃" })
        assertTrue(detect("原神 胡桃が笑う").any { it.nameJa == "胡桃" })
    }

    @Test
    fun shortNameInsideACompoundIsNotSuggested() {
        assertTrue(
            detect("崩壊：スターレイルの花火大会").none { it.nameJa == "花火" },
        )
    }

    @Test
    fun englishNamesRequireWordBoundaries() {
        assertTrue(detect("Rapid response team").none { it.nameEn == "Rapi" })
        assertTrue(detect("Anise is a spice").none { it.nameEn == "Anis" })
        assertTrue(detect("Rapi is on the roof").any { it.nameEn == "Rapi" })
    }

    @Test
    fun katakanaMatchDoesNotCrossIntoHiragana() {
        assertTrue(
            detect("NIKKEのヴェスナが笑う").none { it.nameJa == "ナガ" },
        )
    }

    @Test
    fun workNameFiltersSameNameAcrossWorks() {
        val results = detect("NIKKEのアリスが笑う")
        assertTrue(results.any { it.nameJa == "アリス" && it.series == "勝利の女神：NIKKE" })
        assertTrue(results.none { it.series == "原神" || it.series == "ゼンレスゾーンゼロ" })
    }

    @Test
    fun ambiguousNameReturnsAllCandidatesWithoutChoosingOne() {
        val results = detect("アリスが笑う")
        assertTrue(results.count { it.nameJa == "アリス" } >= 2)
    }

    @Test
    fun longestMatchWinsOverBaseAndShortAliases() {
        val results = detect("崩壊：スターレイルの丹恒・飲月が現れる")
        assertTrue(results.any { it.nameJa == "丹恒・飲月" })
        assertFalse(results.any { it.nameJa == "丹恒" })
    }

    @Test
    fun englishLongNameNeedsNoWorkContext() {
        val results = detect("Cinderella is dancing")
        assertTrue(results.any { it.nameEn == "Cinderella" })
        assertTrue(results.none { it.category == "variant" })
    }

    @Test
    fun fullVariantNameWinsOverTheBaseName() {
        val results = detect("Cinderella: Crystal Wave is dancing")
        assertTrue(results.any { it.nameEn == "Cinderella: Crystal Wave" })
        assertTrue(results.none { it.nameEn == "Cinderella" })
    }
}
