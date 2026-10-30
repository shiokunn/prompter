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

    private val works = listOf(
        listOf("干物妹!うまるちゃん", "うまるちゃん", "Himouto! Umaru-chan"),
        listOf("崩壊：スターレイル", "スターレイル", "スタレ", "Honkai Star Rail"),
        listOf("原神", "Genshin Impact", "Genshin"),
        listOf("ゼンレスゾーンゼロ", "ゼンゼロ", "Zenless Zone Zero", "ZZZ"),
        listOf("鳴潮", "めいちょう", "Wuthering Waves"),
        listOf("勝利の女神：NIKKE", "NIKKE", "ニケ", "Goddess of Victory NIKKE"),
        listOf("学園アイドルマスター", "学マス", "Gakuen Idolmaster", "Gakumas"),
        listOf("マブラヴ ガールズガーデン", "マブガル", "Muv-Luv Girls Garden"),
        listOf("Re:ゼロから始める異世界生活", "リゼロ", "Re:Zero", "Re:Zero Starting Life in Another World"),
        listOf("この素晴らしい世界に祝福を！", "このすば", "Konosuba"),
    ).map { names -> names.map(::normalize) }

    fun workMatches(query: String, titles: List<String>): Boolean {
        val q = normalize(query)
        if (q.isEmpty()) return true
        val equivalents = works.firstOrNull { aliases -> aliases.any { alias -> alias == q || alias.contains(q) || q.contains(alias) } }
            ?: listOf(q)
        return titles.any { title ->
            val normalized = normalize(title)
            equivalents.any { alias -> normalized.contains(alias) }
        }
    }
}
