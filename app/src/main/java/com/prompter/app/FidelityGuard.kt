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
        preserveStatedColors: Boolean = true,
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

        val stated = statedTags.toSet()
        val people = peopleCount(text, stated)
        if (people.multiple) {
            remove("solo")
            if (femalePresent(text)) {
                remove("1girl")
                val expected = if (people.three) "3girls" else "2girls"
                for (tag in GIRL_COUNT_TAGS) {
                    if (tag != expected) remove(tag)
                }
                add(expected)
                add("multiple_girls")
            }
        } else if (people.single) {
            for (tag in GIRL_COUNT_TAGS) remove(tag)
            remove("multiple_girls")
            if ("1boy" !in current && "1girl" !in current &&
                femalePresent(text) && !malePresent(text)
            ) {
                add("1girl")
            }
            if ("1girl" in current && "1boy" !in current) add("solo")
        }

        // If the input names no clothing at all, clothing tags from the AI are
        // inventions even when they carry no color (for example school_uniform).
        if (!hasClothingCue(text)) {
            for (tag in current.toList()) {
                if (tag !in stated && isClothingTag(tag)) remove(tag)
            }
        }

        // A long compound hair alias can consume the ボブカット part, leaving
        // only the color tag. Add the style back when the input names it.
        if (BOB_CUE.containsMatchIn(text) && "bob_cut" !in current && isKnownTag("bob_cut")) {
            add("bob_cut")
        }

        // The AI sometimes drops a stated hair/eye/clothing attribute from its
        // tag line even though the prose keeps it. Restore those attributes so
        // the tags and the prose stay consistent.
        for (tag in statedTags) {
            if (tag in current || tag in removed || !isAttributeTag(tag) || !isKnownTag(tag)) continue
            add(tag)
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

        // 水色のインカラー / 赤い目 and similar color cues rule out a
        // greyscale image. The model sometimes adds greyscale for black+white
        // outfits, which contradicts the stated colors.
        if (NON_GREY_COLOR_CUE.containsMatchIn(text)) {
            remove("greyscale")
            remove("monochrome")
            remove("black_and_white")
        }

        // A camera angle that the input never mentions is an invention. Keep
        // the angles that the input actually states (左側から, 上から, ...).
        if (!CAMERA_CUE.containsMatchIn(text)) {
            for (tag in CAMERA_TAGS) remove(tag)
        }

        // 左側から見た / 横から is a side view, not a high-angle view.
        if (SIDE_CUE.containsMatchIn(text) && !ABOVE_CUE.containsMatchIn(text)) {
            remove("from_above")
            add("from_side")
        }

        // A color on a garment that is neither stated nor extracted is an
        // invention. Keep the garment tag, remove the color tag.
        for (tag in current.toList()) {
            val match = COLOR_CLOTHING_TAG.matchEntire(tag) ?: continue
            val color = match.groupValues[1]
            val noun = match.groupValues[2]
            if (noun !in CLOTHING_NOUNS) continue
            if ((!preserveStatedColors || tag !in stated) &&
                !colorStatedInJapanese(text, color, noun)
            ) {
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

        out = removeUnstatedClothingSentences(text, out)
        out = removeUnstatedClothingColors(text, out, offlineTags)
        out = normalizeHairPhrases(text, out, offlineTags)
        out = normalizeEyePhrases(text, out, offlineTags)
        val people = peopleCount(text, offlineTags.toSet())
        if (people.single) out = singularizePrompt(out)
        out = out
            .replace(
                Regex(
                    "(?i)\\b(?:her|his)\\s+hair\\s+is\\s+not\\s+specified,\\s+so\\s+" +
                        "it\\s+is\\s+described\\s+simply\\s+as\\s+(?:her|his)\\s+hair,?\\s*",
                ),
                "Her hair ",
            )
            .replace(Regex("(?i)\\bHer hair\\s+falling\\b"), "Her hair falls")
            .replace(
                Regex(
                    "(?i)\\b(?:her|his)\\s+eyes\\s+are\\s+not\\s+specified,\\s+so\\s+" +
                        "(?:she|he)\\s+is\\s+left\\s+as\\s+a\\s+neutral\\s+gaze\\s+",
                ),
                "Her gaze is ",
            )
            .replace(
                Regex(
                    "(?i)\\b(?:her|his)\\s+hands\\s+are\\s+not\\s+specified,\\s+so\\s+" +
                        "(?:she|he)\\s+hang\\s+naturally\\s+by\\s+(?:her|his)\\s+sides\\b",
                ),
                "Her hands hang naturally by her sides",
            )
            .replace(Regex("(?i)\\bwith hair and eyes\\b"), "with delicate features")
            .replace(Regex("(?i)\\bher hair and eyes\\b"), "her features")
        out = stripUnstatedHair(text, out)
        return out
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

    /**
     * The model sometimes invents a hair length or style when the input says
     * nothing about hair at all ("strands falling around her shoulders").
     * v13 still did this on ID24 and ID26, so remove those claims
     * deterministically. Hair phrases that the input supports are untouched.
     */
    private fun stripUnstatedHair(text: String, prompt: String): String {
        if (HAIR_CUE.containsMatchIn(text)) return prompt
        return prompt
            .replace(
                Regex(
                    "(?i)\\b(hair)\\s+is\\s+(?:styled|arranged|worn)\\s+in\\s+a\\s+" +
                        "manner\\s+that\\s+frames\\s+(her|his)\\s+face\\s+naturally\\b",
                ),
                "$1 frames $2 face",
            )
            .replace(
                Regex(
                    "(?i),?\\s*with\\s+(?:her\\s+|his\\s+)?(?:strands|locks)\\s+" +
                        "(?:gently\\s+|softly\\s+)?falling\\s+" +
                        "(?:around|past|down|over|to)\\s+(?:her|his)\\s+shoulders\\b",
                ),
                "",
            )
            .replace(
                Regex(
                    "(?i),?\\s*falling\\s+(?:naturally\\s+|softly\\s+|gently\\s+)?" +
                        "(?:around|past|down|over|to)\\s+(?:her|his)\\s+shoulders" +
                        "(?:\\s+in\\s+a\\s+[^.]*?\\s+manner)?" +
                        "(?:\\s+that\\s+complements\\s+([^.,;]+))?",
                ),
            ) { match ->
                val complements = match.groupValues[1].trim()
                if (complements.isEmpty()) "" else ", in keeping with $complements"
            }
            .replace(
                Regex(
                    "(?i)\\bhair\\s+falls\\s+(?:naturally\\s+|softly\\s+|gently\\s+)?" +
                        "(?:around|past|down|over|to)\\s+(her|his)\\s+shoulders" +
                        "(?:\\s+in\\s+a\\s+[^.]*?\\s+manner)?",
                ),
                "hair frames $1 face",
            )
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

    private fun removeUnstatedClothingSentences(text: String, prompt: String): String {
        if (hasClothingCue(text)) return prompt
        return prompt
            .split(Regex("(?<=[.!?])\\s+"))
            .filterNot { CLOTHING_SENTENCE.containsMatchIn(it) }
            .joinToString(" ")
    }

    private fun singularizePrompt(prompt: String): String {
        var out = prompt
            .replace(Regex("(?i)\\btwo girls are\\b"), "a girl is")
            .replace(Regex("(?i)\\bthe two girls are\\b"), "the girl is")
            .replace(Regex("(?i)\\bboth girls are\\b"), "the girl is")
            .replace(Regex("(?i)\\btwo girls\\b"), "a girl")
            .replace(Regex("(?i)\\bthe two girls\\b"), "the girl")
            .replace(Regex("(?i)\\bboth girls\\b"), "the girl")
            .replace(Regex("(?i)\\bthe girls\\b"), "the girl")
            .replace(Regex("(?i)\\bone girl\\b"), "she")
            .replace(Regex("(?i)\\bthe other\\b"), "she")
            .replace(Regex("(?i)\\bboth possessing\\b"), "possessing")
            .replace(Regex("(?i)\\bboth have\\b"), "she has")
            .replace(Regex("(?i)\\bboth are\\b"), "she is")
            .replace(Regex("(?i)\\btheir bodies\\b"), "her body")
            .replace(Regex("(?i)\\btheir faces\\b"), "her face")
            .replace(Regex("(?i)\\btheir hands\\b"), "her hands")
            .replace(Regex("(?i)\\btheir hair\\b"), "her hair")
            .replace(Regex("(?i)\\btheir eyes\\b"), "her eyes")
            .replace(Regex("(?i)\\btheir\\b"), "her")
            .replace(Regex("(?i)\\bthey\\b"), "she")
            .replace(Regex("(?i)\\bthem\\b"), "her")
            .replace(Regex("(?i)\\bschool uniforms\\b"), "school uniform")
            .replace(Regex("(?i)\\buniforms\\b"), "uniform")
            .replace(Regex("(?i)\\bpositioned close together\\b"), "positioned calmly")
            .replace(Regex("(?i)\\bstanding side-by-side\\b"), "standing gracefully")
            .replace(Regex("(?i)\\bside-by-side\\b"), "gracefully")
            .replace(Regex("(?i)\\beach other's\\b"), "her own")
            .replace(Regex("(?i)\\beach other\\b"), "herself")
        out = out
            .replace(Regex("(?i)\\ba girl are\\b"), "a girl is")
            .replace(Regex("(?i)\\bthe girl are\\b"), "the girl is")
            .replace(Regex("(?i)\\bshe are\\b"), "she is")
            .replace(Regex("(?i)\\bshe have\\b"), "she has")
            .replace(Regex("(?i)\\bshe is both\\b"), "she is")
            .replace(Regex("(^|[.!?]\\s+)([a-z])")) { match ->
                match.groupValues[1] + match.groupValues[2].uppercase(Locale.ROOT)
            }
        return out
    }

    private data class PeopleCount(
        val multiple: Boolean,
        val single: Boolean,
        val three: Boolean,
    )

    private fun peopleCount(text: String, statedTags: Set<String>): PeopleCount {
        val three = THREE_PEOPLE.containsMatchIn(text)
        val inputMultiple = three || twoPeople(text)
        val inputSingle = singlePerson(text)
        val statedMultiple = statedTags.any {
            it in GIRL_COUNT_TAGS || it in BOY_COUNT_TAGS ||
                it == "multiple_girls" || it == "multiple_boys"
        }
        val statedSingle = statedTags.any { it == "1girl" || it == "1boy" || it == "solo" }
        val multiple = inputMultiple || (!inputSingle && statedMultiple && !statedSingle)
        val single = !multiple && (inputSingle || statedSingle)
        return PeopleCount(multiple = multiple, single = single, three = three)
    }

    private fun hasClothingCue(text: String): Boolean = CLOTHING_CUE.containsMatchIn(text)

    private fun isClothingTag(tag: String): Boolean {
        if (tag in CLOTHING_TAGS) return true
        return tag.substringAfterLast('_') in CLOTHING_NOUNS
    }

    private fun isAttributeTag(tag: String): Boolean =
        tag.endsWith("_hair") ||
            tag == "bob_cut" ||
            tag.endsWith("_eyes") ||
            tag in RESTORABLE_SUBJECT_TAGS ||
            isClothingTag(tag)

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

    private fun malePresent(text: String): Boolean = MALE_CUE.containsMatchIn(text)

    private fun singlePerson(text: String): Boolean {
        if (twoPeople(text)) return false
        if (SINGLE_CUE.containsMatchIn(text)) return true
        return when (personMentions(text)) {
            0 -> femalePresent(text) || PERSON_FEATURE_CUE.containsMatchIn(text)
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
    private val BOY_COUNT_TAGS = setOf("2boys", "3boys", "4boys", "5boys", "6+boys")

    private val FEMALE_CUE = Regex(
        "少女|女の子|女性|彼女|お嬢さん|めいど|どれす|すかーと|ぶらうす|" +
            "りぼん|にーそ|みずぎ|びきに|きもの|ゆかた|胸元|乳房|乳首|おっぱい|" +
            "髪|目|瞳|顔|表情|肌|唇|口",
    )
    private val MALE_CUE = Regex("少年|男の子|男性|ぺにす|男性器|勃起|精液")
    private val PERSON_FEATURE_CUE = Regex(
        "髪|目|瞳|顔|表情|肌|唇|口|首|腕|手|脚|足|胸|乳房|乳首|お腹|背中|腰",
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
    private val SIDE_CUE = Regex("左側から|右側から|左から|右から|横から|側面|横顔|よこから")
    private val CAMERA_CUE = Regex(
        "左側から|右側から|左から|右から|横から|側面|横顔|よこから|" +
            "上から見|上から撮|上から描|下から見|下から撮|俯瞰|見下ろ|煽り|あおり|" +
            "ローアングル|ハイアングル|正面|後ろ姿|背面|斜めから|アングル|ふかん|みおろ",
    )
    private val CAMERA_TAGS = setOf("from_above", "from_below", "from_side", "from_behind", "from_front")
    private val NON_GREY_COLOR_CUE = Regex(
        "水色|青|あお|赤|あか|金|きん|紫|むらさき|ピンク|ぴんく|桃色|緑|みどり|" +
            "黄色|きいろ|オレンジ|おれんじ|茶色|ちゃいろ|銀|ぎん",
    )
    private val HAIR_CUE = Regex(
        "髪|ヘア|へあ|ぼぶ|ボブ|ロングヘア|ショートヘア|セミロング|ポニーテール|" +
            "ツインテール|おかっぱ|ハーフアップ|三つ編み|インカラー|前髪|後ろ髪|" +
            "巻き髪|ストレート|ウェーブ|カーリー",
    )
    private val BOB_CUE = Regex("ぼぶ|ボブ|bob")

    // Objects and composition tags that the AI drops from its tag line even
    // though the offline conversion extracted them from the input. They are
    // not hair/eye/clothing attributes, so they need an explicit allow-list.
    private val RESTORABLE_SUBJECT_TAGS = setOf(
        "tray", "holding_tray", "juice", "omurice", "split_screen",
        "drink", "glass", "food", "cooking",
    )
    private val ABOVE_CUE = Regex("上から見|上から撮|上から描|俯瞰|見下ろ|高い位置|ふかん|みおろ")

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
    private val CLOTHING_TAGS = setOf(
        "maid", "school_uniform", "serafuku", "japanese_clothes",
        "military_uniform", "nurse", "waitress", "chef", "apron", "dress",
    )
    private val CLOTHING_CUE = Regex(
        "服|どれす|しゃつ|すかーと|ぶらうす|じゃけっと|こーと|えぷろん|" +
            "ずぼん|ぱんつ|くつした|にーそ|くつ|ぶーつ|てぶくろ|ぼうし|べすと|" +
            "みずぎ|びきに|きもの|ゆかた|せいふく|がくせいふく|めいど|" +
            "かーでぃがん|ぱーかー|せーたー|まふらー|ねくたい|りぼん|ふりる|" +
            "れーす|そで|えり|とりむ|ぬの|きじ|着て|着ている|身につけ|着用|" +
            "衣装|装束",
    )
    private val CLOTHING_SENTENCE = Regex(
        "(?i)\\b(?:dress(?:es)?|shirts?|skirts?|blouses?|jackets?|coats?|aprons?|" +
            "pants|trousers|shorts|socks?|thighhighs|pantyhose|shoes?|boots?|gloves?|" +
            "hats?|vests?|swimsuits?|bikinis?|kimonos?|yukatas?|uniforms?|cardigans?|" +
            "hoodies?|sweaters?|scarves?|neckties?|ribbons?|frills?|lace|sleeves?|" +
            "collars?|trim|fabrics?|attire|outfits?|dressed|garments?|clothes|clothing)\\b",
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
