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
class HistoryStoreTest {
    @Test
    fun characterFieldsAreSavedAndRestored() {
        val store = HistoryStore(RuntimeEnvironment.getApplication())
        try {
            store.clear()
            store.add("情景", "The character is Cinderella.", "1girl", "",
                "シンデレラ", "ニケ", "Cinderella")
            val item = store.recent().single()
            assertEquals("シンデレラ", item.characterName)
            assertEquals("ニケ", item.characterSeries)
            assertEquals("Cinderella", item.characterEnglish)
            store.clear()
        } finally {
            store.close()
        }
    }

    @Test
    fun storesFavoritesUpdatesAndDeletes() {
        val store = HistoryStore(RuntimeEnvironment.getApplication())
        try {
            val id = store.add(
                input = "長い黒髪の少女",
                pixaiPrompt = "A girl with long black hair.",
                tagText = "1girl, long_hair",
                unmatched = "",
            )
            val initial = store.recent()
            assertEquals(1, initial.size)
            assertEquals("長い黒髪の少女", initial[0].input)
            assertEquals("1girl, long_hair", initial[0].tagText)
            assertTrue(!initial[0].isFavorite)

            store.setFavorite(id, true)
            assertEquals(1, store.recent(onlyFavorites = true).size)

            store.update(id, "Updated prompt.", "1girl, (long_hair:1.2)")
            val updated = store.recent().single()
            assertEquals("Updated prompt.", updated.pixaiPrompt)
            assertEquals("1girl, (long_hair:1.2)", updated.tagText)

            store.delete(id)
            assertTrue(store.recent().isEmpty())
        } finally {
            store.close()
        }
    }

    @Test
    fun clearsAllHistory() {
        val store = HistoryStore(RuntimeEnvironment.getApplication())
        try {
            store.add("one", "p1", "1girl", "")
            store.add("two", "p2", "1boy", "")
            assertEquals(2, store.recent().size)
            store.clear()
            assertTrue(store.recent().isEmpty())
        } finally {
            store.close()
        }
    }
}
