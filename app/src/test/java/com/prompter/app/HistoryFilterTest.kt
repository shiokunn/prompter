package com.prompter.app

import org.junit.Assert.*
import org.junit.Test

class HistoryFilterTest {
    @Test
    fun searchesSceneCharacterAndWorkWithoutModifyingRows() {
        val item = HistoryItem(1, "青い服", "A girl", "blue_dress", "", 0, false,
            "シンデレラ", "NIKKE", "Cinderella")
        for (q in listOf("青い", "CINDERELLA", "nikke", "blue_dress")) {
            assertEquals(listOf(item), filterHistory(listOf(item), q))
        }
        assertTrue(filterHistory(listOf(item), "原神").isEmpty())
    }
}
