package com.prompter.app

/**
 * Turns the dictionary tags into a PixAI DiT-friendly English prompt.
 *
 * The output follows the planned order: subject, hair/eyes, clothing, pose,
 * scene, mood, style, then any explicit or otherwise ungrouped tags. Tag mode
 * uses the exact tag strings instead, so adult tags are never filtered.
 */
object PromptComposer {
    private val HAIR_SIZE = listOf("very_long_hair", "long_hair", "medium_hair", "short_hair")
    private val HAIR_STYLE = setOf(
        "twintails", "twin_braids", "single_braid", "braid", "ponytail",
        "hair_bun", "single_hair_bun", "drill_hair", "wavy_hair", "ahoge",
        "hair_over_one_eye", "hair_between_eyes",
    )
    private val HAIR_ADJECTIVE = setOf("wavy_hair", "drill_hair")

    private val EYE_TAGS = setOf(
        "heterochromia", "eyelashes", "eyeshadow", "eyebrows",
    )

    private val CLOTHING_GENERIC = setOf(
        "dress", "shirt", "skirt", "ribbon", "panties", "bra", "underwear",
        "thighhighs", "pantyhose", "socks", "gloves", "hat", "wings", "bow",
        "necktie", "vest", "coat", "swimsuit", "bikini", "yukata", "kimono",
        "apron", "shoes", "boots", "scarf", "necklace", "earrings",
        "sailor_collar", "leotard", "bodysuit", "camisole", "blouse",
        "japanese_clothes", "footwear", "armor", "pants", "shorts", "uniform",
        "school_uniform", "military_uniform", "hoodie", "sweater", "cardigan",
        "jacket", "veil", "mask", "eyepatch", "bandages", "hairband",
        "hair_ornament", "hair_ribbon", "thigh_strap", "cape", "capelet",
        "cloak", "helmet", "armor", "breastplate", "shoulder_armor",
        "serafuku", "one-piece_swimsuit", "school_swimsuit",
    )

    private val CLOTHING_SUFFIXES = listOf(
        "_dress", "_shirt", "_skirt", "_uniform", "_swimsuit", "_bikini",
        "_kimono", "_yukata", "_coat", "_jacket", "_hoodie", "_sweater",
        "_cardigan", "_blouse", "_camisole", "_leotard", "_bodysuit",
        "_apron", "_panties", "_bra", "_underwear", "_thighhighs",
        "_pantyhose", "_socks", "_boots", "_shoes", "_sandals", "_hat",
        "_cap", "_beret", "_cape", "_capelet", "_cloak", "_gloves",
        "_ribbon", "_necktie", "_bowtie", "_scarf", "_necklace", "_earrings",
        "_belt", "_armor", "_breastplate", "_shoulder_armor", "_thigh_strap",
        "_hairband", "_hair_ornament", "_hair_ribbon", "_veil", "_mask",
        "_eyepatch", "_bandages", "_collar", "_leash", "_handcuffs",
        "_chain", "_rope", "_blindfold", "_gag", "_footwear",
    )

    private val NO_ARTICLE = setOf(
        "thighhighs", "pantyhose", "socks", "boots", "shoes", "sandals",
        "gloves", "wings", "earrings", "panties", "underwear", "pants",
        "shorts", "japanese_clothes", "armor", "shoulder_armor", "bandages",
        "high_heels", "loafers", "sneakers", "footwear", "handcuffs",
    )

    private val NO_ARTICLE_SUFFIXES = listOf(
        "_thighhighs", "_pantyhose", "_socks", "_boots", "_shoes", "_sandals",
        "_gloves", "_wings", "_earrings", "_panties", "_pants", "_shorts",
        "_high_heels", "_loafers", "_sneakers", "_footwear", "_handcuffs",
        "_armor",
    )

    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u')

