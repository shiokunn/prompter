#!/usr/bin/env python3
"""Shared helpers for the M1 dictionary pipeline.

The app will eventually reimplement the hot path in Kotlin. These Python
tools are the build-time reference implementation and the evaluation harness.
"""
from __future__ import annotations

import csv
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "research" / "data"
ALIASES = ROOT / "research" / "aliases"
ASSETS = ROOT / "app" / "src" / "main" / "assets"

TOP_N = 10000
TERM = "\0"

# Priority is used when the same surface form maps to multiple tags.
SOURCE_PRIORITY = {
    "curated": 80,
    "manual": 70,
    "canonical": 60,
    "official": 50,
    "generated": 40,
    "machine": 20,
}
SOURCE_TIER = {
    "curated": "A",
    "manual": "A",
    "canonical": "A",
    "official": "B",
    "generated": "B",
    "machine": "C",
}
SOURCE_CONFIDENCE = {
    "curated": 1.0,
    "manual": 0.95,
    "canonical": 1.0,
    "official": 0.9,
    "generated": 0.75,
    "machine": 0.45,
}

ASCII_ONLY = re.compile(r"^[A-Za-z0-9 ._\-()/]+$")
KANA_ONLY = re.compile(r"^[\u3041-\u309f\u30a1-\u30f6\u30fc]+$")
KANA_CHAR = re.compile(r"[\u3041-\u309f\u30a1-\u30f6\u30fc]")
KANJI_CHAR = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")
# Single-kanji body-part aliases are valid after left/right/both modifiers,
# e.g. 右胸, 両腕, 片手.  They are still rejected inside names/compounds such
# as 水銀燈 or 胸元 (the latter gets the dedicated 胸元 -> cleavage alias).
BODY_PREFIX_OK: dict[str, set[str]] = {
    "胸": set("右左両片上下大中小"),
    "目": set("右左両片"),
    "耳": set("右左両片"),
    "腕": set("右左両片"),
    "脚": set("右左両片"),
    "足": set("右左両片"),
    "手": set("右左両片"),
    "指": set("右左両片"),
    "尻": set("右左"),
    "腹": set("下"),
    "首": set("右左"),
    "歯": set("上下"),
    "唇": set("上下"),
    "顔": set("右左"),
    "髪": set("右左"),
}
# A kana word may be followed/preceded by these particles/auxiliaries. If a
# kana alias touches another kana that is not in this set, it is probably part
# of a longer word (e.g. ドラゴン inside ペンドラゴン, パン inside パンティ).
KANA_BOUNDARY_OK = set(
    "はがをにでとのへもやかならばだ"
    "たていくるすずれりきけしせめろわを"
    "からまでよりなどだけしかこそってながらそうない"
)
# Short kana words (パン, カニ, ソロ) need a stricter boundary: て/ば/い etc.
# are too often the start of a longer word (パンティ, カニバル).
KANA_SHORT_BOUNDARY_OK = set("はがをにでとのへもやかなら")
BAD_JP = re.compile(
    r"(です|ます|しています|見ています|ような|ための|による|された|ている|"
    r"でした|ください|こと|もの|ところ|リクエスト|コメント|画像|投稿|タグ|作品)"
)
BAD_ALIAS_CHARS = re.compile(r"[（）()「」『』【】\[\]<>＜＞\"'“”‘’]")
PUNCT = re.compile(r"[\s_\-・,、。.!?！？「」『』()（）\[\]【】/\\:;…~〜]+")


def normalize(s: str) -> str:
    """NFKC, lower-case, katakana -> hiragana, remove punctuation/space."""
    s = unicodedata.normalize("NFKC", str(s)).lower()
    out = []
    for ch in s:
        o = ord(ch)
        out.append(chr(o - 0x60) if 0x30A1 <= o <= 0x30F6 else ch)
    return PUNCT.sub("", "".join(out))


def is_ascii(s: str) -> bool:
    return bool(ASCII_ONLY.match(s))


def is_kana_only(s: str) -> bool:
    return bool(KANA_ONLY.match(s))


