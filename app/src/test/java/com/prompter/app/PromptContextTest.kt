package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PromptContextTest {
    @Test
    fun removesOnlyGrammarGaps() {
        assertEquals("", PromptContext.cleanUnmatched("の / が / で / の / に / いる /"))
        assertEquals("ほのぼの / 海辺の霧", PromptContext.cleanUnmatched("の / ほのぼの / 海辺の霧 / いる"))
    }

    @Test
    fun filtersTranslatedGrammarWithoutLosingVisualDetails() {
        assertEquals("", PromptContext.cleanTranslation("Of / in / / / / / /."))
        assertEquals("mist by the sea", PromptContext.cleanTranslation("Of / mist by the sea / in /."))
        assertEquals("a wooden chair", PromptContext.cleanTranslation("a wooden chair"))
    }
}