    private val POSE = setOf(
        "standing", "sitting", "lying", "on_back", "on_stomach", "seiza",
        "kneeling", "squatting", "walking", "running", "jumping", "holding",
        "holding_weapon", "holding_sword", "looking_at_viewer", "looking_away",
        "looking_back", "from_side", "from_behind", "from_above", "from_below",
        "arms_up", "arms_behind_back", "crossed_arms", "hand_on_hip",
        "hands_on_hips", "hand_up", "waving", "pointing", "saluting",
        "dancing", "sleeping", "eating", "drinking", "reading", "writing",
        "playing", "fighting", "hugging", "kiss", "v_arms", "arm_support",
        "interlocked_fingers", "partially_unzipped", "leaning_forward",
        "leaning_back", "head_tilt", "turning_head",
    )

    private val SCENE = setOf(
        "indoors", "outdoors", "on_bed", "on_chair", "on_floor", "on_sofa",
        "classroom", "school", "park", "garden", "forest", "city", "street",
        "beach", "ocean", "sky", "night", "day", "sunset", "sunrise", "rain",
        "snow", "starry_sky", "star_(sky)", "moon", "sun", "window",
        "curtains", "bookshelf", "desk", "table", "bed", "chair", "couch",
        "mirror", "bathroom", "kitchen", "pool", "onsen", "shrine", "temple",
        "church", "castle", "ruins", "laboratory", "stage", "concert",
        "simple_background", "white_background", "black_background",
        "transparent_background", "gradient_background", "blue_sky",
        "cloud", "clouds", "field", "flower_field", "mountain", "river",
        "lake", "island", "waterfall", "bridge", "tower", "gate", "wall",
        "floor", "ceiling", "stairs", "roof", "lamp", "clock", "television",
        "computer", "phone", "smartphone", "umbrella", "swing", "broom",
    )

    private val MOOD = setOf(
        "smile", "light_smile", "grin", "laughing", "blush", "crying", "tears",
        "angry", "serious", "surprised", "open_mouth", "closed_eyes",
        "half-closed_eyes", "naughty_face", "expressionless", "smug", "sad",
        "fear", "sweat", "sweatdrop", "nervous", "embarrassed", "shy",
        "confident", "gentle", "calm", "playful", "melancholic",
    )

    private val STYLE = setOf(
        "highres", "absurdres", "anime_coloring", "monochrome", "greyscale",
        "watercolor", "sketch", "lineart", "realistic", "photorealistic",
        "3d", "pixel_art", "chibi", "official_art", "illustration",
        "detailed", "cinematic_lighting", "soft_lighting",
    )

    private val EXPLICIT = setOf(
        "nipples", "pussy", "penis", "sex", "nude", "topless", "breasts_out",
        "cum", "ejaculation", "vaginal", "censored", "mosaic_censoring",
        "pubic_hair", "areola", "areola_slip", "clothing_aside",
        "leotard_aside", "panties_aside", "bikini_aside", "bottomless",
        "no_panties", "spread_legs", "sex_from_behind", "missionary", "oral",
        "fellatio", "cunnilingus", "anal", "bondage", "bdsm", "handcuffs",
        "blindfold", "gag", "whip", "vibrator", "dildo", "condom", "creampie",
        "cum_in_pussy", "cum_on_body", "cum_on_face", "cum_on_breasts",
        "large_penis", "small_penis", "erection", "testicles", "leotard_aside",
    )

    private val FILLERS = listOf(
        "The composition keeps the subject as the clear focal point.",
        "Soft lighting and careful shading bring out the details of the hair, face, and clothing.",
        "The background supports the scene without distracting from the character.",
        "The overall finish is polished and suitable for a high-quality illustration.",
        "The colors are balanced, and the linework stays crisp at display size.",
        "The image is designed to read clearly as a single, coherent illustration.",
    )

