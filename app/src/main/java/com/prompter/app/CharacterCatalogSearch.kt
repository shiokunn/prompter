package com.prompter.app

import kotlinx.coroutines.CancellationException
import org.json.JSONArray
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
    val aliases: List<String> = emptyList(),
    val workTitles: List<String> = emptyList(),
)

/** Candidate lookup, never automatic identity confirmation. Run on IO. */
object CharacterCatalogSearch {
    fun search(name: String, series: String = ""): List<CharacterCandidate> =
        search(name, series, ::request)

    internal fun search(
        name: String,
        series: String,
        transport: (String, String?) -> JSONObject,
    ): List<CharacterCandidate> {
        require(name.isNotBlank())
        val failures = mutableListOf<Exception>()
        try {
            val query = "query (\$search: String) { Page(page: 1, perPage: 20) { characters(search: \$search) { name { full native alternative } siteUrl media(perPage: 8) { nodes { title { romaji english native } } } } } }"
            val request = JSONObject().put("query", query)
                .put("variables", JSONObject().put("search", name.trim()))
            val response = transport("https://graphql.anilist.co", request.toString())
            val ranked = rank(parseAniList(response), name, series)
            if (ranked.isNotEmpty()) return ranked
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            failures.add(failure)
        }

        try {
            val lang = if (name.any { it.code > 127 }) "ja" else "en"
            val search = transport("https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encode(name.trim())}&language=$lang&uselang=en&type=item&format=json&limit=10", null)
            val items = search.optJSONArray("search") ?: error("Wikidataから不正な応答を受信しました。")
            val ids = (0 until items.length()).mapNotNull { i ->
                items.optJSONObject(i)?.optString("id")?.takeIf { it.matches(Regex("Q[1-9][0-9]*")) }
            }
            if (ids.isEmpty()) {
                if (failures.isNotEmpty()) throw IllegalStateException("AniListに接続できず、Wikidataに候補がありません。再試行してください。")
                return emptyList()
            }
            val response = transport("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${ids.joinToString("%7C")}&props=labels%7Cdescriptions%7Caliases%7Cclaims&languages=en%7Cja&format=json", null)
            val entities = response.optJSONObject("entities") ?: error("Wikidataから不正な応答を受信しました。")
            val candidates = ids.mapNotNull { id -> parseWikidata(id, entities.optJSONObject(id)) }
            return rank(candidates, name, series)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw IllegalStateException("外部候補を検索できませんでした。通信を確認するか、関連資料を開いてください。", failure)
        }
    }

    internal fun parseAniList(response: JSONObject): List<CharacterCandidate> {
        check(!response.has("errors")) { "AniListの検索に失敗しました。" }
        val items = response.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("characters")
            ?: error("AniListから不正な応答を受信しました。")
        return (0 until items.length()).mapNotNull { i ->
            val item = items.optJSONObject(i) ?: return@mapNotNull null
            val names = item.optJSONObject("name") ?: return@mapNotNull null
            val name = names.text("full")
            val link = item.text("siteUrl")
            if (name.isBlank() || !link.matches(Regex("https://anilist\\.co/character/[0-9]+(?:/[^\\s]*)?"))) return@mapNotNull null
            val nodes = item.optJSONObject("media")?.optJSONArray("nodes") ?: JSONArray()
            val titles = (0 until nodes.length()).flatMap { j ->
                val title = nodes.optJSONObject(j)?.optJSONObject("title")
                listOf("native", "english", "romaji").mapNotNull { title?.text(it)?.takeIf(String::isNotBlank) }
            }.distinct()
            CharacterCandidate(name, names.text("native"), titles.joinToString(" / "), "AniList", link,
                strings(names.optJSONArray("alternative")), titles)
        }
    }

    internal fun parseWikidata(id: String, entity: JSONObject?): CharacterCandidate? {
        if (entity == null) return null
        val descriptions = entity.optJSONObject("descriptions")
        val description = listOf("en", "ja").mapNotNull { descriptions?.optJSONObject(it)?.text("value") }.joinToString(" / ")
        val claims = entity.optJSONObject("claims")?.optJSONArray("P31") ?: JSONArray()
        val fictional = (0 until claims.length()).any {
            claims.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")
                ?.optJSONObject("value")?.text("id") == "Q95074"
        } || Regex("fictional (?:\\w+ )?character|登場人物|架空の人物|キャラクター", RegexOption.IGNORE_CASE).containsMatchIn(description)
        if (!fictional) return null // Never offer real people, places or work titles as character identities.
        val labels = entity.optJSONObject("labels") ?: return null
        val en = labels.optJSONObject("en")?.text("value").orEmpty()
        if (en.isBlank()) return null
        val aliases = entity.optJSONObject("aliases")
        val alternatives = listOf("ja", "en").flatMap { lang ->
            val values = aliases?.optJSONArray(lang) ?: JSONArray()
            (0 until values.length()).mapNotNull { values.optJSONObject(it)?.text("value") }
        }
        return CharacterCandidate(en, labels.optJSONObject("ja")?.text("value").orEmpty(), description,
            "Wikidata", "https://www.wikidata.org/wiki/$id", alternatives)
    }

    internal fun rank(items: List<CharacterCandidate>, name: String, series: String): List<CharacterCandidate> {
        val q = CharacterNames.normalize(name)
        fun score(item: CharacterCandidate): Int {
            val names = (listOf(item.name, item.nativeName) + item.aliases).map(CharacterNames::normalize)
            return when {
                q in names -> 0
                names.any { it.startsWith(q) } -> 1
                names.any { it.contains(q) } -> 2
                else -> 3
            }
        }
        return items.filter { score(it) < 3 && CharacterNames.workMatches(series, it.workTitles.ifEmpty { listOf(it.series) }) }
            .distinctBy { it.url }
            .sortedWith(compareBy<CharacterCandidate> { score(it) }.thenBy { it.name })
            .take(10)
    }

    private fun JSONObject.text(key: String) = optString(key).takeUnless { it == "null" }.orEmpty()
    private fun strings(values: JSONArray?): List<String> = if (values == null) emptyList() else
        (0 until values.length()).mapNotNull { values.optString(it).takeIf { s -> s.isNotBlank() && s != "null" } }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun request(url: String, body: String?): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Prompter/0.10 (character lookup)")
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            check(connection.responseCode in 200..299) { "検索に失敗しました（HTTP ${connection.responseCode}）。" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1_000_000) { "検索結果が大きすぎます。" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }
}
