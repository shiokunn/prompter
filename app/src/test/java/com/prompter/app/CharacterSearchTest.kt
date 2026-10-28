package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSearchTest {
    @Test
    fun extractsOnlyFirstTwoResultsAndUnwrapsLinks() {
        val html = """
            <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2Fone&amp;rut=x">Cinderella &amp; Nikke</a>
            <a class="result__a" href="https://example.org/two">日本語Wiki</a>
            <a class="result__a" href="https://example.org/three">Third</a>
        """.trimIndent()
        assertEquals(
            listOf(CharacterSearchResult("Cinderella & Nikke", "https://example.org/one"),
                CharacterSearchResult("日本語Wiki", "https://example.org/two")),
            CharacterSearch.parseResults(html),
        )
    }

    @Test
    fun unsafeFirstResultDoesNotPullInThird() {
        val html = """
            <a class="result__a" href="javascript:alert(1)">Bad</a>
            <a class="result__a" href="https://example.org/two">Second</a>
            <a class="result__a" href="https://example.org/three">Third</a>
        """.trimIndent()
        assertEquals(listOf(CharacterSearchResult("Second", "https://example.org/two")),
            CharacterSearch.parseResults(html))
    }
}