    fun compose(tags: List<String>, extraContext: String? = null): String {
        val context = extraContext
            ?.trim()
            ?.trimEnd('.', '。', '!', '?')
            ?.takeIf { it.isNotBlank() }
        if (tags.isEmpty() && context == null) return ""
        if (tags.isEmpty()) {
            val sb = StringBuilder("An anime-style illustration.")
            sb.append(" Additional context from the input: ").append(context).append(".")
            sb.append(" ").append(styleSentence(emptyList()))
            return fitLength(sb.toString())
        }
        val ordered = tags.distinct()
        val set = ordered.toSet()
        val used = mutableSetOf<String>()

        val subject = subjectPhrase(set, used)
        val plural = isPluralSubject(set)
        val pronoun = pronounFor(set)
        val subjectLabel = if (plural) "They" else "The character"
        val subjectVerb = if (plural) "are" else "is"
        val hair = hairPhrase(ordered, set, used)
        val eyes = eyePhrase(ordered, used)
        val clothing = clothingPhrase(ordered, set, used)
        val pose = posePhrase(ordered, set, used, pronoun)
        val scene = scenePhrase(ordered, set, used)
        val mood = moodPhrase(ordered, set, used)
        val style = stylePhrase(ordered, used)

        val explicit = ordered.filter { it in EXPLICIT }
        used.addAll(explicit)
        val extras = ordered.filter { it !in used }

        val sb = StringBuilder()
        sb.append("An anime-style illustration of ").append(subject)
        val descriptors = mutableListOf<String>()
        if (hair.isNotEmpty()) descriptors.add(hair)
        if (eyes.isNotEmpty()) descriptors.add(eyes)
        if (descriptors.isNotEmpty()) {
            sb.append(" with ").append(joinNatural(descriptors))
        }
        sb.append(".")

        if (clothing.isNotEmpty()) {
            sb.append(" ").append(subjectLabel).append(" ").append(subjectVerb)
                .append(" wearing ").append(clothing).append(".")
        }
        if (pose.isNotEmpty()) {
            sb.append(" ").append(subjectLabel).append(" ").append(subjectVerb)
                .append(" ").append(pose).append(".")
        }
        if (scene.isNotEmpty()) {
            sb.append(" The scene is set ").append(scene).append(".")
        }
        if (mood.isNotEmpty()) {
            sb.append(" ").append(subjectLabel).append(" ")
                .append(if (plural) "have" else "has").append(" ").append(mood).append(".")
        }

        sb.append(" ").append(styleSentence(style))

        if (explicit.isNotEmpty()) {
            sb.append(" Explicit adult elements are present: ")
                .append(explicit.joinToString(", "))
                .append(".")
        }
        if (extras.isNotEmpty()) {
            sb.append(" Additional visual details: ")
                .append(extras.joinToString(", ") { natural(it) })
                .append(".")
        }
        if (context != null) {
            sb.append(" Additional context from the input: ").append(context).append(".")
        }

        return fitLength(sb.toString())
    }

    private fun fitLength(raw: String): String {
        var text = raw.replace(Regex("\\s+"), " ").trim()
        var words = wordCount(text)
        var fillerIndex = 0
        while (words < 80 && fillerIndex < FILLERS.size) {
            val candidate = "$text ${FILLERS[fillerIndex]}"
            if (wordCount(candidate) > 150) break
            text = candidate
            words = wordCount(text)
            fillerIndex += 1
        }
        if (words > 150) {
            text = text.split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .take(150)
                .joinToString(" ")
        }
        return text
    }

    private fun subjectPhrase(set: Set<String>, used: MutableSet<String>): String {
        val count = when {
            "6+girls" in set -> "six or more girls"
            "5girls" in set -> "five girls"
            "4girls" in set -> "four girls"
            "3girls" in set -> "three girls"
            "2girls" in set -> "two girls"
            "2boys" in set -> "two boys"
            "multiple_girls" in set -> "multiple girls"
            "1girl" in set && "1boy" in set -> "a girl and a boy"
            "1girl" in set -> "a girl"
            "1boy" in set -> "a boy"
            else -> "a character"
        }
        used.addAll(
            listOf(
                "1girl", "1boy", "2girls", "2boys", "3girls", "4girls",
                "5girls", "6+girls", "multiple_girls",
            ),
        )
        if ("hetero" in set) {
            used.add("hetero")
            return "a girl and a boy"
        }
        if ("yuri" in set && count.endsWith("girls")) {
            used.add("yuri")
            return count.replace("girls", "women")
        }
        val solo = "solo" in set
        if (solo) used.add("solo")
        return when {
            solo && count == "a girl" -> "a solo girl"
            solo && count == "a boy" -> "a solo boy"
            solo && count == "a character" -> "a solo character"
            else -> count
        }
    }

