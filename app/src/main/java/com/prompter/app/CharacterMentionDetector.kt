package com.prompter.app

import android.content.Context
import java.text.Normalizer
import java.util.Locale

/**
 * Finds character names mentioned in a scene and returns them as candidates.
 *
 * This is deliberately conservative: one-character aliases and readings never
 * participate, two-character aliases require the work to be named in the same
 * text, and Latin names require word boundaries. The caller must still ask the
 * user to tap a candidate; detection never confirms an identity by itself.
 */
object CharacterMentionDetector {
    private const val MAX_RESULTS = 8
    private val SEPARATORS = "・·'’-!:：".toSet()

    private data class Hit(
        val alias: CharacterDb.DetectionAlias,
        val surface: String,
        val canonical: Boolean,
    )

    private data class CompactText(
        val source: String,
        val value: String,
        val positions: IntArray,
    )

    fun detect(context: Context, text: String): List<CharacterEntry> {
        val trimmed = text.trim()
        if (trimmed.length < 2) return emptyList()
        val aliases = CharacterDb.detectionAliases(context.applicationContext)
        if (aliases.isEmpty()) return emptyList()

        val works = CharacterNames.worksIn(trimmed)
        val latinText = latinForm(trimmed)
        val japaneseText = compactWithPositions(trimmed)
        val hits = buildList {
            aliases.forEach { alias ->
                val surface = when (alias.lang) {
                    "en" -> findLatin(latinText, alias.text)
                    else -> findJapanese(
                        japaneseText,
                        alias.text,
                        requireNameBoundary = alias.detectTier == 1,
                    )
                } ?: return@forEach
                // A two-character Japanese name is too ambiguous without its work.
                if (alias.detectTier == 1 && alias.series !in works) return@forEach
                if (works.isNotEmpty() && alias.series !in works) return@forEach
                add(Hit(alias, surface, alias.kind == "canonical"))
            }
        }
        if (hits.isEmpty()) return emptyList()

        // A variant often carries the base name as a short alias. When the base
        // canonical name is present, do not offer the variant for that surface.
        val canonicalSurfaces = hits.filter { it.canonical }.map { it.surface }.toSet()
        val preferred = hits.filter { it.canonical || it.surface !in canonicalSurfaces }

        // Prefer the most specific surface: 丹恒・飲月 over 丹恒, 姫子・旅立ち over 姫子.
        val longest = preferred.filter { hit ->
            preferred.none { other -> other.surface != hit.surface && hit.surface in other.surface }
        }
        val best = linkedMapOf<Long, Hit>()
        longest.forEach { hit ->
            val previous = best[hit.alias.characterId]
            if (previous == null ||
                hit.surface.length > previous.surface.length ||
                (hit.surface.length == previous.surface.length && hit.canonical && !previous.canonical)
            ) {
                best[hit.alias.characterId] = hit
            }
        }
        return best.values
            .sortedWith(
                compareByDescending<Hit> { it.surface.length }
                    .thenByDescending { it.canonical }
                    .thenBy { it.alias.category == "variant" },
            )
            .take(MAX_RESULTS)
            .map { hit ->
                CharacterEntry(
                    id = hit.alias.characterId,
                    nameJa = hit.alias.nameJa,
                    nameEn = hit.alias.nameEn,
                    series = hit.alias.series,
                    category = hit.alias.category,
                    note = hit.alias.note,
                )
            }
    }

    /** Lowercase and collapse whitespace so English aliases match spaced text. */
    private fun latinForm(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[\\s\\-_]+"), " ")
        .trim()

    /**
     * Remove spacing and punctuation but keep the original script. In
     * particular, do not convert katakana to hiragana: ヴェスナ + が must not
     * become a match for ナガ.
     */
    private fun compactForm(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .filterNot { it.isWhitespace() || it in SEPARATORS }

    private fun compactWithPositions(text: String): CompactText {
        val source = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val value = StringBuilder(source.length)
        val positions = ArrayList<Int>(source.length)
        source.forEachIndexed { index, character ->
            if (!character.isWhitespace() && character !in SEPARATORS) {
                value.append(character)
                positions.add(index)
            }
        }
        return CompactText(source, value.toString(), positions.toIntArray())
    }

    private fun findJapanese(
        haystack: CompactText,
        alias: String,
        requireNameBoundary: Boolean,
    ): String? {
        val needle = compactForm(alias)
        if (needle.length < 2) return null
        var start = haystack.value.indexOf(needle)
        while (start >= 0) {
            if (!requireNameBoundary || hasNameBoundary(haystack, start, needle.length)) {
                return needle
            }
            start = haystack.value.indexOf(needle, start + 1)
        }
        return null
    }

    /**
     * Reject a two-character alias that is part of a longer compound, such as
     * 花火 in 花火大会. Particles and punctuation still count as boundaries.
     * Positions point back into the un-compacted text so separators such as
     * ・, ： and spaces remain real boundaries.
     */
    private fun hasNameBoundary(haystack: CompactText, start: Int, length: Int): Boolean {
        val first = haystack.positions.getOrNull(start) ?: return true
        val last = haystack.positions.getOrNull(start + length - 1) ?: return true
        val before = haystack.source.getOrNull(first - 1)
        val after = haystack.source.getOrNull(last + 1)
        return before?.isKanjiOrKatakana() != true && after?.isKanjiOrKatakana() != true
    }

    private fun findLatin(haystack: String, alias: String): String? {
        val needle = latinForm(alias)
        if (needle.length < 2) return null
        var start = haystack.indexOf(needle)
        while (start >= 0) {
            val before = haystack.getOrNull(start - 1)
            val after = haystack.getOrNull(start + needle.length)
            if (before?.isAsciiLetter() != true && after?.isAsciiLetter() != true) {
                return needle
            }
            start = haystack.indexOf(needle, start + 1)
        }
        return null
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isKanjiOrKatakana(): Boolean =
        this in '\u4e00'..'\u9fff' || this in '\u3400'..'\u4dbf' || this in '\u30a0'..'\u30ff'
}
