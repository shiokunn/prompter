package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun parsesLiteMarkupAndIgnoresAds() {
        val html = """<a class="result-sponsored" href="https://ad.example">Ad</a>
            <a class="result-link" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2Fone">One</a>
            <a class="result-link" href="https://example.org/two">Two</a>"""
        assertEquals(listOf(CharacterSearchResult("One", "https://example.org/one"),
            CharacterSearchResult("Two", "https://example.org/two")), CharacterSearch.interpret(html))
    }

    @Test
    fun distinguishesChallengeNoResultsAndUnknownMarkup() {
        listOf(
            "<form id='challenge-form' action='/anomaly.js'></form>" to SearchFailure.CHALLENGE,
            "<div class='no-results'>No results found</div>" to SearchFailure.NO_RESULTS,
            "<html><p>New layout</p></html>" to SearchFailure.MARKUP,
        ).forEach { (html, reason) ->
            val failure = assertThrows(CharacterSearchException::class.java) { CharacterSearch.interpret(html) }
            assertEquals(reason, failure.reason)
        }
    }
}
