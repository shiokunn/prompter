package com.prompter.app

import java.text.Normalizer
import java.util.Locale

/** Shared by local and remote lookup. Normalization never joins records. */
object CharacterNames {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .filterNot { it.isWhitespace() || it in "・·'’-!:：" }
        .map { if (it in 'ァ'..'ヶ') (it.code - 0x60).toChar() else it }
        .joinToString("")

    private data class Work(val name: String, val aliases: List<String>)

    private val works = listOf(
        Work(
            "干物妹!うまるちゃん",
            listOf("干物妹!うまるちゃん", "うまるちゃん", "Himouto! Umaru-chan"),
        ),
        Work(
            "崩壊：スターレイル",
            listOf("崩壊：スターレイル", "スターレイル", "スタレ", "Honkai Star Rail"),
        ),
        Work("原神", listOf("原神", "Genshin Impact", "Genshin")),
        Work(
            "ゼンレスゾーンゼロ",
            listOf("ゼンレスゾーンゼロ", "ゼンゼロ", "Zenless Zone Zero", "ZZZ"),
        ),
        Work("鳴潮", listOf("鳴潮", "めいちょう", "Wuthering Waves")),
        Work(
            "勝利の女神：NIKKE",
            listOf("勝利の女神：NIKKE", "NIKKE", "ニケ", "Goddess of Victory NIKKE"),
        ),
        Work(
            "学園アイドルマスター",
            listOf("学園アイドルマスター", "学マス", "Gakuen Idolmaster", "Gakumas"),
        ),
        Work(
            "マブラヴ ガールズガーデン",
            listOf("マブラヴ ガールズガーデン", "マブガル", "Muv-Luv Girls Garden"),
        ),
        Work(
            "Re:ゼロから始める異世界生活",
            listOf(
                "Re:ゼロから始める異世界生活",
                "リゼロ",
                "Re:Zero",
                "Re:Zero Starting Life in Another World",
            ),
        ),
        Work(
            "この素晴らしい世界に祝福を！",
            listOf("この素晴らしい世界に祝福を！", "このすば", "Konosuba"),
        ),
    ).map { work -> Work(work.name, work.aliases.map { alias -> normalize(alias) }) }

    /** Canonical work names mentioned in the text. Used only for scene detection. */
    fun worksIn(text: String): Set<String> {
        val query = normalize(text)
        if (query.isEmpty()) return emptySet()
        return works
            .filter { work -> work.aliases.any { it.isNotEmpty() && query.contains(it) } }
            .map { it.name }
            .toSet()
    }

    fun workMatches(query: String, titles: List<String>): Boolean {
        val q = normalize(query)
        if (q.length < 2) return true
        val equivalents = works.firstOrNull { work ->
            work.aliases.any { alias -> alias == q || alias.contains(q) || q.contains(alias) }
        }?.aliases ?: listOf(q)
        return titles.any { title ->
            val normalized = normalize(title)
            equivalents.any { alias -> normalized.contains(alias) }
        }
    }
}