    private fun isPluralSubject(set: Set<String>): Boolean =
        set.any {
            it in setOf(
                "2girls", "2boys", "3girls", "4girls", "5girls", "6+girls",
                "multiple_girls", "hetero",
            )
        } || ("1girl" in set && "1boy" in set) ||
            ("yuri" in set && set.any { it in setOf("2girls", "3girls", "multiple_girls") })

    private fun pronounFor(set: Set<String>): String = when {
        isPluralSubject(set) -> "their"
        "1boy" in set && "1girl" !in set -> "his"
        "1girl" in set -> "her"
        else -> "their"
    }

    private fun hairPhrase(ordered: List<String>, set: Set<String>, used: MutableSet<String>): String {
        val size = HAIR_SIZE.firstOrNull { it in set }
        val color = ordered.firstOrNull {
            it.endsWith("_hair") && it !in HAIR_SIZE && it !in HAIR_STYLE && !it.startsWith("hair_")
        }
        val adjectives = ordered.filter { it in HAIR_ADJECTIVE }
        val styles = ordered.filter { it in HAIR_STYLE && it !in HAIR_ADJECTIVE }

        if (size == null && color == null && adjectives.isEmpty() && styles.isEmpty()) return ""
        val parts = mutableListOf<String>()
        if (size != null) {
            used.add(size)
            parts.add(
                when (size) {
                    "very_long_hair" -> "very long"
                    "long_hair" -> "long"
                    "medium_hair" -> "medium-length"
                    "short_hair" -> "short"
                    else -> ""
                },
            )
        }
        for (a in adjectives) {
            used.add(a)
            parts.add(if (a == "wavy_hair") "wavy" else "drill-curled")
        }
        if (color != null) {
            used.add(color)
            parts.add(if (color == "blonde_hair") "blonde" else natural(color.removeSuffix("_hair")))
        }
        val hair = (parts.filter { it.isNotBlank() }.joinToString(" ") + " hair").trim()
        val styleWords = styles.map { styleWord(it) }
        used.addAll(styles)
        return if (styleWords.isEmpty()) hair else "$hair styled in ${joinNatural(styleWords)}"
    }

    private fun eyePhrase(ordered: List<String>, used: MutableSet<String>): String {
        val eyes = ordered.filter { it.endsWith("_eyes") || it in EYE_TAGS }
        if (eyes.isEmpty()) return ""
        used.addAll(eyes)
        return joinNatural(eyes.map { eyeWord(it) })
    }

    private fun clothingPhrase(ordered: List<String>, set: Set<String>, used: MutableSet<String>): String {
        val candidates = ordered.filter {
            it in CLOTHING_GENERIC || CLOTHING_SUFFIXES.any { suffix -> it.endsWith(suffix) }
        }
        val filtered = candidates.filter { tag ->
            if (tag !in CLOTHING_GENERIC) return@filter true
            val representedBySpecific = candidates.any { other ->
                other != tag && (other.endsWith("_$tag") || other.endsWith(tag))
            }
            val representedByAlternative = when (tag) {
                "swimsuit" -> "bikini" in set || "one-piece_swimsuit" in set || "school_swimsuit" in set
                "japanese_clothes" -> "kimono" in set || "yukata" in set
                "footwear" -> candidates.any { it.endsWith("_footwear") }
                "armor" -> candidates.any { it.endsWith("_armor") }
                "uniform" -> "school_uniform" in set || "military_uniform" in set || "serafuku" in set
                "school_uniform" -> "serafuku" in set
                else -> false
            }
            !representedBySpecific && !representedByAlternative
        }.distinct()
        // Parents that are represented by a more specific tag are still
        // consumed; otherwise they would show up again in "additional details".
        used.addAll(candidates)
        return joinNatural(filtered.map { clothingWord(it) })
    }

