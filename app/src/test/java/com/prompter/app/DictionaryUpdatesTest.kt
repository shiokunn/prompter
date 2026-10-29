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
class DictionaryUpdatesTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun installReplacesTheWholeSetAndKeepsOneVersion() {
        DictionaryUpdates.clear(context)
        DictionaryUpdates.install(
            context,
            VerifiedDictionaryUpdate(
                version = 1,
                aliases = listOf(DictionaryAliasUpdate("立ち絵", "standing")),
            ),
            sourceTag = "dictionary-v1",
            now = 100,
        )
        assertEquals(1, DictionaryUpdates.snapshot(context).aliasCount)

        val second = DictionaryUpdates.install(
            context,
            VerifiedDictionaryUpdate(
                version = 2,
                aliases = listOf(
                    DictionaryAliasUpdate("立ち絵", "standing"),
                    DictionaryAliasUpdate("ねこみみ", "cat_ears"),
                ),
            ),
            sourceTag = "dictionary-v2",
            now = 200,
        )
        assertEquals(2L, second.version)
        assertEquals(2, second.aliasCount)

        val snapshot = DictionaryUpdates.snapshot(context)
        assertEquals(2L, snapshot.version)
        assertEquals("dictionary-v2", snapshot.sourceTag)
        assertEquals(2, snapshot.aliasCount)
        assertTrue(snapshot.aliases.any { it.norm == "ねこみみ" && it.tag == "cat_ears" })
        assertTrue(snapshot.aliases.all { it.priority > 1000 })

        DictionaryUpdates.clear(context)
        assertEquals(0L, DictionaryUpdates.snapshot(context).version)
    }

    @Test
    fun signedUpdateWinsOverBundledAliasAndCanIntroduceTags() {
        val db = PromptDb.open(context)
        val updates = listOf(
            UpdateAlias(
                surface = "立ち絵",
                norm = M1Matcher.normalize("立ち絵"),
                tag = "sitting",
                source = "signed-update",
                confidence = 1.0,
                priority = 10_000,
            ),
            UpdateAlias(
                surface = "ぷろんぷたー",
                norm = M1Matcher.normalize("ぷろんぷたー"),
                tag = "prompter_test_alias",
                source = "signed-update",
                confidence = 1.0,
                priority = 10_000,
            ),
        )
        val matcher = M1Matcher.load(db, updates)

        val overridden = matcher.convert("立ち絵")
        assertTrue(overridden.tags.contains("sitting"))
        assertTrue(!overridden.tags.contains("standing"))

        val introduced = matcher.convert("ぷろんぷたー")
        assertTrue(introduced.tags.contains("prompter_test_alias"))
        db.close()
    }
}
