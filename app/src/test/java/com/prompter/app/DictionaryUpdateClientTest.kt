package com.prompter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DictionaryUpdateClientTest {
    @Test
    fun prefersTheEnvelopeAsset() {
        val body = """
            {
              "tag_name": "dictionary-v4",
              "assets": [
                {"name": "notes.txt", "browser_download_url": "https://example.com/n.txt", "size": 10},
                {"name": "dictionary.json", "browser_download_url": "https://example.com/d.json", "size": 20},
                {"name": "dictionary-envelope.json", "browser_download_url": "https://example.com/e.json", "size": 30}
              ]
            }
        """.trimIndent()
        val release = DictionaryUpdateClient.parseRelease(body)
        assertEquals("dictionary-envelope.json", release.assetName)
        assertEquals(4L, release.version)
        assertEquals(30L, release.size)
    }

    @Test
    fun readsVersionFromTrailingTagNumber() {
        assertEquals(12L, DictionaryUpdateClient.versionOf("dictionary-v12"))
        assertEquals(7L, DictionaryUpdateClient.versionOf("dict-2026-07"))
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateClient.versionOf("dictionary-latest")
        }
    }

    @Test
    fun acceptsVersionedEnvelopeAssetNames() {
        val body = """{"tag_name":"dictionary-v2","assets":[{"name":"dictionary-envelope-v2.json","browser_download_url":"https://example.com/dictionary-envelope-v2.json","size":123}]}"""
        assertEquals("dictionary-envelope-v2.json", DictionaryUpdateClient.parseRelease(body).assetName)
    }

    @Test
    fun rejectsReleaseWithoutDictionaryAsset() {
        val body = """{"tag_name":"dictionary-v1","assets":[{"name":"readme.md","browser_download_url":"https://example.com/r.md","size":1}]}"""
        assertThrows(IllegalArgumentException::class.java) {
            DictionaryUpdateClient.parseRelease(body)
        }
    }
}
