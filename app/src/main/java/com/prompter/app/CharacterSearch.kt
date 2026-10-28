package com.prompter.app

import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/** A search result is evidence to inspect, not an authoritative character name. */
data class CharacterSearchResult(val title: String, val url: String)

/** Synchronous network operation: callers own cancellation and must run it on IO. */
object CharacterSearch {
    private const val ENDPOINT = "https://html.duckduckgo.com/html/"
    private const val MAX_BODY_BYTES = 2_000_000

    fun search(name: String, series: String): List<CharacterSearchResult> {
        require(name.isNotBlank()) { "キャラクター名を入力してください。" }
        val query = listOf(name.trim(), series.trim(), "character english name")
            .filter { it.isNotEmpty() }.joinToString(" ")
        val url = URL("$ENDPOINT?" + "q=" + URLEncoder.encode(query, "UTF-8"))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("検索に失敗しました（HTTP $status）。")
            }
            val html = connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() < MAX_BODY_BYTES) {
                    val read = stream.read(buffer, 0, minOf(buffer.size, MAX_BODY_BYTES - output.size()))
                    if (read < 0) break
                    output.write(buffer, 0, read)
                }
                output.toString("UTF-8")
            }
            return parseResults(html).ifEmpty {
                throw IllegalStateException("検索結果を取得できませんでした。時間を置いて再試行してください。")
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseResults(html: String): List<CharacterSearchResult> =
        Jsoup.parse(html, ENDPOINT).select("a.result__a").take(2).mapNotNull { element ->
            val title = element.text().trim()
            val raw = element.absUrl("href")
            val uri = runCatching { URI(raw) }.getOrNull() ?: return@mapNotNull null
            val target = if (uri.host in setOf("duckduckgo.com", "www.duckduckgo.com") && uri.path == "/l/") {
                uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("uddg=") }
                    ?.substringAfter('=')?.let { URLDecoder.decode(it, "UTF-8") }.orEmpty()
            } else {
                raw
            }
            val parsed = runCatching { URI(target) }.getOrNull() ?: return@mapNotNull null
            if (title.isEmpty() || parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) {
                null
            } else {
                CharacterSearchResult(title, target)
            }
        }
}
