#!/usr/bin/env python3
"""Feasibility spike: JP surface form -> Danbooru tag longest-match matcher.

Data: a1111-sd-webui-tagcomplete danbooru.csv (MIT) +
booru-japanese-tag manual/machine JP CSVs (MIT).
This is a throwaway prototype to estimate coverage/latency, not the app implementation.

Usage: python3 tools/spike_match.py
"""
import csv
import re
import time
import unicodedata
from dataclasses import dataclass
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / "research" / "data"
MACHINE_MIN_COUNT = 5000
MACHINE_MIN_LEN = 2
TERM = "\0"

BAD_JP = re.compile(r"(です|ます|しています|見ています|ような|ための|による|された|ている|でした|ください|こと|もの|ところ)")
ASCII_ONLY = re.compile(r"^[A-Za-z0-9 ._\-()]+$")
KATAKANA = re.compile(r"^[\u30a1-\u30f6\u30fc]+$")

# Hand-curated aliases that a build-time LLM pass would generate for the top tags.
# This is deliberately separate from the public CSVs to show the effect of aliases.
CURATED = {
    "銀髪": "silver_hair",
    "銀の髪": "silver_hair",
    "青い目": "blue_eyes",
    "桜": "cherry_blossoms",
    "夕焼け": "sunset",
    "カメラ目線": "looking_at_viewer",
    "手を振っている": "waving",
    "手を振る": "waving",
    "水彩": "watercolor_(medium)",
    "水彩風": "watercolor_(medium)",
    "淡い色合い": "pastel_colors",
    "サイバーパンク": "cyberpunk",
    "ネオン": "neon_lights",
    "雨": "rain",
    "夜": "night",
    "草原": "grass",
    "空を見上げている": "looking_up",
    "見上げている": "looking_up",
    "座って": "sitting",
    "白いワンピース": "white_dress",
    "夏": "summer",
    "海辺": "beach",
    "紅茶": "tea",
    "注いでいる": "pouring",
    "俯瞰": "from_above",
    "俯瞰視点": "from_above",
    "ダイナミックなポーズ": "dynamic_pose",
    "春": "spring_(season)",
    "公園": "park",
    "教室": "classroom",
    "制服": "school_uniform",
    "セーラー服": "serafuku",
    "猫耳": "cat_ears",
    "メイド": "maid",
    "笑顔": "smile",
    "逆光": "backlighting",
    "浴衣": "yukata",
    "黒髪": "black_hair",
    "八重歯": "fang",
    "ツインテール": "twintails",
    "ロングヘア": "long_hair",
    "女の子": "1girl",
    "少女": "1girl",
    "立ち": "standing",
    "立っている": "standing",
    "ポーズ": "pose",
    "構図": "composition",
}


def norm(s: str) -> str:
    s = unicodedata.normalize("NFKC", s).lower()
    out = []
    for ch in s:
        o = ord(ch)
        if 0x30A1 <= o <= 0x30F6:
            out.append(chr(o - 0x60))
        else:
            out.append(ch)
    s = "".join(out)
    s = re.sub(r"[\s_\-・,、。.!?！？「」『』()（）\[\]【】/\\:;]+", "", s)
    return s


@dataclass
class Entry:
    tag: str
    surface: str
    count: int
    source: str


