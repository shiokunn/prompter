#!/usr/bin/env python3
"""Build the M1 top-5,000 Danbooru alias database.

Sources:
  - danbooru.csv (tag names, categories, post counts, official aliases)
  - danbooru-jp.csv (manual Japanese translations)
  - danbooru-only-machine-jp.csv (machine Japanese translations)
  - research/aliases/ja_lexicon.tsv (hand-curated aliases)
  - generated colour + noun aliases (e.g. 赤髪 -> red_hair)

Output:
  app/src/main/assets/promptdb.sqlite

The SQLite file is the app asset. The matcher loads the `aliases` table into
a trie; the DB also gives us a place to store validation metadata later.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sqlite3
import time
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

from m1_common import (
    ALIASES,
    DATA,
    SOURCE_CONFIDENCE,
    SOURCE_PRIORITY,
    SOURCE_TIER,
    Alias,
    alias_is_usable_jp,
    db_path,
    is_ascii,
    load_lexicon,
    load_tags,
    load_translations,
    normalize,
    top_tags,
)

# A few scene tags sit just outside the top 5,000 but are common in prompts.
# Keep the core at 5,000 and add these explicitly.
SUPPLEMENT_TAGS = {"park", "morning", "spring_(season)"}

# Generic setting/object tags are frequent false positives when they occur
# inside character names (ペンドラゴン -> dragon, 水銀燈 -> water). Explicit
# curated aliases below still cover the useful cases.
MACHINE_BLOCKLIST = {
    "school",
    "the_pose",
    "camera",
    "water",
    "tree",
    "book",
    "flower",
    "dragon",
    "crab",
    "star_(symbol)",
    "cloud",
    "night",
    "forest",
    "rain",
    "snow",
    "sky",
    "bare_arms",
    # 半分 is too ambiguous for half_updo (e.g. 縦半分, 半分閉じた目).
    "half_updo",
}

# Colour base -> canonical prefix. Hair/eyes have special mappings below.
COLORS: tuple[tuple[str, str], ...] = (
    ("赤", "red"),
    ("赤い", "red"),
    ("赤色", "red"),
    ("青", "blue"),
    ("青い", "blue"),
    ("青色", "blue"),
    ("蒼", "blue"),
    ("白", "white"),
    ("白い", "white"),
    ("白色", "white"),
    ("黒", "black"),
    ("黒い", "black"),
    ("黒色", "black"),
    ("茶", "brown"),
    ("茶色", "brown"),
    ("茶色い", "brown"),
    ("緑", "green"),
    ("緑色", "green"),
    ("紫", "purple"),
    ("紫色", "purple"),
    ("ピンク", "pink"),
    ("桃色", "pink"),
    ("オレンジ", "orange"),
    ("橙色", "orange"),
    ("黄色", "yellow"),
    ("黄", "yellow"),
    ("灰色", "grey"),
    ("グレー", "grey"),
    ("銀", "silver"),
    ("銀色", "silver"),
    ("金", "gold"),
    ("金色", "gold"),
    ("水色", "aqua"),
)

NOUNS: tuple[tuple[str, str], ...] = (
    ("髪", "hair"),
    ("目", "eyes"),
    ("瞳", "eyes"),
    ("シャツ", "shirt"),
    ("ドレス", "dress"),
    ("スカート", "skirt"),
    ("リボン", "ribbon"),
    ("セーラーカラー", "sailor_collar"),
    ("背景", "background"),
    ("靴下", "socks"),
    ("ソックス", "socks"),
    ("ニーソ", "thighhighs"),
    ("ブラウス", "blouse"),
    ("キャミソール", "camisole"),
    ("レオタード", "leotard"),
    ("ボディスーツ", "bodysuit"),
    ("ジャケット", "jacket"),
    ("手袋", "gloves"),
    ("帽子", "hat"),
    ("翼", "wings"),
    ("パンティ", "panties"),
    ("ブラ", "bra"),
    ("ネクタイ", "necktie"),
    ("ベスト", "vest"),
    ("コート", "coat"),
    ("水着", "swimsuit"),
    ("浴衣", "yukata"),
    ("着物", "kimono"),
    ("エプロン", "apron"),
    ("ニーソ", "thighhighs"),
    ("ニーハイ", "thighhighs"),
    ("ハイソックス", "thighhighs"),
    ("ストッキング", "pantyhose"),
    ("靴", "footwear"),
    ("ブーツ", "boots"),
    ("ハイヒール", "high_heels"),
    ("ローファー", "loafers"),
    ("スニーカー", "sneakers"),
    ("サンダル", "sandals"),
    ("ズボン", "pants"),
    ("ビキニ", "bikini"),
    ("マフラー", "scarf"),
    ("ネックレス", "necklace"),
    ("イヤリング", "earrings"),
    ("蝶ネクタイ", "bowtie"),
)

HAIR_COLOR = {
    "red": "red",
    "blue": "blue",
    "white": "white",
    "black": "black",
    "brown": "brown",
    "green": "green",
    "purple": "purple",
    "pink": "pink",
    "orange": "orange",
    "yellow": "blonde",
    "grey": "grey",
    "silver": "silver",
    "gold": "blonde",
    "aqua": "aqua",
}
EYE_COLOR = {
    "red": "red",
    "blue": "blue",
    "white": "white",
    "black": "black",
    "brown": "brown",
    "green": "green",
    "purple": "purple",
    "pink": "pink",
    "orange": "orange",
    "yellow": "yellow",
    "grey": "grey",
    "silver": "grey",
    "gold": "yellow",
    "aqua": "aqua",
}


def generated_colour_aliases(top_names: set[str]) -> list[tuple[str, str, str, float]]:
    out: list[tuple[str, str, str, float]] = []
    seen: set[tuple[str, str]] = set()
    for colour_surface, colour in COLORS:
        for noun_surface, noun in NOUNS:
            if noun == "hair":
                prefix = HAIR_COLOR.get(colour, colour)
            elif noun == "eyes":
                prefix = EYE_COLOR.get(colour, colour)
            else:
                prefix = colour
            tag = f"{prefix}_{noun}"
            if tag not in top_names:
                continue
            # 赤髪, 赤い髪, 赤の髪, 赤色の髪, ピンクの髪, ...
            surfaces = {
                f"{colour_surface}{noun_surface}",
                f"{colour_surface}の{noun_surface}",
            }
            if colour_surface.endswith("い"):
                base = colour_surface[:-1]
                surfaces.add(f"{base}{noun_surface}")
                surfaces.add(f"{base}の{noun_surface}")
            if not colour_surface.endswith("色"):
                surfaces.add(f"{colour_surface}色の{noun_surface}")
            for surface in surfaces:
                if (surface, tag) in seen:
                    continue
                seen.add((surface, tag))
                out.append((surface, tag, "generated", 0.75))
    return out


def add_alias(
    out: list[Alias],
    tag: str,
    surface: str,
    source: str,
    confidence: float | None = None,
    post_count: int = 0,
) -> None:
    n = normalize(surface)
    if not n:
        return
    # Canonical/official Danbooru tags are English, even when they contain
    # punctuation or emoticons (e.g. :t, \m/). Keep them out of the Japanese
    # trie; they are still stored for tag-mode/English lookup.
    lang = "en" if source in {"canonical", "official"} or is_ascii(surface) else "ja"
    out.append(
        Alias(
            tag=tag,
            surface=surface,
            norm=n,
            source=source,
            lang=lang,
            confidence=SOURCE_CONFIDENCE.get(source, 0.5)
            if confidence is None
            else confidence,
            tier=SOURCE_TIER.get(source, "C"),
            validation={
                "curated": "manual",
                "manual": "manual",
                "canonical": "dictionary",
                "official": "dictionary",
                "generated": "generated",
                "machine": "unverified",
            }.get(source, "unverified"),
            post_count=post_count,
        )
    )


def resolve_collisions(aliases: list[Alias]) -> tuple[list[Alias], dict]:
    """Keep one tag per normalized surface, preferring curated/manual sources.

    Ambiguous aliases from the same tier are dropped rather than guessed. This
    matters for machine translations, where several tags often share the same
    Japanese phrase.
    """
    groups: dict[str, list[Alias]] = defaultdict(list)
    for alias in aliases:
        groups[alias.norm].append(alias)

    kept: list[Alias] = []
    dropped: list[dict] = []
    for norm, group in groups.items():
        # Best alias per tag first.
        by_tag: dict[str, Alias] = {}
        for alias in group:
            old = by_tag.get(alias.tag)
            if old is None or (
                alias.priority,
                alias.confidence,
                alias.post_count,
            ) > (old.priority, old.confidence, old.post_count):
                by_tag[alias.tag] = alias
        ranked = sorted(
            by_tag.values(),
            key=lambda a: (a.priority, a.confidence, a.post_count, len(a.norm)),
            reverse=True,
        )
        best = ranked[0]
        if len(ranked) > 1:
            second = ranked[1]
            # Same source tier and same confidence but different tags:
            # this surface is genuinely ambiguous. Drop it.
            if (
                best.priority == second.priority
                and abs(best.confidence - second.confidence) < 1e-9
            ):
                dropped.append(
                    {
                        "norm": norm,
                        "surface": best.surface,
                        "tags": [a.tag for a in ranked[:4]],
                        "reason": "ambiguous_same_tier",
                    }
                )
                continue
            # A machine alias must never override a curated/manual alias.
            if second.priority >= best.priority and second.source in {"curated", "manual"}:
                dropped.append(
                    {
                        "norm": norm,
                        "surface": second.surface,
                        "tags": [a.tag for a in ranked[:4]],
                        "reason": "higher_quality_source",
                    }
                )
                continue
        kept.append(best)

    kept.sort(key=lambda a: (a.tag, -a.priority, a.norm))
    return kept, {
        "input_aliases": len(aliases),
        "kept_aliases": len(kept),
        "dropped_aliases": len(aliases) - len(kept),
        "collision_groups": len(groups),
        "ambiguous_groups": len(dropped),
        "dropped_examples": dropped[:20],
    }


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def build(output: Path) -> dict:
    t0 = time.time()
    tags = load_tags()
    top = top_tags(tags)
    top_names = {t.name for t in top}
    for name in sorted(SUPPLEMENT_TAGS):
        if name in tags and name not in top_names:
            top.append(tags[name])
            top_names.add(name)
    top.sort(key=lambda t: (-t.post_count, t.name))
    manual, machine = load_translations()
    lexicon = load_lexicon()

    aliases: list[Alias] = []
    for tag in top:
        add_alias(aliases, tag.name, tag.name, "canonical", post_count=tag.post_count)
        add_alias(
            aliases,
            tag.name,
            tag.name.replace("_", " "),
            "canonical",
            post_count=tag.post_count,
        )
        for official in tag.aliases:
            if official.startswith("/"):
                continue
            if len(normalize(official)) < 2:
                continue
            add_alias(aliases, tag.name, official, "official", post_count=tag.post_count)

    for tag_name, jp in manual.items():
        if tag_name in top_names and jp:
            add_alias(aliases, tag_name, jp, "manual", post_count=tags[tag_name].post_count)

    for tag_name, jp in machine.items():
        if tag_name in MACHINE_BLOCKLIST:
            continue
        if tag_name in top_names and alias_is_usable_jp(jp):
            add_alias(
                aliases,
                tag_name,
                jp,
                "machine",
                post_count=tags[tag_name].post_count,
            )

    missing_lexicon: list[str] = []
    for surface, tag_name, source, confidence in lexicon:
        if tag_name not in top_names:
            missing_lexicon.append(tag_name)
            continue
        add_alias(
            aliases,
            tag_name,
            surface,
            source,
            confidence=confidence,
            post_count=tags[tag_name].post_count,
        )

    for surface, tag_name, source, confidence in generated_colour_aliases(top_names):
        add_alias(
            aliases,
            tag_name,
            surface,
            source,
            confidence=confidence,
            post_count=tags[tag_name].post_count,
        )

    kept, collision_stats = resolve_collisions(aliases)

    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = output.with_suffix(".tmp.sqlite")
    if tmp.exists():
        tmp.unlink()
    con = sqlite3.connect(tmp)
    cur = con.cursor()
    cur.executescript(
        """
        PRAGMA journal_mode=OFF;
        PRAGMA synchronous=OFF;
        PRAGMA page_size=4096;
        CREATE TABLE tags(
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL UNIQUE,
            category INTEGER NOT NULL,
            post_count INTEGER NOT NULL
        );
        CREATE TABLE aliases(
            id INTEGER PRIMARY KEY,
            tag_id INTEGER NOT NULL REFERENCES tags(id),
            surface TEXT NOT NULL,
            norm TEXT NOT NULL,
            source TEXT NOT NULL,
            lang TEXT NOT NULL,
            confidence REAL NOT NULL,
            priority INTEGER NOT NULL,
            tier TEXT NOT NULL,
            validation TEXT NOT NULL,
            UNIQUE(tag_id, norm, source)
        );
        CREATE INDEX idx_aliases_norm ON aliases(norm);
        CREATE INDEX idx_aliases_tag ON aliases(tag_id);
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        """
    )
    id_by_name: dict[str, int] = {}
    for i, tag in enumerate(top, 1):
        id_by_name[tag.name] = i
        cur.execute(
            "INSERT INTO tags(id,name,category,post_count) VALUES(?,?,?,?)",
            (i, tag.name, tag.category, tag.post_count),
        )
    for alias in kept:
        cur.execute(
            "INSERT OR REPLACE INTO aliases"
            "(tag_id,surface,norm,source,lang,confidence,priority,tier,validation)"
            " VALUES(?,?,?,?,?,?,?,?,?)",
            (
                id_by_name[alias.tag],
                alias.surface,
                alias.norm,
                alias.source,
                alias.lang,
                alias.confidence,
                alias.priority,
                alias.tier,
                alias.validation,
            ),
        )
    source_hashes = {
        "danbooru.csv": sha256(DATA / "danbooru.csv"),
        "danbooru-jp.csv": sha256(DATA / "danbooru-jp.csv"),
        "danbooru-only-machine-jp.csv": sha256(DATA / "danbooru-only-machine-jp.csv"),
        "ja_lexicon.tsv": sha256(ALIASES / "ja_lexicon.tsv"),
    }
    meta = {
        "schema_version": "m1-1",
        "built_at": datetime.now(timezone.utc).isoformat(),
        "top_n": str(len(top)),
        "tag_count": str(len(top)),
        "alias_count": str(len(kept)),
        "source_hashes": json.dumps(source_hashes, ensure_ascii=False, sort_keys=True),
        "collision_stats": json.dumps(collision_stats, ensure_ascii=False),
        "missing_lexicon_tags": json.dumps(sorted(set(missing_lexicon)), ensure_ascii=False),
    }
    cur.executemany("INSERT INTO meta(key,value) VALUES(?,?)", meta.items())
    con.commit()
    cur.execute("VACUUM")
    con.close()
    tmp.replace(output)

    by_source: dict[str, int] = defaultdict(int)
    by_lang: dict[str, int] = defaultdict(int)
    by_tier: dict[str, int] = defaultdict(int)
    for alias in kept:
        by_source[alias.source] += 1
        by_lang[alias.lang] += 1
        by_tier[alias.tier] += 1
    jp_tags = {a.tag for a in kept if a.lang == "ja"}
    stats = {
        "output": str(output),
        "size_bytes": output.stat().st_size,
        "build_seconds": round(time.time() - t0, 2),
        "tags": len(top),
        "aliases": len(kept),
        "tags_with_japanese_alias": len(jp_tags),
        "japanese_coverage": round(len(jp_tags) / len(top), 4),
        "by_source": dict(sorted(by_source.items())),
        "by_lang": dict(sorted(by_lang.items())),
        "by_tier": dict(sorted(by_tier.items())),
        "collisions": collision_stats,
        "missing_lexicon_tags": sorted(set(missing_lexicon)),
        "source_hashes": source_hashes,
    }
    return stats


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=db_path())
    parser.add_argument("--stats", type=Path, default=None)
    args = parser.parse_args()
    stats = build(args.output)
    if args.stats:
        args.stats.parent.mkdir(parents=True, exist_ok=True)
        args.stats.write_text(json.dumps(stats, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in stats.items() if k != "collisions"}, ensure_ascii=False, indent=2))
    print("collisions:", json.dumps(stats["collisions"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
