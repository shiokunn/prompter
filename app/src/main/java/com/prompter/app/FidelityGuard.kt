package com.prompter.app

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic corrections for the two fidelity problems found in the M7
 * human evaluation:
 *
 *  - the natural-language prompt invents hair length, eye color, clothing
 *    colors, or extra people;
 *  - the tag list contradicts the input (for example `cleavage` while the
 *    chest is covered, or `solo, 1girl` for a two-girl composition).
 *
 * The AI still does the writing. This guard only removes details that are not
 * present in the input, so it is deliberately conservative and testable on the
 * JVM.
 */
object FidelityGuard {
    data class TagCorrection(
        val tags: List<String>,
        val removed: List<String>,
        val added: List<String>,
    )

    fun correctTags(
        input: String,
        tags: List<String>,
        isKnownTag: (String) -> Boolean,
        statedTags: List<String> = tags,
    ): TagCorrection {
        val text = normalizeJapanese(input)
        val current = tags.toMutableList()
        val removed = LinkedHashSet<String>()
        val added = LinkedHashSet<String>()

        fun remove(tag: String) {
            if (current.remove(tag)) removed.add(tag)
        }

        fun add(tag: String) {
            if (tag !in current && isKnownTag(tag)) {
                current.add(tag)
                added.add(tag)
            }
        }

        val three = THREE_PEOPLE.containsMatchIn(text)
        val multiple = three || twoPeople(text)
        if (multiple) {
            remove("solo")
            if (femalePresent(text)) {
                remove("1girl")
                val expected = if (three) "3girls" else "2girls"
                for (tag in GIRL_COUNT_TAGS) {
                    if (tag != expected) remove(tag)
                }
                add(expected)
                add("multiple_girls")
            }
        } else if (singlePerson(text)) {
            for (tag in GIRL_COUNT_TAGS) remove(tag)
            remove("multiple_girls")
            if ("1boy" !in current && "1girl" !in current && femalePresent(text)) {
                add("1girl")
            }
            if ("1girl" in current && "1boy" !in current) add("solo")
        }

        // 胸元を隠す / 露出が少ない must not carry cleavage or exposed-nipple
        // tags. `breasts` itself is still a valid body tag.
        if (concealsChest(text) || "covering_breasts" in current) {
            remove("cleavage")
            remove("breasts_out")
            remove("nipples")
            remove("sideboob")
            remove("underboob")
            remove("cleavage_cutout")
            add("covering_breasts")
        }

        // 上目遣い is looking up, not aiming a weapon.
        if (!AIMING_CUE.containsMatchIn(text)) {
            remove("aiming")
            remove("aiming_at_viewer")
        }

        // 半分 alone (縦半分, 半分閉じた目) must not become half_updo.
        if (!HALF_UPDO_CUE.containsMatchIn(text)) remove("half_updo")

        // A color on a garment that is neither stated nor extracted is an
        // invention. Keep the garment tag, remove the color tag.
        val stated = statedTags.toSet()
        for (tag in current.toList()) {
            val match = COLOR_CLOTHING_TAG.matchEntire(tag) ?: continue
            val color = match.groupValues[1]
            val noun = match.groupValues[2]
            if (noun !in CLOTHING_NOUNS) continue
            if (tag !in stated && !colorStatedInJapanese(text, color, noun)) {
                remove(tag)
            }
        }

        return TagCorrection(
            tags = current,
            removed = removed.toList(),
            added = added.toList(),
        )
    }