    private fun posePhrase(
        ordered: List<String>,
        set: Set<String>,
        used: MutableSet<String>,
        pronoun: String,
    ): String {
        val all = ordered.filter { it in POSE }
        val selected = all.filter { tag ->
            when (tag) {
                "holding" -> "holding_weapon" !in set && "holding_sword" !in set
                "lying" -> "on_back" !in set && "on_stomach" !in set
                "closed_eyes" -> false
                else -> true
            }
        }
        used.addAll(all)
        return joinNatural(selected.map { poseWord(it, pronoun) })
    }

    private fun scenePhrase(ordered: List<String>, set: Set<String>, used: MutableSet<String>): String {
        val all = ordered.filter { it in SCENE }
        val selected = all.filter { tag ->
            when (tag) {
                "chair" -> "on_chair" !in set
                "bed" -> "on_bed" !in set
                "star_(sky)" -> "starry_sky" !in set
                "sky" -> "starry_sky" !in set
                else -> true
            }
        }.sortedWith(compareBy<String> { sceneRank(it) })
        used.addAll(all)
        return joinNatural(selected.map { sceneWord(it) })
    }

    private fun moodPhrase(ordered: List<String>, set: Set<String>, used: MutableSet<String>): String {
        val all = ordered.filter { it in MOOD }
        val selected = all.filter { tag ->
            when (tag) {
                "smile" -> "light_smile" !in set && "grin" !in set
                "closed_eyes" -> "half-closed_eyes" !in set
                "tears" -> "crying" !in set
                else -> true
            }
        }
        used.addAll(all)
        return joinNatural(selected.map { moodWord(it) })
    }

    private fun stylePhrase(ordered: List<String>, used: MutableSet<String>): List<String> {
        val styles = ordered.filter { it in STYLE }
        used.addAll(styles)
        return styles.map { styleWord(it) }
    }

    private fun styleSentence(styles: List<String>): String {
        val base = if (styles.isEmpty()) {
            "high-quality anime coloring, clean linework, and detailed shading"
        } else {
            joinNatural(styles)
        }
        return "The image uses $base, giving it a polished, high-quality finish."
    }

    private fun styleWord(tag: String): String = when (tag) {
        "twintails" -> "twin tails"
        "twin_braids" -> "twin braids"
        "single_braid" -> "a single braid"
        "braid" -> "braids"
        "ponytail" -> "a ponytail"
        "hair_bun" -> "a hair bun"
        "single_hair_bun" -> "a single hair bun"
        "drill_hair" -> "drill curls"
        "wavy_hair" -> "wavy hair"
        "ahoge" -> "an ahoge"
        "hair_over_one_eye" -> "hair over one eye"
        "hair_between_eyes" -> "hair between the eyes"
        "highres" -> "high resolution"
        "absurdres" -> "absurd resolution"
        "anime_coloring" -> "anime coloring"
        "monochrome" -> "monochrome"
        "greyscale" -> "greyscale"
        "watercolor" -> "watercolor"
        "sketch" -> "a sketch style"
        "lineart" -> "clean line art"
        "realistic" -> "realistic rendering"
        "photorealistic" -> "photorealistic rendering"
        "3d" -> "3D rendering"
        "pixel_art" -> "pixel art"
        "chibi" -> "chibi style"
        "official_art" -> "an official-art style"
        "cinematic_lighting" -> "cinematic lighting"
        "soft_lighting" -> "soft lighting"
        else -> natural(tag)
    }

