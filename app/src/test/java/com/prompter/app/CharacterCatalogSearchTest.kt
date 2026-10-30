package com.prompter.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CharacterCatalogSearchTest {
    @Test
    fun ranksExactAliasesAndFiltersByWork() {
        val items = listOf(
            CharacterCandidate("Cinderella", "シンデレラ", "NIKKE", "AniList", "1", workTitles = listOf("Goddess of Victory: NIKKE")),
            CharacterCandidate("Cinderella", "シンデレラ", "Disney", "AniList", "2", workTitles = listOf("Disney Cinderella")),
            CharacterCandidate("Umaru Doma", "土間埋", "Himouto! Umaru-chan", "AniList", "3", aliases = listOf("UMR")),
        )
        assertEquals(listOf("1"), CharacterCatalogSearch.rank(items, "シンデレラ", "ニケ").map { it.url })
        assertEquals("3", CharacterCatalogSearch.rank(items, "UMR", "").first().url)
        assertTrue(CharacterCatalogSearch.rank(items, "Unrelated", "").isEmpty())
        val rezero = CharacterCandidate("Emilia", "エミリア", "Re:Zero kara Hajimeru Isekai Seikatsu", "AniList", "4",
            workTitles = listOf("Re:Zero kara Hajimeru Isekai Seikatsu"))
        assertEquals(listOf(rezero), CharacterCatalogSearch.rank(listOf(rezero), "Emilia", "Re:ゼロから始める異世界生活"))
    }

    @Test
    fun fallsBackToWikidataEvenWhenAniListFailsAndRejectsRealPeople() {
        val calls = mutableListOf<String>()
        val results = CharacterCatalogSearch.search("Cinderella", "") { url, _ ->
            calls.add(url)
            when {
                url.contains("anilist") -> error("HTTP 429")
                url.contains("wbsearchentities") -> JSONObject("""{"search":[{"id":"Q1"},{"id":"Q2"}]}""")
                else -> JSONObject("""{"entities":{
                    "Q1":{"labels":{"en":{"value":"Cinderella"}},"descriptions":{"en":{"value":"fictional character from NIKKE"}}},
                    "Q2":{"labels":{"en":{"value":"A real person"}},"descriptions":{"en":{"value":"American actor"}}}
                }}""")
            }
        }
        assertEquals(3, calls.size)
        assertEquals("Cinderella", results.single().name)
        assertEquals("Wikidata", results.single().source)
    }

    @Test
    fun aniListPreservesJapaneseWorkTitlesAndAlternatives() {
        val data = JSONObject("""{"data":{"Page":{"characters":[{
            "name":{"full":"Umaru Doma","native":"土間埋","alternative":["UMR"]},
            "siteUrl":"https://anilist.co/character/123",
            "media":{"nodes":[{"title":{"english":"Himouto! Umaru-chan","native":"干物妹!うまるちゃん","romaji":null}}]}
        }]}}}""")
        val item = CharacterCatalogSearch.parseAniList(data).single()
        assertTrue(item.workTitles.contains("干物妹!うまるちゃん"))
        assertTrue(item.aliases.contains("UMR"))
        assertEquals(listOf(item), CharacterCatalogSearch.rank(listOf(item), "UMR", "うまるちゃん"))
    }
}
