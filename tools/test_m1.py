#!/usr/bin/env python3
"""Regression tests for the M1 dictionary matcher and database."""
from __future__ import annotations

import sqlite3
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from m1_common import boundary_ok, db_path, normalize
from m1_match import Matcher


class M1Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.matcher = Matcher()

    def tags(self, text: str) -> set[str]:
        return {m.tag for m in self.matcher.match(text)}

    def test_normalize(self) -> None:
        self.assertEqual(normalize("ＡＢＣ！"), "abc")
        self.assertEqual(normalize("ドラゴン"), "どらごん")
        self.assertEqual(normalize("長い 髪"), "長い髪")

    def test_single_kanji_boundary(self) -> None:
        text = normalize("水銀燈")
        self.assertFalse(boundary_ok(text, 0, 1, "水"))
        text = normalize("十六夜")
        self.assertFalse(boundary_ok(text, 2, 3, "夜"))
        text = normalize("日本")
        self.assertFalse(boundary_ok(text, 1, 2, "本"))

    def test_body_prefix_boundary(self) -> None:
        text = normalize("右胸、鎖骨")
        self.assertTrue(boundary_ok(text, 1, 2, "胸"))
        text = normalize("胸元")
        self.assertFalse(boundary_ok(text, 0, 1, "胸"))

    def test_kana_boundary(self) -> None:
        text = normalize("ペンドラゴン")
        self.assertFalse(boundary_ok(text, 2, 6, "ドラゴン"))
        text = normalize("ドラゴンが")
        self.assertTrue(boundary_ok(text, 0, 4, "ドラゴン"))
        text = normalize("パンティ")
        self.assertFalse(boundary_ok(text, 0, 2, "パン"))

    def test_canonical_aliases_are_english(self) -> None:
        con = sqlite3.connect(db_path())
        rows = con.execute(
            "SELECT a.lang FROM aliases a WHERE a.surface IN (':t', '\\\\m/')"
        ).fetchall()
        con.close()
        self.assertTrue(rows)
        self.assertTrue(all(lang == "en" for (lang,) in rows))

    def test_hair_and_clothing(self) -> None:
        tags = self.tags("長い黒髪の少女が青いドレスを着て、椅子に座っている")
        for tag in {
            "long_hair",
            "black_hair",
            "1girl",
            "blue_dress",
            "dress",
            "chair",
            "on_chair",
            "sitting",
            "solo",
        }:
            self.assertIn(tag, tags)

    def test_name_substrings_do_not_leak(self) -> None:
        self.assertNotIn("water", self.tags("水銀燈が微笑んでいる"))
        self.assertNotIn("night", self.tags("十六夜が微笑んでいる"))
        self.assertNotIn("book", self.tags("日本"))
        self.assertNotIn("dragon", self.tags("ペンドラゴン"))
        tags = self.tags("パンティを着ている")
        self.assertIn("panties", tags)
        self.assertNotIn("bread", tags)

    def test_body_part_prefix(self) -> None:
        self.assertIn("breasts", self.tags("右胸が見えている"))
        tags = self.tags("胸元が見えている")
        self.assertIn("cleavage", tags)
        self.assertIn("breasts", tags)

    def test_count_inference(self) -> None:
        tags = self.tags("二人の少女が並んでいる")
        self.assertIn("2girls", tags)
        self.assertIn("multiple_girls", tags)
        self.assertNotIn("solo", tags)

        tags = self.tags("一人の少女が立っている")
        self.assertIn("1girl", tags)
        self.assertIn("solo", tags)

    def test_parent_expansion(self) -> None:
        tags = self.tags("青いビキニを着た女性")
        self.assertIn("bikini", tags)
        self.assertIn("swimsuit", tags)

        tags = self.tags("着物を着た女性")
        self.assertIn("kimono", tags)
        self.assertIn("japanese_clothes", tags)

        tags = self.tags("ベレー帽をかぶった女性")
        self.assertIn("beret", tags)
        self.assertIn("hat", tags)

        tags = self.tags("軍服を着た女性")
        self.assertIn("military_uniform", tags)
        self.assertIn("military", tags)

        tags = self.tags("スナイパーライフルを持っている")
        for tag in {"sniper_rifle", "rifle", "gun", "weapon", "holding"}:
            self.assertIn(tag, tags)

    def test_tag_mapping_fixes(self) -> None:
        tags = self.tags("黒いマントを着た男性")
        self.assertIn("cape", tags)
        self.assertNotIn("cloak", tags)

        tags = self.tags("小さなマントを着た女性")
        self.assertIn("capelet", tags)

        tags = self.tags("双髪の少女")
        self.assertIn("twin_braids", tags)
        self.assertIn("braid", tags)

        tags = self.tags("和紙の傘を持っている")
        self.assertIn("oil-paper_umbrella", tags)
        self.assertIn("holding", tags)

        tags = self.tags("手を組んでいる")
        self.assertIn("interlocked_fingers", tags)

        tags = self.tags("舌を出している")
        self.assertIn("tongue_out", tags)

        tags = self.tags("後ろ向きに寝ている")
        self.assertIn("on_back", tags)
        self.assertIn("lying", tags)

        tags = self.tags("胸を露出している")
        self.assertIn("breasts_out", tags)
        self.assertIn("nipples", tags)

        tags = self.tags("レオタードがずらされている")
        self.assertIn("leotard_aside", tags)
        self.assertIn("clothing_aside", tags)

        tags = self.tags("腕で体を支えている")
        self.assertIn("arm_support", tags)

        tags = self.tags("検閲されている")
        self.assertIn("censored", tags)

        tags = self.tags("射精している男性")
        self.assertIn("ejaculation", tags)
        self.assertIn("cum", tags)
        self.assertIn("1boy", tags)

        tags = self.tags("男性器")
        self.assertIn("penis", tags)
        self.assertIn("1boy", tags)

    def test_machine_blocklist(self) -> None:
        tags = self.tags("学校の制服を着た少女")
        self.assertIn("school_uniform", tags)
        self.assertNotIn("school", tags)

        tags = self.tags("カメラ目線で微笑んでいる")
        self.assertIn("looking_at_viewer", tags)
        self.assertNotIn("camera", tags)

        self.assertNotIn("the_pose", self.tags("ポーズをとっている"))

    def test_nudity_inference(self) -> None:
        tags = self.tags("裸の女性")
        for tag in {"1girl", "nude", "breasts", "nipples"}:
            self.assertIn(tag, tags)

    def test_precision_regressions(self) -> None:
        self.assertIn("light_smile", self.tags("軽い笑みを浮かべている"))
        self.assertIn("brown_footwear", self.tags("茶色の靴を履いている"))
        self.assertIn("black_thighhighs", self.tags("黒のハイソックスを履いている"))
        self.assertIn("white_thighhighs", self.tags("白いニーハイを履いている"))
        self.assertIn("thighhighs", self.tags("膝上丈のストッキングを履いている"))
        self.assertIn("witch_hat", self.tags("魔女帽をかぶっている"))
        self.assertIn("mob_cap", self.tags("モーブ帽をかぶっている"))
        self.assertIn("shoulder_armor", self.tags("肩当てをつけている"))
        self.assertIn("field", self.tags("野原にいる"))
        self.assertIn("grin", self.tags("大きく笑っている"))
        self.assertIn("bowl", self.tags("ボウルを持っている"))
        self.assertIn("pants", self.tags("ズボンを履いている"))
        self.assertIn("drill_hair", self.tags("ドリルの髪飾りをつけている"))
        self.assertIn("lolita_hairband", self.tags("ロリータ風のヘアバンドをしている"))
        self.assertIn("thigh_boots", self.tags("ハイブーツを履いている"))
        self.assertIn("bare_arms", self.tags("裸の腕"))

    def test_curated_generic_aliases(self) -> None:
        for text, tag in [
            ("水", "water"),
            ("木", "tree"),
            ("花", "flower"),
            ("ドラゴン", "dragon"),
            ("カニ", "crab"),
            ("夜", "night"),
            ("森", "forest"),
            ("雨", "rain"),
            ("雪", "snow"),
            ("空", "sky"),
        ]:
            self.assertIn(tag, self.tags(text), text)


if __name__ == "__main__":
    unittest.main()
