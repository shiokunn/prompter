package com.prompter.app

import android.database.sqlite.SQLiteDatabase
import java.text.Normalizer
import java.util.Locale

data class TagMatch(
    val tag: String,
    val surface: String,
    val source: String,
    val start: Int,
    val end: Int,
    val confidence: Double,
)

/**
 * Kotlin port of tools/m1_match.py. It intentionally mirrors the reference
 * implementation's longest-match, boundary, parent-expansion, and inference
 * rules so the app and the evaluation harness agree.
 */
class M1Matcher private constructor(
    private val allTags: Set<String>,
    private val tagCounts: Map<String, Long>,
    private val root: Node,
    private val compositeRules: List<CompositeRule>,
    private val sizeRules: List<CompositeRule>,
    val tagCount: Int,
    val aliasCount: Int,
) {
    val allTagNames: Set<String>
        get() = allTags

    data class Conversion(
        val tags: List<String>,
        val matches: List<TagMatch>,
        val unmatched: String,
    )

    private class Node {
        val children = HashMap<Char, Node>()
        var entries: MutableList<AliasEntry>? = null
    }

    private data class AliasEntry(
        val tag: String,
        val surface: String,
        val source: String,
        val confidence: Double,
        val priority: Int,
        val postCount: Long,
    )

    private data class CompositeRule(val regex: Regex, val tag: String)

    fun convert(input: String): Conversion {
        val matches = match(input)
        return Conversion(
            tags = matches.map { it.tag },
            matches = matches,
            unmatched = unmatched(input, matches),
        )
    }

    fun isKnownTag(tag: String): Boolean = tag in allTags

    fun match(input: String): List<TagMatch> {
        val text = normalize(input)
        val matches = mutableListOf<TagMatch>()
        matches.addAll(dictionaryMatches(text))

        val occupied = matches.map { it.start to it.end }
        val tags = matches.map { it.tag }.toMutableSet()
        matches.addAll(ruleMatches(text, occupied, tags))
        expandParents(matches, tags)
        infer(text, matches, tags)

        // One output per tag, keeping the most confident / earliest match.
        val best = LinkedHashMap<String, TagMatch>()
        for (m in matches) {
            val old = best[m.tag]
            if (old == null || m.confidence > old.confidence ||
                (m.confidence == old.confidence && m.start < old.start)
            ) {
                best[m.tag] = m
            }
        }
        return best.values.sortedWith(
            compareBy<TagMatch> { it.start }
                .thenByDescending { it.confidence }
                .thenBy { it.tag },
        )
    }

    private fun dictionaryMatches(text: String): List<TagMatch> {
        val out = mutableListOf<TagMatch>()
        var i = 0
        while (i < text.length) {
            var node = root
            var j = i
            var bestEnd = -1
            var bestEntries: MutableList<AliasEntry>? = null

            while (j < text.length) {
                val next = node.children[text[j]] ?: break
                node = next
                j += 1
                val entries = node.entries
                if (entries != null && j - i > bestEnd - i) {
                    bestEnd = j
                    bestEntries = entries
                }
            }

            val entries = bestEntries
            if (entries != null && boundaryOk(text, i, bestEnd, entries.first().surface)) {
                val best = entries.maxWithOrNull(
                    compareBy<AliasEntry> { it.priority }
                        .thenBy { it.confidence }
                        .thenBy { it.postCount }
                        .thenBy { it.surface.length },
                )
                if (best != null) {
                    out.add(TagMatch(best.tag, best.surface, best.source, i, bestEnd, best.confidence))
                    i = bestEnd
                    continue
                }
            }
            i += 1
        }
        return out
    }

    private fun ruleMatches(
        text: String,
        occupied: List<Pair<Int, Int>>,
        tags: MutableSet<String>,
    ): List<TagMatch> {
        val out = mutableListOf<TagMatch>()
        for (rule in compositeRules + sizeRules) {
            if (rule.tag in tags) continue
            for (m in rule.regex.findAll(text)) {
                val start = m.range.first
                val end = m.range.last + 1
                if (occupied.any { (a, b) -> a <= start && end <= b }) continue
                out.add(TagMatch(rule.tag, m.value, "composed", start, end, 0.8))
                tags.add(rule.tag)
                break
            }
        }
        return out
    }

    private fun expandParents(matches: MutableList<TagMatch>, tags: MutableSet<String>) {
        val queue = ArrayDeque<String>()
        queue.addAll(tags)
        while (queue.isNotEmpty()) {
            val tag = queue.removeLast()
            val parents = mutableListOf<String>()
            PARENT_OF[tag]?.let { parents.addAll(it) }
            for ((suffix, parent) in SUFFIX_PARENT) {
                if (tag.endsWith(suffix)) parents.add(parent)
            }
            for (parent in parents) {
                if (parent in allTags && parent !in tags) {
                    matches.add(TagMatch(parent, "", "parent", 0, 0, 0.9))
                    tags.add(parent)
                    queue.add(parent)
                }
            }
        }
    }

    private fun addInferred(
        matches: MutableList<TagMatch>,
        tags: MutableSet<String>,
        tag: String,
        confidence: Double,
        surface: String = "",
    ) {
        if (tag in tags || tag !in allTags) return
        matches.add(TagMatch(tag, surface, "inferred", 0, 0, confidence))
        tags.add(tag)
    }

    private fun infer(text: String, matches: MutableList<TagMatch>, tags: MutableSet<String>) {
        // Count inference must run before gender inference: 二人の少女 is
        // 2girls, not 1girl + solo.
        var multiple = tags.any { it in MULTIPLE_TAGS }
        val malePresent = "1boy" in tags || tags.any { it in MALE_CUES }
        val femaleContext = tags.any { it in FEMALE_CUES } || "1girl" in tags

        if (!multiple) {
            if (Regex("二人|2人|ふたり|もう一人|両者").containsMatchIn(text)) {
                if (femaleContext && !malePresent) {
                    addInferred(matches, tags, "2girls", 0.7, "two people")
                    addInferred(matches, tags, "multiple_girls", 0.65, "two people")
                } else if (malePresent && !femaleContext) {
                    addInferred(matches, tags, "2boys", 0.6, "two people")
                }
            } else if (Regex("三人|3人|みたり").containsMatchIn(text)) {
                if (femaleContext && !malePresent) {
                    addInferred(matches, tags, "3girls", 0.65, "three people")
                    addInferred(matches, tags, "multiple_girls", 0.6, "three people")
                }
            } else if ("それぞれ" in text && "と" in text && femaleContext && !malePresent) {
                addInferred(matches, tags, "2girls", 0.6, "それぞれ")
                addInferred(matches, tags, "multiple_girls", 0.55, "それぞれ")
            }
        }

        multiple = tags.any { it in MULTIPLE_TAGS }
        if ("1girl" !in tags && "1boy" !in tags && !multiple) {
            if (femaleContext && tags.none { it in MALE_CUES }) {
                addInferred(matches, tags, "1girl", 0.65, "female cues")
            } else if (tags.any { it in MALE_CUES } && !femaleContext) {
                addInferred(matches, tags, "1boy", 0.6, "male cues")
            }
        }

        val strongMale = tags.any { it in STRONG_MALE_CUES }
        val strongFemale = tags.any { it in STRONG_FEMALE_CUES }
        if (strongMale && "1boy" !in tags) {
            addInferred(matches, tags, "1boy", 0.6, "male anatomy")
        }
        if (strongFemale && "1girl" !in tags) {
            addInferred(matches, tags, "1girl", 0.6, "female anatomy")
        }

        if ("sex" in tags && tags.none { it == "yuri" || it == "lesbian" }) {
            if ("1girl" !in tags) addInferred(matches, tags, "1girl", 0.55, "sex scene")
            if ("1boy" !in tags) addInferred(matches, tags, "1boy", 0.55, "sex scene")
        }

        if ("1girl" in tags && ("nude" in tags || "topless" in tags)) {
            addInferred(matches, tags, "breasts", 0.6, "female nudity")
            addInferred(matches, tags, "nipples", 0.55, "female nudity")
        }

        multiple = tags.any { it in MULTIPLE_TAGS }
        if (tags.any { it in GIRL_COUNT_TAGS }) {
            addInferred(matches, tags, "multiple_girls", 0.6, "multiple girls")
        }
        if ("1girl" in tags && "1boy" in tags) {
            addInferred(matches, tags, "hetero", 0.5, "1girl+1boy")
        }
        if (Regex("百合|ゆり|レズ").containsMatchIn(text) && "1boy" !in tags) {
            addInferred(matches, tags, "yuri", 0.7, "yuri")
        } else if (multiple && "1boy" !in tags && Regex("抱きつ|キス|口づけ").containsMatchIn(text)) {
            addInferred(matches, tags, "yuri", 0.55, "intimate girls")
        }

        val people = tags.filter { it == "1girl" || it == "1boy" }
        multiple = tags.any { it in MULTIPLE_TAGS }
        if (people.size == 1 && !multiple) {
            if ("1girl" in people) {
                addInferred(matches, tags, "solo", 0.55, "single person")
            } else if (Regex("一人|独り|単独|1人|ひとり").containsMatchIn(text)) {
                addInferred(matches, tags, "solo", 0.55, "single person")
            }
        }
    }

    private fun unmatched(text: String, matches: List<TagMatch>): String {
        val normalized = normalize(text)
        val spans = matches
            .filter { it.end > it.start }
            .sortedBy { it.start }
            .map { it.start to it.end }
        val out = StringBuilder()
        var pos = 0
        for ((start, end) in spans) {
            if (start > pos) out.append(normalized, pos, start).append(" / ")
            pos = maxOf(pos, end)
        }
        if (pos < normalized.length) out.append(normalized, pos, normalized.length)
        return out.toString().trim().trimEnd('/').trim()
    }

    companion object {
        private const val TERM = '\u0000'

        fun load(db: SQLiteDatabase): M1Matcher {
            val tags = mutableSetOf<String>()
            val counts = HashMap<String, Long>()
            db.rawQuery("SELECT name, post_count FROM tags", null).use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0)
                    tags.add(name)
                    counts[name] = c.getLong(1)
                }
            }

            val root = Node()
            var aliasCount = 0
            db.rawQuery(
                "SELECT t.name, a.surface, a.norm, a.source, a.confidence, a.priority " +
                    "FROM aliases a JOIN tags t ON t.id=a.tag_id WHERE a.lang='ja'",
                null,
            ).use { c ->
                while (c.moveToNext()) {
                    val tag = c.getString(0)
                    val surface = c.getString(1)
                    val norm = c.getString(2)
                    val source = c.getString(3)
                    val confidence = c.getDouble(4)
                    val priority = c.getInt(5)
                    var node = root
                    for (ch in norm) {
                        node = node.children.getOrPut(ch) { Node() }
                    }
                    val list = node.entries ?: mutableListOf<AliasEntry>().also { node.entries = it }
                    list.add(
                        AliasEntry(
                            tag = tag,
                            surface = surface,
                            source = source,
                            confidence = confidence,
                            priority = priority,
                            postCount = counts[tag] ?: 0L,
                        ),
                    )
                    aliasCount += 1
                }
            }

            val sizeRules = SIZE_RULES.map { (pattern, tag) ->
                CompositeRule(Regex(pattern), tag)
            }
            val compositeRules = buildCompositeRules(tags)
            return M1Matcher(
                allTags = tags,
                tagCounts = counts,
                root = root,
                compositeRules = compositeRules,
                sizeRules = sizeRules,
                tagCount = tags.size,
                aliasCount = aliasCount,
            )
        }

        private fun buildCompositeRules(tags: Set<String>): List<CompositeRule> {
            val colorGroups = LinkedHashMap<String, LinkedHashSet<String>>()
            for ((surface, color) in COLORS) {
                val norm = normalize(surface)
                if (norm.isEmpty()) continue
                val group = colorGroups.getOrPut(color) { LinkedHashSet() }
                group.add(norm)
                if (norm.endsWith("い")) group.add(norm.dropLast(1))
            }

            val rules = mutableListOf<CompositeRule>()
            for ((color, surfaces) in colorGroups) {
                val colorRe = surfaces
                    .sortedByDescending { it.length }
                    .joinToString("|") { Regex.escape(it) }
                for ((nounSurface, noun) in NOUNS) {
                    val tag = tagForColor(color, noun)
                    if (tag !in tags) continue
                    val nounNorm = normalize(nounSurface)
                    if (nounNorm.isEmpty()) continue
                    rules.add(
                        CompositeRule(
                            Regex("(?:$colorRe).{0,4}?${Regex.escape(nounNorm)}"),
                            tag,
                        ),
                    )
                }
            }
            return rules
        }

        private fun tagForColor(color: String, noun: String): String {
            val prefix = when (noun) {
                "hair" -> HAIR_COLOR[color] ?: color
                "eyes" -> EYE_COLOR[color] ?: color
                else -> color
            }
            return "${prefix}_$noun"
        }

        fun normalize(value: String): String {
            val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
            val out = StringBuilder(nfkc.length)
            for (ch in nfkc) {
                val o = ch.code
                if (o in 0x30A1..0x30F6) {
                    out.append((o - 0x60).toChar())
                } else {
                    out.append(ch)
                }
            }
            return PUNCT.replace(out.toString(), "")
        }

        private fun isKana(ch: Char): Boolean =
            ch in '\u3041'..'\u309F' || ch in '\u30A1'..'\u30F6' || ch == 'ー'

        private fun isKanji(ch: Char): Boolean =
            ch in '\u3400'..'\u4DBF' || ch in '\u4E00'..'\u9FFF'

        private fun boundaryOk(text: String, start: Int, end: Int, surface: String): Boolean {
            val n = normalize(surface)
            val before = text.getOrNull(start - 1)
            val after = text.getOrNull(end)
            if (n.isNotEmpty() && n.all { isKana(it) }) {
                if (n.length <= 1) {
                    return !(before?.let { isKana(it) } == true || after?.let { isKana(it) } == true)
                }
                val allowed = if (n.length == 2) KANA_SHORT_BOUNDARY_OK else KANA_BOUNDARY_OK
                if (before != null && isKana(before) && before !in allowed) return false
                if (after != null && isKana(after) && after !in allowed) return false
                return true
            }
            if (n.length == 1 && isKanji(n[0])) {
                val prefixes = BODY_PREFIX_OK[n[0]]
                if (before != null && prefixes?.contains(before) == true) return true
                return !(before?.let { isKanji(it) } == true || after?.let { isKanji(it) } == true)
            }
            return true
        }

        private val PUNCT = Regex("[\\s_\\-・,、。.!?！？「」『』()（）\\[\\]【】/\\\\:;…~〜]+")

        private val KANA_BOUNDARY_OK = (
            "はがをにでとのへもやかならばだ" +
                "たていくるすずれりきけしせめろわを" +
                "からまでよりなどだけしかこそってながらそうない"
            ).toSet()

        private val KANA_SHORT_BOUNDARY_OK = "はがをにでとのへもやかなら".toSet()

        private val BODY_PREFIX_OK: Map<Char, Set<Char>> = mapOf(
            '胸' to "右左両片上下大中小".toSet(),
            '目' to "右左両片".toSet(),
            '耳' to "右左両片".toSet(),
            '腕' to "右左両片".toSet(),
            '脚' to "右左両片".toSet(),
            '足' to "右左両片".toSet(),
            '手' to "右左両片".toSet(),
            '指' to "右左両片".toSet(),
            '尻' to "右左".toSet(),
            '腹' to "下".toSet(),
            '首' to "右左".toSet(),
            '歯' to "上下".toSet(),
            '唇' to "上下".toSet(),
            '顔' to "右左".toSet(),
            '髪' to "右左".toSet(),
        )

        private val COLORS: List<Pair<String, String>> = listOf(
            "赤" to "red", "赤い" to "red", "赤色" to "red",
            "青" to "blue", "青い" to "blue", "青色" to "blue", "蒼" to "blue",
            "白" to "white", "白い" to "white", "白色" to "white",
            "黒" to "black", "黒い" to "black", "黒色" to "black",
            "茶" to "brown", "茶色" to "brown", "茶色い" to "brown",
            "緑" to "green", "緑色" to "green",
            "紫" to "purple", "紫色" to "purple",
            "ピンク" to "pink", "桃色" to "pink",
            "オレンジ" to "orange", "橙色" to "orange",
            "黄色" to "yellow", "黄" to "yellow",
            "灰色" to "grey", "グレー" to "grey",
            "銀" to "silver", "銀色" to "silver",
            "金" to "gold", "金色" to "gold",
            "水色" to "aqua",
        )

        private val NOUNS: List<Pair<String, String>> = listOf(
            "髪" to "hair",
            "目" to "eyes",
            "瞳" to "eyes",
            "シャツ" to "shirt",
            "ドレス" to "dress",
            "スカート" to "skirt",
            "リボン" to "ribbon",
            "セーラーカラー" to "sailor_collar",
            "背景" to "background",
            "靴下" to "socks",
            "ソックス" to "socks",
            "ニーソ" to "thighhighs",
            "ニーハイ" to "thighhighs",
            "ハイソックス" to "thighhighs",
            "ブラウス" to "blouse",
            "キャミソール" to "camisole",
            "レオタード" to "leotard",
            "ボディスーツ" to "bodysuit",
            "ジャケット" to "jacket",
            "手袋" to "gloves",
            "帽子" to "hat",
            "翼" to "wings",
            "パンティ" to "panties",
            "ブラ" to "bra",
            "ネクタイ" to "necktie",
            "ベスト" to "vest",
            "コート" to "coat",
            "水着" to "swimsuit",
            "浴衣" to "yukata",
            "着物" to "kimono",
            "エプロン" to "apron",
            "ストッキング" to "pantyhose",
            "靴" to "footwear",
            "ブーツ" to "boots",
            "ハイヒール" to "high_heels",
            "ローファー" to "loafers",
            "スニーカー" to "sneakers",
            "サンダル" to "sandals",
            "ズボン" to "pants",
            "ビキニ" to "bikini",
            "マフラー" to "scarf",
            "ネックレス" to "necklace",
            "イヤリング" to "earrings",
            "蝶ネクタイ" to "bowtie",
        )

        private val HAIR_COLOR = mapOf(
            "red" to "red", "blue" to "blue", "white" to "white",
            "black" to "black", "brown" to "brown", "green" to "green",
            "purple" to "purple", "pink" to "pink", "orange" to "orange",
            "yellow" to "blonde", "grey" to "grey", "silver" to "silver",
            "gold" to "blonde", "aqua" to "aqua",
        )

        private val EYE_COLOR = mapOf(
            "red" to "red", "blue" to "blue", "white" to "white",
            "black" to "black", "brown" to "brown", "green" to "green",
            "purple" to "purple", "pink" to "pink", "orange" to "orange",
            "yellow" to "yellow", "grey" to "grey", "silver" to "grey",
            "gold" to "yellow", "aqua" to "aqua",
        )

        private val SIZE_RULES: List<Pair<String, String>> = listOf(
            "(?:とても|非常に|すごく)長[いく].{0,10}?髪" to "very_long_hair",
            "長[いく].{0,10}?髪" to "long_hair",
            "短[いく].{0,10}?髪" to "short_hair",
            "髪.{0,10}?短く" to "short_hair",
            "髪.{0,10}?長く" to "long_hair",
            "中くらいの.{0,4}?髪" to "medium_hair",
            "(?:大きな|大きい|豊満な).{0,4}?胸" to "large_breasts",
            "(?:中くらいの|普通の).{0,4}?胸" to "medium_breasts",
            "(?:小さな|小さい).{0,4}?胸" to "small_breasts",
            "長い.{0,4}?袖" to "long_sleeves",
            "(?:短い|半).{0,2}?袖" to "short_sleeves",
            "(?:しんぷる|単色|無地|単純).{0,6}?背景" to "simple_background",
            "背景.{0,4}?(?:しんぷる|単色|無地|単純)" to "simple_background",
        )

        private val SUFFIX_PARENT: Map<String, String> = mapOf(
            "_dress" to "dress",
            "_shirt" to "shirt",
            "_skirt" to "skirt",
            "_ribbon" to "ribbon",
            "_panties" to "panties",
            "_bra" to "bra",
            "_thighhighs" to "thighhighs",
            "_pantyhose" to "pantyhose",
            "_socks" to "socks",
            "_gloves" to "gloves",
            "_hat" to "hat",
            "_wings" to "wings",
            "_bow" to "bow",
            "_necktie" to "necktie",
            "_vest" to "vest",
            "_coat" to "coat",
            "_swimsuit" to "swimsuit",
            "_bikini" to "bikini",
            "_yukata" to "yukata",
            "_kimono" to "kimono",
            "_apron" to "apron",
            "_shoes" to "shoes",
            "_boots" to "boots",
            "_scarf" to "scarf",
            "_necklace" to "necklace",
            "_earrings" to "earrings",
            "_sailor_collar" to "sailor_collar",
            "_leotard" to "leotard",
            "_bodysuit" to "bodysuit",
            "_camisole" to "camisole",
            "_blouse" to "blouse",
            "_tail" to "tail",
            "_sword" to "sword",
            "_weapon" to "weapon",
        )

        private val PARENT_OF: Map<String, List<String>> = mapOf(
            "large_breasts" to listOf("breasts"),
            "medium_breasts" to listOf("breasts"),
            "small_breasts" to listOf("breasts"),
            "very_long_hair" to listOf("long_hair"),
            "cat_ears" to listOf("animal_ears"),
            "dog_ears" to listOf("animal_ears"),
            "fox_ears" to listOf("animal_ears"),
            "rabbit_ears" to listOf("animal_ears"),
            "serafuku" to listOf("school_uniform", "sailor_collar"),
            "panties" to listOf("underwear"),
            "bra" to listOf("underwear"),
            "on_back" to listOf("lying"),
            "on_stomach" to listOf("lying"),
            "seiza" to listOf("sitting"),
            "kneeling" to listOf("sitting"),
            "light_smile" to listOf("smile"),
            "laughing" to listOf("smile"),
            "crying" to listOf("tears"),
            "2girls" to listOf("multiple_girls"),
            "3girls" to listOf("multiple_girls"),
            "4girls" to listOf("multiple_girls"),
            "5girls" to listOf("multiple_girls"),
            "6+girls" to listOf("multiple_girls"),
            "cover_page" to listOf("cover"),
            "doujin_cover" to listOf("cover"),
            "holding_sword" to listOf("holding", "sword", "weapon"),
            "holding_weapon" to listOf("holding", "weapon"),
            "sword" to listOf("weapon"),
            "gun" to listOf("weapon"),
            "spear" to listOf("weapon"),
            "knife" to listOf("weapon"),
            "bow_(weapon)" to listOf("weapon"),
            "armored_dress" to listOf("armor"),
            "breastplate" to listOf("armor"),
            "faulds" to listOf("armor"),
            "gauntlets" to listOf("armor"),
            "shoulder_armor" to listOf("armor"),
            "on_chair" to listOf("chair", "sitting"),
            "office_chair" to listOf("chair"),
            "on_bed" to listOf("bed"),
            "bikini" to listOf("swimsuit"),
            "one-piece_swimsuit" to listOf("swimsuit"),
            "school_swimsuit" to listOf("swimsuit"),
            "kimono" to listOf("japanese_clothes"),
            "yukata" to listOf("japanese_clothes"),
            "beret" to listOf("hat"),
            "mob_cap" to listOf("hat"),
            "witch_hat" to listOf("hat"),
            "santa_hat" to listOf("hat"),
            "military_uniform" to listOf("military"),
            "sniper_rifle" to listOf("rifle", "gun", "weapon"),
            "rifle" to listOf("gun", "weapon"),
            "cleavage" to listOf("breasts"),
            "profile" to listOf("from_side"),
            "ribbon" to listOf("bow"),
            "single_braid" to listOf("braid"),
            "twin_braids" to listOf("braid", "long_hair"),
            "star_(sky)" to listOf("starry_sky"),
            "starry_sky" to listOf("sky"),
            "peach" to listOf("fruit", "food"),
            "fruit" to listOf("food"),
            "mosaic_censoring" to listOf("censored"),
            "ejaculation" to listOf("cum"),
            "pussy" to listOf("vaginal"),
            "leotard_aside" to listOf("clothing_aside"),
            "breasts_out" to listOf("nipples"),
            "topless" to listOf("breasts", "nipples"),
            "thigh_boots" to listOf("boots"),
        )

        private val FEMALE_CUES = setOf(
            "skirt", "dress", "miniskirt", "pleated_skirt", "long_skirt",
            "serafuku", "breasts", "large_breasts", "medium_breasts",
            "small_breasts", "panties", "bra", "underwear", "thighhighs",
            "pantyhose", "hair_ribbon", "hair_ornament", "maid", "apron",
            "swimsuit", "bikini", "yukata", "kimono", "zettai_ryouiki",
            "cleavage", "high_heels", "twintails", "nude", "topless",
            "breasts_out", "pussy", "vaginal", "leotard_aside",
        )

        private val MALE_CUES = setOf(
            "1boy", "male", "male_focus", "faceless_male", "beard",
            "muscular", "penis", "testicles", "erection", "ejaculation", "cum",
        )

        private val STRONG_MALE_CUES = setOf(
            "penis", "testicles", "erection", "ejaculation", "cum",
            "male_focus", "faceless_male", "beard", "muscular",
        )

        private val STRONG_FEMALE_CUES = setOf(
            "breasts", "large_breasts", "medium_breasts", "small_breasts",
            "pussy", "vaginal", "panties", "bra", "underwear",
        )

        private val MULTIPLE_TAGS = setOf(
            "multiple_girls", "2girls", "3girls", "4girls", "5girls", "6+girls",
        )

        private val GIRL_COUNT_TAGS = setOf("2girls", "3girls", "4girls", "5girls", "6+girls")
    }
}