def load():
    tags = {}
    with open(DATA / "danbooru.csv", encoding="utf-8") as f:
        for row in csv.reader(f):
            if len(row) < 3:
                continue
            name, count = row[0], int(row[2])
            aliases = row[3].split(",") if len(row) > 3 and row[3] else []
            tags[name] = {"count": count, "aliases": aliases, "jp": None, "src": None}
    for fn, src in [("danbooru-only-machine-jp.csv", "machine"), ("danbooru-jp.csv", "manual")]:
        with open(DATA / fn, encoding="utf-8") as f:
            for row in csv.reader(f):
                if len(row) < 2:
                    continue
                name, jp = row[0], row[1].strip()
                if name not in tags or not jp:
                    continue
                if src == "manual" or tags[name]["jp"] is None:
                    tags[name]["jp"] = jp
                    tags[name]["src"] = src
    entries = []
    for name, v in tags.items():
        count = v["count"]
        forms = []
        if v["jp"]:
            jp = v["jp"]
            if v["src"] == "manual":
                if len(norm(jp)) >= 1:
                    forms.append((jp, "manual"))
            elif (
                count >= MACHINE_MIN_COUNT
                and len(norm(jp)) >= MACHINE_MIN_LEN
                and not BAD_JP.search(jp)
                and not ASCII_ONLY.match(jp)
            ):
                forms.append((jp, "machine"))
        for form in [name, name.replace("_", " ")] + v["aliases"]:
            if len(norm(form)) >= 3:
                forms.append((form, "english"))
        for surface, source in forms:
            n = norm(surface)
            if len(n) < 1:
                continue
            entries.append(Entry(name, surface, count, source))
    # curated aliases (highest priority)
    for surface, tag in CURATED.items():
        count = tags.get(tag, {}).get("count", 0)
        entries.append(Entry(tag, surface, count, "curated"))
    # dedupe identical normalized surface, keep highest rank
    best = {}
    rank_source = {"curated": 3, "manual": 2, "machine": 1, "english": 1}
    for e in entries:
        n = norm(e.surface)
        rank = (rank_source[e.source], e.count)
        if n not in best:
            best[n] = e
            continue
        old = best[n]
        old_rank = (rank_source[old.source], old.count)
        if rank > old_rank:
            best[n] = e
    return list(best.values())


def build_trie(entries):
    root = {}
    for e in entries:
        node = root
        for ch in norm(e.surface):
            node = node.setdefault(ch, {})
        node.setdefault(TERM, []).append(e)
    return root


def boundary_ok(s: str, start: int, end: int, surface: str) -> bool:
    """Cheap guard against substring false positives (e.g. パン inside サイバーパンク)."""
    n = norm(surface)
    if not KATAKANA.match(n):
        return True
    if len(n) >= 4:
        return True
    before = s[start - 1] if start > 0 else ""
    after = s[end] if end < len(s) else ""
    return not (KATAKANA.match(before or " ") and KATAKANA.match(after or " "))


def match(text, root):
    s = norm(text)
    matches = []
    i = 0
    while i < len(s):
        node = root
        j = i
        best = None
        while j < len(s) and s[j] in node:
            node = node[s[j]]
            j += 1
            if TERM in node:
                if best is None or j - i > best[0]:
                    best = (j - i, i, j, node[TERM])
        if best:
            _, start, end, cands = best
            if boundary_ok(s, start, end, cands[0].surface):
                cands = sorted(
                    cands,
                    key=lambda e: ({"curated": 3, "manual": 2}.get(e.source, 1), e.count),
                    reverse=True,
                )
                matches.append((start, end, cands))
                i = end
                continue
        i += 1
    return matches


def main():
    t0 = time.time()
    entries = load()
    root = build_trie(entries)
    load_ms = (time.time() - t0) * 1000
    prompts = [
        "銀髪のツインテールの少女が、制服姿で教室に立っている",
        "猫耳のメイドが紅茶を注いでいる",
        "夏の海辺で浴衣を着た女の子、夕焼け、逆光",
        "俯瞰視点で見下ろす構図、ダイナミックなポーズ",
        "水彩風の柔らかいタッチ、淡い色合い",
        "黒髪ロング、青い目、八重歯、セーラー服",
        "手を振っている、笑顔、カメラ目線",
        "背景に桜が舞う、春の公園",
        "白いワンピースを着た金髪の少女が、草原に座って空を見上げている",
        "サイバーパンク風の街並み、ネオン、雨、夜",
    ]
    print(f"entries={len(entries)} load={load_ms:.0f}ms")
    for p in prompts:
        m = match(p, root)
        print("\nJA:", p)
        seen = []
        for _, _, cands in m:
            seen.append(f"{cands[0].tag}({cands[0].surface})")
        print("MATCH:", ", ".join(seen))
        s = norm(p)
        rest, pos = [], 0
        for a, b, _ in m:
            if a > pos:
                rest.append(s[pos:a])
            pos = max(pos, b)
        if pos < len(s):
            rest.append(s[pos:])
        print("UNMATCHED:", " / ".join(x for x in rest if x))


if __name__ == "__main__":
    main()
