package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharacterDbTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun normalizesWidthCaseAndSeparators() {
        assertEquals("神里綾華", CharacterDb.normalize(" 神里・綾華 "))
        assertEquals("umr", CharacterDb.normalize("ＵＭＲ"))
    }

    @Test
    fun findsCharactersByJapaneseName() {
        val hit = CharacterDb.search(context, "綾華").first()
        assertEquals("神里綾華", hit.nameJa)
        assertEquals("Kamisato Ayaka", hit.nameEn)
        assertEquals("原神", hit.series)
    }

    @Test
    fun findsCharactersByEnglishAndNickname() {
        assertTrue(CharacterDb.search(context, "ayaka").any { it.nameJa == "神里綾華" })
        assertTrue(CharacterDb.search(context, "UMR").any { it.nameJa == "土間埋" })
    }

    @Test
    fun kanaVariantsWorkAliasesAndLiteralWildcards() {
        assertTrue(CharacterDb.search(context, "しんでれら", series = "ニケ").any { it.nameEn == "Cinderella" })
        assertTrue(CharacterDb.search(context, "ｼﾝﾃﾞﾚﾗ", series = "NIKKE").any { it.nameEn == "Cinderella" })
        assertTrue(CharacterDb.search(context, "Cinderella", series = "原神").isEmpty())
        assertTrue(CharacterDb.search(context, "%").isEmpty())
        assertEquals("神里綾華", CharacterDb.search(context, "綾華", series = "Genshin Impact").first().nameJa)
        assertEquals("Cinderella", CharacterDb.search(context, "Cinderella").first().nameEn)
    }

    @Test
    fun noteTextIsNotStoredAsAnAlias() {
        assertTrue(CharacterDb.search(context, "二次資料").isEmpty())
    }

    @Test
    fun keepsAlternateFormsSeparateFromBaseCharacter() {
        val results = CharacterDb.search(context, "シンデレラ")
        val base = results.filter { it.category == "base" }
        val variant = results.filter { it.category == "variant" }
        assertEquals(1, base.size)
        assertTrue(variant.any { it.nameEn == "Cinderella: Crystal Wave" })
        assertTrue(base.none { it.nameEn == "Cinderella: Crystal Wave" })
    }
}