    private fun eyeWord(tag: String): String = when (tag) {
        "heterochromia" -> "heterochromia"
        "eyelashes" -> "long eyelashes"
        "eyeshadow" -> "eyeshadow"
        "eyebrows" -> "expressive eyebrows"
        "closed_eyes" -> "closed eyes"
        "half-closed_eyes" -> "half-closed eyes"
        "wide_eyes" -> "wide eyes"
        else -> natural(tag)
    }

    private fun clothingWord(tag: String): String {
        val word = when (tag) {
            "japanese_clothes" -> "Japanese clothing"
            "one-piece_swimsuit" -> "a one-piece swimsuit"
            "school_swimsuit" -> "a school swimsuit"
            "sailor_collar" -> "a sailor collar"
            "hair_ribbon" -> "a hair ribbon"
            "hair_ornament" -> "a hair ornament"
            "thigh_strap" -> "a thigh strap"
            "shoulder_armor" -> "shoulder armor"
            else -> natural(tag)
        }
        return withArticle(tag, word)
    }

    private fun poseWord(tag: String, pronoun: String): String = when (tag) {
        "on_back" -> "lying on $pronoun back"
        "on_stomach" -> "lying on $pronoun stomach"
        "seiza" -> "sitting in seiza"
        "looking_at_viewer" -> "looking at the viewer"
        "looking_away" -> "looking away"
        "looking_back" -> "looking back"
        "from_side" -> "seen from the side"
        "from_behind" -> "seen from behind"
        "from_above" -> "seen from above"
        "from_below" -> "seen from below"
        "holding_weapon" -> "holding a weapon"
        "holding_sword" -> "holding a sword"
        "holding" -> "holding something"
        "arms_up" -> "with arms raised"
        "arms_behind_back" -> "with arms behind $pronoun back"
        "crossed_arms" -> "with crossed arms"
        "hand_on_hip" -> "with a hand on $pronoun hip"
        "hands_on_hips" -> "with hands on $pronoun hips"
        "v_arms" -> "making a V-sign with both arms"
        "arm_support" -> "with one arm supporting the body"
        "interlocked_fingers" -> "with interlocked fingers"
        "partially_unzipped" -> "with clothing partially unzipped"
        "head_tilt" -> "with a slight head tilt"
        "turning_head" -> "turning $pronoun head"
        "leaning_forward" -> "leaning forward"
        "leaning_back" -> "leaning back"
        else -> natural(tag)
    }

    private fun sceneWord(tag: String): String = when (tag) {
        "on_bed" -> "on a bed"
        "on_chair" -> "on a chair"
        "on_floor" -> "on the floor"
        "on_sofa" -> "on a sofa"
        "classroom" -> "in a classroom"
        "park" -> "in a park"
        "garden" -> "in a garden"
        "forest" -> "in a forest"
        "city" -> "in a city"
        "street" -> "on a street"
        "beach" -> "on a beach"
        "ocean" -> "by the ocean"
        "sky" -> "under the sky"
        "night" -> "at night"
        "day" -> "during the day"
        "sunset" -> "at sunset"
        "sunrise" -> "at sunrise"
        "rain" -> "in the rain"
        "snow" -> "in the snow"
        "starry_sky" -> "under a starry sky"
        "moon" -> "with the moon visible"
        "sun" -> "with the sun visible"
        "window" -> "near a window"
        "curtains" -> "with curtains"
        "bookshelf" -> "near a bookshelf"
        "desk" -> "at a desk"
        "table" -> "at a table"
        "bed" -> "in a bedroom"
        "chair" -> "on a chair"
        "couch" -> "on a couch"
        "mirror" -> "in front of a mirror"
        "bathroom" -> "in a bathroom"
        "kitchen" -> "in a kitchen"
        "pool" -> "at a pool"
        "onsen" -> "in an onsen"
        "shrine" -> "at a shrine"
        "temple" -> "at a temple"
        "church" -> "in a church"
        "castle" -> "in a castle"
        "ruins" -> "in ruins"
        "laboratory" -> "in a laboratory"
        "stage" -> "on a stage"
        "concert" -> "at a concert"
        "simple_background" -> "against a simple background"
        "white_background" -> "against a white background"
        "black_background" -> "against a black background"
        "transparent_background" -> "against a transparent background"
        "gradient_background" -> "against a gradient background"
        "blue_sky" -> "under a blue sky"
        "cloud" -> "with clouds"
        "clouds" -> "with clouds"
        "field" -> "in a field"
        "flower_field" -> "in a flower field"
        "mountain" -> "near mountains"
        "river" -> "by a river"
        "lake" -> "by a lake"
        "island" -> "on an island"
        "waterfall" -> "near a waterfall"
        "bridge" -> "on a bridge"
        "tower" -> "near a tower"
        "gate" -> "at a gate"
        "wall" -> "near a wall"
        "floor" -> "on the floor"
        "ceiling" -> "under a ceiling"
        "stairs" -> "on stairs"
        "roof" -> "on a roof"
        "lamp" -> "with a lamp"
        "clock" -> "with a clock"
        "television" -> "with a television"
        "computer" -> "with a computer"
        "phone" -> "with a phone"
        "smartphone" -> "with a smartphone"
        "umbrella" -> "with an umbrella"
        "swing" -> "on a swing"
        "broom" -> "with a broom"
        else -> natural(tag)
    }

