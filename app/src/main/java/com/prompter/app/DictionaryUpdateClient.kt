package com.prompter.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the newest signed dictionary from GitHub Releases.
 *
 * The release tag carries the dictionary version (for example
 * `dictionary-v12`); the asset is a detached-signature envelope checked by
 * [DictionaryUpdateVerifier] before anything is stored. Network failures never
 * change local data.
 */
object DictionaryUpdateClient {
    private const val RELEASES_URL =
        "https://api.github.com/repos/shiokunn/prompter-dictionary/releases?per_page=20"
    private const val MAX_BYTES = 8 * 1024 * 1024

    // Preference order when a release publishes more than one candidate asset.
    private val ASSET_NAMES = listOf(
        "dictionary-envelope.json",
        "dictionary.json",
        "prompter-dictionary.json",
    )

    data class Release(
        val tag: String,
        val version: Long,
        val assetName: String,
        val url: String,
        val size: Long,
    )

    fun latestRelease(): Release {
        val body = getText(RELEASES_URL)
        val releases = JSONArray(body)
        for (index in 0 until releases.length()) {
            val release = releases.getJSONObject(index)
            if (release.optBoolean("draft")) continue
            runCatching { parseRelease(release) }.getOrNull()?.let { return it }
        }
        throw IllegalArgumentException("配布元に辞書リリースがありません。")
    }

    internal fun parseRelease(body: String): Release = parseRelease(JSONObject(body))

    private fun parseRelease(root: JSONObject): Release {
        require(!root.has("message")) { root.optString("message", "辞書リリースを取得できません。") }
        val tag = root.getString("tag_name")
        val assets = root.getJSONArray("assets")
        var chosen: JSONObject? = null
        for (wanted in ASSET_NAMES) {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name") == wanted) {
                    chosen = asset
                    break
                }
            }
            if (chosen != null) break
        }
        if (chosen == null) {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (name.startsWith("dictionary-envelope-") && name.endsWith(".json")) {
                    chosen = asset
                    break
                }
            }
        }
        val asset = chosen ?: throw IllegalArgumentException("辞書ファイルがリリースに含まれていません。")
        return Release(
            tag = tag,
            version = versionOf(tag),
            assetName = asset.getString("name"),
            url = asset.getString("browser_download_url"),
            size = asset.optLong("size", 0L),
        )
    }

    internal fun versionOf(tag: String): Long =
        Regex("(\\d+)\\s*$").find(tag)?.groupValues?.get(1)?.toLongOrNull()
            ?: throw IllegalArgumentException("辞書リリースのバージョンを読み取れません。")

    fun download(release: Release): ByteArray {
        require(release.size <= MAX_BYTES) { "辞書ファイルが大きすぎます。" }
        val connection = open(release.url)
        try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "辞書ファイルを取得できません（HTTP ${connection.responseCode}）。"
            }
            val declared = connection.contentLengthLong
            require(declared <= MAX_BYTES) { "辞書ファイルが大きすぎます。" }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    require(output.size() <= MAX_BYTES) { "辞書ファイルが大きすぎます。" }
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun getText(url: String): String {
        val connection = open(url)
        try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    "配布元に辞書リリースがありません。"
                } else {
                    "更新を確認できませんでした（HTTP ${connection.responseCode}）。"
                }
            }
            return connection.inputStream.bufferedReader().use { reader -> reader.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Prompter-Dictionary-Updater")
        }
}
