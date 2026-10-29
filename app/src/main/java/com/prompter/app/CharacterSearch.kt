package com.prompter.app

import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** A search result is evidence to inspect, not an authoritative character name. */
data class CharacterSearchResult(val title: String, val url: String)

enum class SearchFailure { CHALLENGE, NO_RESULTS, HTTP, MARKUP, TOO_LARGE }
class CharacterSearchException(val reason: SearchFailure, message: String) : IllegalStateException(message)

/** Synchronous network operation: callers own cancellation and must run it on IO. */
object CharacterSearch {
    private const val ENDPOINT = "https://lite.duckduckgo.com/lite/"
    private const val MAX_BODY_BYTES = 2_000_000

    fun search(name: String, series: String): List<CharacterSearchResult> {
        require(name.isNotBlank()) { "キャラクター名を入力してください。" }
        val url = URI(searchUrl(name, series, ENDPOINT)).toURL()
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val status = connection.responseCode
            if (status == 202 || status == 403 || status == 429) {
                throw CharacterSearchException(SearchFailure.CHALLENGE, "DuckDuckGoで認証が必要です。ブラウザで開いてください。")
            }
            if (status !in 200..299) {
                throw CharacterSearchException(SearchFailure.HTTP, "検索に失敗しました（HTTP $status）。")
            }
            val html = connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (output.size() + read > MAX_BODY_BYTES) {
                        throw CharacterSearchException(SearchFailure.TOO_LARGE, "検索結果が大きすぎます。ブラウザで開いてください。")
                    }
                    output.write(buffer, 0, read)
                }
                output.toString("UTF-8")
            }
            return interpret(html)
        } finally {
            connection.disconnect()
        }
    }

    fun searchUrl(name: String, series: String, endpoint: String = "https://duckduckgo.com/"): String {
        val query = listOf(name.trim(), series.trim(), "character english name")
            .filter { it.isNotEmpty() }.joinToString(" ")
        return "$endpoint?q=" + URLEncoder.encode(query, "UTF-8")
    }

    internal fun interpret(html: String): List<CharacterSearchResult> {
        val doc = Jsoup.parse(html)
        if (doc.select("#challenge-form, #anomaly-modal, .anomaly-modal, form[action*=anomaly]").isNotEmpty()) {
            throw CharacterSearchException(SearchFailure.CHALLENGE, "DuckDuckGoで認証が必要です。ブラウザで開いてください。")
        }
        val results = parseResults(html)
        if (results.isNotEmpty()) return results
        if (doc.select(".no-results, .no-results__message").isNotEmpty() ||
            Regex("No (?:more )?results (?:were )?found", RegexOption.IGNORE_CASE).containsMatchIn(doc.text())) {
            throw CharacterSearchException(SearchFailure.NO_RESULTS, "関連資料が見つかりませんでした。名前や作品名を変えてください。")
        }
        throw CharacterSearchException(SearchFailure.MARKUP, "検索ページを読み取れませんでした。ブラウザで開いてください。")
    }

    internal fun parseResults(html: String): List<CharacterSearchResult> =
        Jsoup.parse(html, ENDPOINT).select("a.result__a, a.result-link")
            .filterNot { element ->
                element.closest(".result--ad, .result-sponsored") != null ||
                    element.attr("rel").split(' ').contains("sponsored") ||
                    element.attr("href").contains("/y.js?")
            }.take(2).mapNotNull { element ->
            val title = element.text().trim()
            val raw = element.absUrl("href")
            val uri = runCatching { URI(raw) }.getOrNull() ?: return@mapNotNull null
            val target = if (uri.host in setOf("duckduckgo.com", "www.duckduckgo.com", "lite.duckduckgo.com", "html.duckduckgo.com") && uri.path == "/l/") {
                uri.rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("uddg=") }
                    ?.substringAfter('=')?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }.orEmpty()
            } else {
                raw
            }
            val parsed = runCatching { URI(target) }.getOrNull() ?: return@mapNotNull null
            if (title.isEmpty() || parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank() || parsed.userInfo != null) {
                null
            } else {
                CharacterSearchResult(title, target)
            }
        }
}