    private fun sceneRank(tag: String): Int = when {
        tag == "indoors" || tag == "outdoors" -> 0
        tag.startsWith("on_") -> 1
        tag in setOf("night", "day", "sunset", "sunrise", "rain", "snow") -> 3
        else -> 2
    }

    private fun moodWord(tag: String): String = when (tag) {
        "light_smile" -> "a light smile"
        "smile" -> "a smile"
        "grin" -> "a grin"
        "laughing" -> "a laugh"
        "blush" -> "a blush"
        "crying" -> "tears"
        "tears" -> "tears"
        "open_mouth" -> "an open mouth"
        "closed_eyes" -> "closed eyes"
        "half-closed_eyes" -> "half-closed eyes"
        "naughty_face" -> "a playful expression"
        "expressionless" -> "an expressionless face"
        "smug" -> "a smug expression"
        "sad" -> "a sad expression"
        "angry" -> "an angry expression"
        "serious" -> "a serious expression"
        "surprised" -> "a surprised expression"
        "fear" -> "a fearful expression"
        "sweat" -> "a nervous sweat"
        "sweatdrop" -> "a sweatdrop"
        "nervous" -> "a nervous expression"
        "embarrassed" -> "an embarrassed expression"
        "shy" -> "a shy expression"
        "confident" -> "a confident expression"
        "gentle" -> "a gentle expression"
        "calm" -> "a calm expression"
        "playful" -> "a playful expression"
        "melancholic" -> "a melancholic expression"
        else -> natural(tag)
    }

    private fun withArticle(tag: String, word: String): String {
        if (word.startsWith("a ") || word.startsWith("an ")) return word
        if (!needsArticle(tag)) return word
        val first = word.firstOrNull()?.lowercaseChar()
        val article = if (first != null && first in VOWELS) "an" else "a"
        return "$article $word"
    }

    private fun needsArticle(tag: String): Boolean {
        if (tag in NO_ARTICLE) return false
        return NO_ARTICLE_SUFFIXES.none { tag.endsWith(it) }
    }

    private fun natural(tag: String): String =
        tag.replace('_', ' ').replace('-', ' ').trim()

    private fun joinNatural(items: List<String>): String {
        val clean = items.filter { it.isNotBlank() }.distinct()
        return when (clean.size) {
            0 -> ""
            1 -> clean[0]
            2 -> "${clean[0]} and ${clean[1]}"
            else -> clean.dropLast(1).joinToString(", ") + ", and " + clean.last()
        }
    }

    private fun wordCount(text: String): Int =
        text.split(Regex("\\s+")).count { it.isNotBlank() }
}