def boundary_ok(text: str, start: int, end: int, surface: str) -> bool:
    """Reject aliases that are substrings of a longer word.

    Japanese text has no spaces, so the trie can find パン inside パンティ or
    ドラゴン inside ペンドラゴン. Kana aliases therefore need a particle-aware
    boundary check. Single-kanji aliases are rejected when they touch another
    kanji (夜 inside 十六夜, 水 inside 水銀燈).
    """
    n = normalize(surface)
    before = text[start - 1] if start > 0 else ""
    after = text[end] if end < len(text) else ""
    if is_kana_only(n):
        if len(n) <= 1:
            return not (KANA_CHAR.match(before or " ") or KANA_CHAR.match(after or " "))
        allowed = KANA_SHORT_BOUNDARY_OK if len(n) == 2 else KANA_BOUNDARY_OK
        if KANA_CHAR.match(before or " ") and before not in allowed:
            return False
        if KANA_CHAR.match(after or " ") and after not in allowed:
            return False
        return True
    if len(n) == 1 and KANJI_CHAR.match(n):
        if before in BODY_PREFIX_OK.get(n, set()):
            return True
        return not (KANJI_CHAR.match(before or " ") or KANJI_CHAR.match(after or " "))
    return True


@dataclass(frozen=True)
class Tag:
    name: str
    category: int
    post_count: int
    aliases: tuple[str, ...] = ()


@dataclass(frozen=True)
class Alias:
    tag: str
    surface: str
    norm: str
    source: str
    lang: str
    confidence: float
    tier: str
    validation: str
    post_count: int = 0

    @property
    def priority(self) -> int:
        return SOURCE_PRIORITY.get(self.source, 0)


def load_tags(path: Path | None = None) -> dict[str, Tag]:
    path = path or DATA / "danbooru.csv"
    tags: dict[str, Tag] = {}
    with path.open(encoding="utf-8") as f:
        for row in csv.reader(f):
            if len(row) < 3:
                continue
            name = row[0]
            aliases = tuple(
                a for a in (row[3].split(",") if len(row) > 3 and row[3] else []) if a
            )
            tags[name] = Tag(name, int(row[1]), int(row[2]), aliases)
    return tags


def load_translations() -> tuple[dict[str, str], dict[str, str]]:
    manual: dict[str, str] = {}
    machine: dict[str, str] = {}
    for filename, dest in (
        ("danbooru-jp.csv", manual),
        ("danbooru-only-machine-jp.csv", machine),
    ):
        path = DATA / filename
        if not path.exists():
            continue
        with path.open(encoding="utf-8") as f:
            for row in csv.reader(f):
                if len(row) >= 2 and row[0] and row[1].strip():
                    dest[row[0]] = row[1].strip()
    return manual, machine


def top_tags(tags: dict[str, Tag], n: int = TOP_N) -> list[Tag]:
    return sorted(tags.values(), key=lambda t: (-t.post_count, t.name))[:n]


def load_lexicon(path: Path | None = None) -> list[tuple[str, str, str, float]]:
    path = path or ALIASES / "ja_lexicon.tsv"
    out: list[tuple[str, str, str, float]] = []
    if not path.exists():
        return out
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            cols = line.split("\t")
            if len(cols) < 2:
                continue
            surface, tag = cols[0].strip(), cols[1].strip()
            source = cols[2].strip() if len(cols) > 2 and cols[2].strip() else "curated"
            try:
                confidence = float(cols[3]) if len(cols) > 3 else SOURCE_CONFIDENCE[source]
            except ValueError:
                confidence = SOURCE_CONFIDENCE.get(source, 0.5)
            if surface and tag:
                out.append((surface, tag, source, confidence))
    return out


def canonical_map(tags: dict[str, Tag]) -> dict[str, str]:
    """Map tag names, space variants, and official aliases to canonical names."""
    out: dict[str, str] = {}
    for name, tag in tags.items():
        out[name] = name
        out[name.replace("_", " ")] = name
        for alias in tag.aliases:
            out.setdefault(alias, name)
            out.setdefault(alias.replace("_", " "), name)
    return out


def canonicalize(value: str, mapping: dict[str, str]) -> str | None:
    value = value.strip()
    if value in mapping:
        return mapping[value]
    return mapping.get(value.replace(" ", "_"))


def alias_is_usable_jp(surface: str) -> bool:
    n = normalize(surface)
    if len(n) < 2:
        return False
    if is_ascii(surface):
        return False
    if BAD_JP.search(surface):
        return False
    if BAD_ALIAS_CHARS.search(surface):
        return False
    return True


def db_path() -> Path:
    return ASSETS / "promptdb.sqlite"
