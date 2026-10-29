package com.prompter.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CharacterCandidate(
    val name: String,
    val nativeName: String,
    val series: String,
    val source: String,
    val url: String,
)

/** Returns candidate names, not verified official translations. Run on Dispatchers.IO. */
object CharacterCatalogSearch {
    fun search(name: String): List<CharacterCandidate> {
        require(name.isNotBlank())
        val query = "query (\$search: String) { Page(page: 1, perPage: 5) { characters(search: \$search) { name { full native } siteUrl media(perPage: 3) { nodes { title { romaji english native } } } } } }"
        val request = JSONObject().put("query", query).put("variables", JSONObject().put("search", name.trim()))
        val data = request("https://graphql.anilist.co", request.toString())
        if (data.has("errors")) throw IllegalStateException("AniListの検索に失敗しました。")
        val characters = data.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("characters")
            ?: throw IllegalStateException("AniListから不正な応答を受信しました。")
        val results = buildList {
            for (i in 0 until characters.length()) {
                val item = characters.optJSONObject(i) ?: continue
                val names = item.optJSONObject("name") ?: continue
                val full = names.optString("full").takeUnless { it == "null" }.orEmpty()
                val link = item.optString("siteUrl")
                if (full.isBlank() || !link.startsWith("https://anilist.co/character/")) continue
                val nodes = item.optJSONObject("media")?.optJSONArray("nodes")
                val titles = buildList {
                    if (nodes != null) for (j in 0 until nodes.length()) {
                        val title = nodes.optJSONObject(j)?.optJSONObject("title") ?: continue
                        val text = listOf("english", "romaji", "native").map { title.optString(it) }
                            .firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
                        if (text.isNotBlank()) add(text)
                    }
                }
                add(CharacterCandidate(full, names.optString("native").takeUnless { it == "null" }.orEmpty(),
                    titles.distinct().joinToString(" / "), "AniList", link))
            }
        }
        if (results.isNotEmpty()) return results
        val encoded = URLEncoder.encode(name.trim(), "UTF-8")
        val fallback = request("https://www.wikidata.org/w/api.php?action=wbsearchentities&search=$encoded&language=ja&uselang=en&type=item&format=json&limit=5")
        val items = fallback.optJSONArray("search") ?: throw IllegalStateException("Wikidataから不正な応答を受信しました。")
        val ids = (0 until items.length()).mapNotNull { i ->
            items.optJSONObject(i)?.optString("id")?.takeIf { it.matches(Regex("Q[1-9][0-9]*")) }
        }
        if (ids.isEmpty()) return emptyList()
        val entities = request("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${ids.joinToString("%7C")}&props=labels%7Cdescriptions&languages=en%7Cja&format=json").optJSONObject("entities")
            ?: throw IllegalStateException("Wikidataから不正な応答を受信しました。")
        return ids.mapNotNull { id ->
            val entity = entities.optJSONObject(id) ?: return@mapNotNull null
            val labels = entity.optJSONObject("labels") ?: return@mapNotNull null
            val english = labels.optJSONObject("en")?.optString("value").orEmpty()
            if (english.isBlank()) return@mapNotNull null
            val description = entity.optJSONObject("descriptions")?.optJSONObject("en")?.optString("value").orEmpty()
            CharacterCandidate(english, labels.optJSONObject("ja")?.optString("value").orEmpty(), description,
                "Wikidata", "https://www.wikidata.org/wiki/$id")
        }
    }

    private fun request(url: String, body: String? = null): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Prompter/0.8 (character lookup)")
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            if (connection.responseCode !in 200..299) throw IllegalStateException("検索に失敗しました（HTTP ${connection.responseCode}）。")
            val bytes = connection.inputStream.use { it.readBytesLimited(1_000_000) }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(out.size() + count <= limit) { "検索結果が大きすぎます。" }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }
}