    /**
     * Removes prose that directly contradicts the input. This is intentionally
     * a narrow set of replacements; the main fidelity work belongs in the v10
     * system prompt.
     */
    fun sanitizePrompt(
        input: String,
        prompt: String,
        offlineTags: List<String>,
    ): String {
        val text = normalizeJapanese(input)
        var out = prompt

        if (concealsChest(text)) {
            out = out
                .replace(
                    Regex("(?i)\\b(?:deep|exposed|visible|prominent|revealing|ample)\\s+cleavage\\b"),
                    "modest neckline",
                )
                .replace(Regex("(?i)\\bcleavage\\b"), "neckline")
                .replace(
                    Regex("(?i)\\b(?:exposed|bare|uncovered)\\s+(?:breasts|chest)\\b"),
                    "covered chest",
                )
                .replace(
                    Regex("(?i)\\b(?:breasts|chest)\\s+(?:are|is)\\s+(?:exposed|bare|uncovered)\\b"),
                    "chest is covered",
                )
                .replace(Regex("(?i)\\brevealing\\s+neckline\\b"), "modest neckline")
        }

        out = removeUnstatedClothingColors(text, out, offlineTags)
        out = normalizeHairPhrases(text, out, offlineTags)
        out = normalizeEyePhrases(text, out, offlineTags)
        return out
            .replace(Regex("(?i)\\bwith hair and eyes\\b"), "with delicate features")
            .replace(Regex("(?i)\\bher hair and eyes\\b"), "her features")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.;:])"), "$1")
            .replace(Regex("(?i)\\ba\\s+(apron|orange|aqua|outfit)\\b"), "an $1")
            .trim()
    }

    private fun removeUnstatedClothingColors(
        text: String,
        prompt: String,
        offlineTags: List<String>,
    ): String {
        val tags = offlineTags.toSet()
        return COLOR_BEFORE_CLOTHING.replace(prompt) { match ->
            val color = match.groupValues[1].lowercase(Locale.ROOT)
            val noun = match.groupValues[3].lowercase(Locale.ROOT)
            val stated = colorTagStated(color, noun, tags) ||
                colorStatedInJapanese(text, color, noun)
            if (stated) match.value else match.groupValues[2]
        }
    }

    private fun normalizeHairPhrases(
        text: String,
        prompt: String,
        offlineTags: List<String>,
    ): String {
        val tags = offlineTags.toSet()
        return HAIR_PHRASE.replace(prompt) { match ->
            val words = match.groupValues[1]
                .split(Regex("[,\\s]+"))
                .filter { it.isNotBlank() }
            val kept = words.filter { word ->
                val lower = word.lowercase(Locale.ROOT)
                when {
                    lower in HAIR_ATTRIBUTE_WORDS -> hairAttributeStated(lower, tags)
                    lower in COLOR_WORDS_SET -> {
                        hairColorTagStated(lower, tags) ||
                            colorStatedInJapanese(text, lower, "hair")
                    }
                    else -> false
                }
            }
            if (kept.isEmpty()) "hair" else kept.joinToString(" ") + " hair"
        }
    }

    private fun normalizeEyePhrases(
        text: String,
        prompt: String,
        offlineTags: List<String>,
    ): String {
        val tags = offlineTags.toSet()
        return EYE_PHRASE.replace(prompt) { match ->
            val color = match.groupValues[1].lowercase(Locale.ROOT)
            val stated = eyeColorTagStated(color, tags) ||
                colorStatedInJapanese(text, color, "eyes")
            if (stated) match.value else "eyes"
        }
    }

    private fun colorTagStated(color: String, noun: String, tags: Set<String>): Boolean =
        colorAliases(color).any { "${it}_$noun" in tags }

    private fun hairColorTagStated(color: String, tags: Set<String>): Boolean =
        colorAliases(color).any { "${it}_hair" in tags }

    private fun eyeColorTagStated(color: String, tags: Set<String>): Boolean =
        colorAliases(color).any { "${it}_eyes" in tags }

    private fun hairAttributeStated(word: String, tags: Set<String>): Boolean = when (word) {
        "very" -> "very_long_hair" in tags
        "long" -> "long_hair" in tags || "very_long_hair" in tags
        "very long" -> "very_long_hair" in tags || "long_hair" in tags
        "short" -> "short_hair" in tags || "bob_cut" in tags
        "medium" -> "medium_hair" in tags
        "shoulder-length" -> "medium_hair" in tags || "long_hair" in tags
        "straight" -> "straight_hair" in tags
        "flowing" -> "long_hair" in tags || "very_long_hair" in tags
        "wavy" -> "wavy_hair" in tags
        "curly" -> "curly_hair" in tags
        else -> true
    }

    private fun colorAliases(color: String): List<String> = when (color) {
        "gray" -> listOf("grey", "gray")
        "grey" -> listOf("grey", "gray")
        "yellow" -> listOf("yellow", "blonde")
        "blonde" -> listOf("blonde", "yellow")
        "gold" -> listOf("gold", "blonde")
        "aqua" -> listOf("aqua", "blue")
        else -> listOf(color)
    }

    private fun colorStatedInJapanese(text: String, color: String, noun: String): Boolean {
        val colors = JAPANESE_COLORS[color].orEmpty()
        if (colors.isEmpty()) return false
        val nouns = JAPANESE_NOUNS[noun].orEmpty()
        if (nouns.isEmpty()) return false
        for (colorSurface in colors) {
            for (nounSurface in nouns) {
                val pattern = Regex(
                    Regex.escape(colorSurface) + ".{0,8}" + Regex.escape(nounSurface) + "|" +
                        Regex.escape(nounSurface) + ".{0,8}" + Regex.escape(colorSurface),
                )
                if (pattern.containsMatchIn(text)) return true
            }
        }
        return false
    }

    private fun concealsChest(text: String): Boolean = CONCEAL_CUE.containsMatchIn(text)

    private fun femalePresent(text: String): Boolean = FEMALE_CUE.containsMatchIn(text)

    private fun singlePerson(text: String): Boolean {
        if (SINGLE_CUE.containsMatchIn(text)) return true
        return when (personMentions(text)) {
            0 -> femalePresent(text)
            1 -> true
            else -> false
        }
    }

    private fun twoPeople(text: String): Boolean {
        if (TWO_PEOPLE.containsMatchIn(text)) return true
        if (personMentions(text) < 2) return false
        return "それぞれ" in text ||
            ("右半分" in text && "左半分" in text) ||
            "一方" in text ||
            "もう一方" in text
    }

    private fun personMentions(text: String): Int =
        PERSON_NOUNS.findAll(text).count()

    private fun normalizeJapanese(value: String): String {
        val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
        val out = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            val code = ch.code
            if (code in 0x30A1..0x30F6) {
                out.append((code - 0x60).toChar())
            } else {
                out.append(ch)
            }
        }
        return out.toString()
    }

    private val PERSON_NOUNS = Regex("少女|女の子|女性|男の子|少年|男性")
    private val TWO_PEOPLE = Regex("二人|２人|2人|ふたり|両者|複数|女の子たち|少女たち|女性たち|彼女たち|彼女ら")
    private val THREE_PEOPLE = Regex("三人|３人|3人|みたり")
    private val SINGLE_CUE = Regex("一人|１人|ひとり|単独|そろ")
    private val GIRL_COUNT_TAGS = setOf("2girls", "3girls", "4girls", "5girls", "6+girls")

    private val FEMALE_CUE = Regex(
        "少女|女の子|女性|彼女|お嬢さん|めいど|どれす|すかーと|ぶらうす|" +
            "りぼん|にーそ|みずぎ|びきに|きもの|ゆかた|胸元|乳房|乳首|おっぱい",
    )
    private val CONCEAL_CUE = Regex(
        "胸元?を(?:隠|覆)|胸を(?:隠|覆)|胸元は隠|" +
            "露出が(?:非常に)?少ない|肌の露出が少ない|控えめな胸元",
    )
    private val AIMING_CUE = Regex(
        "狙|照準|構え|弓を|銃を|武器を|矢を|あろー|ぼうがん|すないぱー|らいふる",
    )
    private val HALF_UPDO_CUE = Regex(
        "はーふあっぷ|half[- ]?updo|半分まとめ|半分にまとめ|半分だけまとめ|半分アップ",
    )

    private val COLOR_CLOTHING_TAG = Regex(
        "^(white|black|red|blue|green|yellow|blonde|purple|pink|brown|grey|gray|" +
            "silver|gold|orange|aqua|navy|beige|dark)_(.+)$",
    )
    private val CLOTHING_NOUNS = setOf(
        "shirt", "dress", "skirt", "blouse", "jacket", "coat", "apron", "pants",
        "trousers", "shorts", "socks", "thighhighs", "pantyhose", "shoes", "boots",
        "gloves", "hat", "vest", "swimsuit", "bikini", "kimono", "yukata",
        "uniform", "cardigan", "hoodie", "sweater", "scarf", "necktie", "ribbon",
        "frills", "lace", "sleeves", "collar", "trim", "fabric",
    )

    private val COLOR_WORDS =
        "white|black|red|blue|green|yellow|blonde|purple|pink|brown|grey|gray|" +
            "silver|gold|orange|aqua|navy|beige|dark"
    private val CLOTHING_WORDS =
        "shirt|dress|skirt|blouse|jacket|coat|apron|pants|trousers|shorts|socks|" +
            "thighhighs|pantyhose|shoes|boots|gloves|hat|vest|swimsuit|bikini|" +
            "kimono|yukata|uniform|cardigan|hoodie|sweater|scarf|necktie|ribbon|" +
            "frills|lace|sleeves|collar|trim|fabric"
    private val COLOR_BEFORE_CLOTHING = Regex(
        "(?i)\\b($COLOR_WORDS)((?:\\s+[a-z-]+){0,2}\\s+($CLOTHING_WORDS))\\b",
    )

    private val HAIR_ATTRIBUTE_WORDS = setOf(
        "very", "long", "short", "medium", "shoulder-length",
        "straight", "flowing", "wavy", "curly",
    )
    private val HAIR_PHRASE = Regex(
        "(?i)\\b((?:(?:very\\s+long|shoulder-length|long|short|medium|straight|" +
            "flowing|wavy|curly|$COLOR_WORDS)\\b(?:\\s*,\\s*|\\s+)){1,5})hair\\b",
    )
    private val COLOR_WORDS_SET: Set<String> =
        COLOR_WORDS.split('|').map { it.lowercase(Locale.ROOT) }.toSet()
    private val EYE_PHRASE = Regex("(?i)\\b($COLOR_WORDS)\\s+eyes\\b")

    private val JAPANESE_COLORS: Map<String, List<String>> = mapOf(
        "white" to listOf("白", "しろ"),
        "black" to listOf("黒", "くろ"),
        "red" to listOf("赤", "あか"),
        "blue" to listOf("青", "あお"),
        "green" to listOf("緑", "みどり"),
        "yellow" to listOf("黄色", "黄", "きいろ", "き"),
        "blonde" to listOf("金", "金色", "黄色", "黄", "きん", "きんいろ", "きいろ", "き"),
        "purple" to listOf("紫", "むらさき"),
        "pink" to listOf("ピンク", "ぴんく", "桃色", "ももいろ"),
        "brown" to listOf("茶", "茶色", "ちゃ", "ちゃいろ"),
        "grey" to listOf("灰色", "グレー", "ぐれー", "はいいろ"),
        "gray" to listOf("灰色", "グレー", "ぐれー", "はいいろ"),
        "silver" to listOf("銀", "銀色", "ぎん", "ぎんいろ"),
        "gold" to listOf("金", "金色", "きん", "きんいろ"),
        "orange" to listOf("オレンジ", "おれんじ", "橙色", "だいだいいろ"),
        "aqua" to listOf("水色", "みずいろ"),
        "navy" to listOf("紺", "こん", "濃紺", "のうこん"),
        "beige" to listOf("ベージュ", "べーじゅ"),
    )

    private val JAPANESE_NOUNS: Map<String, List<String>> = mapOf(
        "hair" to listOf("髪", "かみ", "ヘア", "へあ"),
        "eyes" to listOf("目", "め", "瞳", "ひとみ"),
        "shirt" to listOf("シャツ", "しゃつ"),
        "dress" to listOf("ドレス", "どれす"),
        "skirt" to listOf("スカート", "すかーと"),
        "blouse" to listOf("ブラウス", "ぶらうす"),
        "jacket" to listOf("ジャケット", "じゃけっと"),
        "coat" to listOf("コート", "こーと"),
        "apron" to listOf("エプロン", "えぷろん"),
        "pants" to listOf("ズボン", "ずぼん", "パンツ", "ぱんつ"),
        "trousers" to listOf("ズボン", "ずぼん", "パンツ", "ぱんつ"),
        "shorts" to listOf("ショートパンツ", "しょーとぱんつ"),
        "socks" to listOf("靴下", "くつした", "ソックス", "そっくす"),
        "thighhighs" to listOf("ニーソ", "にーそ", "ニーハイ", "にーはい", "ハイソックス", "はいそくす"),
        "pantyhose" to listOf("ストッキング", "すとっきんぐ"),
        "shoes" to listOf("靴", "くつ"),
        "boots" to listOf("ブーツ", "ぶーつ"),
        "gloves" to listOf("手袋", "てぶくろ"),
        "hat" to listOf("帽子", "ぼうし"),
        "vest" to listOf("ベスト", "べすと"),
        "swimsuit" to listOf("水着", "みずぎ"),
        "bikini" to listOf("ビキニ", "びきに"),
        "kimono" to listOf("着物", "きもの"),
        "yukata" to listOf("浴衣", "ゆかた"),
        "uniform" to listOf("制服", "せいふく", "学生服", "がくせいふく", "ユニフォーム", "ゆにふぉーむ"),
        "cardigan" to listOf("カーディガン", "かーでぃがん"),
        "hoodie" to listOf("パーカー", "ぱーかー"),
        "sweater" to listOf("セーター", "せーたー"),
        "scarf" to listOf("マフラー", "まふらー"),
        "necktie" to listOf("ネクタイ", "ねくたい"),
        "ribbon" to listOf("リボン", "りぼん"),
        "frills" to listOf("フリル", "ふりる"),
        "lace" to listOf("レース", "れーす"),
        "sleeves" to listOf("袖", "そで"),
        "collar" to listOf("襟", "えり", "カラー", "からー"),
        "trim" to listOf("トリム", "とりむ"),
        "fabric" to listOf("布", "ぬの", "生地", "きじ", "絹", "きぬ"),
    )
}
