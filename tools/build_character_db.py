#!/usr/bin/env python3
"""Build app/src/main/assets/characterdb.sqlite from character.md.

character.md is the reviewable source of truth. This script only turns the
tables into a read-only SQLite asset; it never invents names. Every row keeps
its note and source series so the app can show why a candidate exists.

Run from the repository root:
    python3 tools/build_character_db.py
"""

from __future__ import annotations

import re
import sqlite3
import unicodedata
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "character.md"
TARGET = ROOT / "app/src/main/assets/characterdb.sqlite"

SERIES = {
    "干物妹!うまるちゃん": ("umaru", "干物妹!うまるちゃん"),
    "崩壊：スターレイル": ("hsr", "崩壊：スターレイル"),
    "原神": ("genshin", "原神"),
    "ゼンレスゾーンゼロ": ("zzz", "ゼンレスゾーンゼロ"),
    "鳴潮": ("wuthering", "鳴潮"),
    "勝利の女神：NIKKE": ("nikke", "勝利の女神：NIKKE"),
    "学園アイドルマスター": ("gakumas", "学園アイドルマスター"),
    "マブラヴ ガールズガーデン": ("muvluv_gg", "マブラヴ ガールズガーデン"),
    "Re:ゼロから始める異世界生活": ("rezero", "Re:ゼロから始める異世界生活"),
    "この素晴らしい世界に祝福を！": ("konosuba", "この素晴らしい世界に祝福を！"),
}

# Same-form aliases that are used in the works but are not derivable from the
# name itself. Keyed by Japanese canonical name.
EXTRA_ALIASES = {
    "土間埋": ["うまる", "UMR", "こまる", "土間うまる"],
    "本場猛": ["ぼんば"],
    "フェリックス・アーガイル": ["フェリス"],
    "アルデバラン": ["アル"],
    "三月なのか": ["なのか"],
    "シンデレラ": ["Cinderella"],
    "ラピ": ["Rapi"],
}

KANA = re.compile(r"^[ぁ-んァ-ヶー・\s]+$")
PARENT = re.compile(r"^(.{1,30}?)(?:とは|と)(?:別形態|別ユニット|別レコード|別個体)")


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKC", text).lower()
    return re.sub(r"[\s・·'’\-]", "", text)


def parse(md: str):
    blocks = md.split("\n## ")[1:]
    series_rows = []
    characters = []
    for block in blocks:
        title = block.splitlines()[0].strip()
        if title not in SERIES:
            continue
        key, name = SERIES[title]
        source = ""
        scope = ""
        for line in block.splitlines():
            if line.startswith("出典:"):
                source = line[len("出典:"):].strip()
            elif line and not line.startswith(("|", "#", "-", "更新日")) and not source:
                scope = line.strip()
        series_rows.append((key, name, source))
        table = []
        for line in block.splitlines():
            if not line.startswith("|") or line.startswith("|---"):
                continue
            cells = [c.strip() for c in line.strip("|").split("|")]
            if cells[0] in ("日本語名",) or len(cells) < 2:
                continue
            table.append(cells)
        for cells in table:
            ja, en = cells[0], cells[1]
            note = cells[2] if len(cells) > 2 else ""
            if not ja or not en:
                continue
            category = "base"
            lowered = note
            if "別形態" in lowered or "別ユニット" in lowered or "別レコード" in lowered or "別個体" in lowered:
                category = "variant"
            elif "コラボ" in lowered:
                category = "collab"
            elif "教員" in lowered:
                category = "staff"
            elif "実装状態要確認" in lowered:
                category = "unreleased"
            verified = "公式" in lowered and "未確認" not in lowered
            characters.append(
                {
                    "series": key,
                    "ja": ja,
                    "en": en,
                    "note": note,
                    "category": category,
                    "status": "verified" if verified else "candidate",
                }
            )
    return series_rows, characters


def aliases_for(row) -> list[tuple[str, str, str]]:
    out: list[tuple[str, str, str]] = []

    def add(text: str, lang: str, kind: str):
        text = text.strip()
        if not text:
            return
        if (text, lang, kind) not in out:
            out.append((text, lang, kind))

    ja, en, note = row["ja"], row["en"], row["note"]
    add(ja, "ja", "canonical")
    add(en, "en", "canonical")

    for part in re.split(r"[・／/＆&\s]+", ja):
        if len(part) >= 2 and part != ja:
            add(part, "ja", "short")
    for part in re.split(r"[:：•]", en):
        if len(part) >= 3 and part.strip() != en:
            add(part, "en", "short")

    for token in re.split(r"[、,／/]", note.split("。")[0]):
        token = token.strip()
        if KANA.fullmatch(token) and 1 < len(token) <= 12:
            add(token, "ja", "reading")

    marker = "は別名"
    if marker in note:
        for token in re.split(r"[、,／/]", note.split(marker)[0]):
            token = token.strip().strip("。")
            if token and len(token) <= 20:
                add(token, "ja" if re.search(r"[ぁ-んァ-ヶ一-龠]", token) else "en", "alias")

    for extra in EXTRA_ALIASES.get(ja, []):
        add(extra, "ja" if re.search(r"[ぁ-んァ-ヶ一-龠]", extra) else "en", "alias")
    return out


def build():
    md = SOURCE.read_text(encoding="utf-8")
    series_rows, characters = parse(md)
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    if TARGET.exists():
        TARGET.unlink()
    db = sqlite3.connect(TARGET)
    db.executescript(
        """
        PRAGMA journal_mode=OFF;
        CREATE TABLE series(
            id INTEGER PRIMARY KEY,
            key TEXT UNIQUE NOT NULL,
            name_ja TEXT NOT NULL,
            source TEXT NOT NULL
        );
        CREATE TABLE characters(
            id INTEGER PRIMARY KEY,
            series_id INTEGER NOT NULL REFERENCES series(id),
            name_ja TEXT NOT NULL,
            name_en TEXT NOT NULL,
            category TEXT NOT NULL,
            parent_name TEXT,
            note TEXT NOT NULL,
            status TEXT NOT NULL
        );
        CREATE TABLE aliases(
            character_id INTEGER NOT NULL REFERENCES characters(id),
            text TEXT NOT NULL,
            text_norm TEXT NOT NULL,
            lang TEXT NOT NULL,
            kind TEXT NOT NULL
        );
        CREATE INDEX aliases_norm ON aliases(text_norm);
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
        """
    )
    for key, name, source in series_rows:
        db.execute("INSERT INTO series(key,name_ja,source) VALUES(?,?,?)", (key, name, source))
    series_id = {key: i for i, key in enumerate([r[0] for r in series_rows], start=1)}

    for row in characters:
        parent = PARENT.match(row["note"])
        db.execute(
            "INSERT INTO characters(series_id,name_ja,name_en,category,parent_name,note,status)"
            " VALUES(?,?,?,?,?,?,?)",
            (
                series_id[row["series"]],
                row["ja"],
                row["en"],
                row["category"],
                parent.group(1) if parent else None,
                row["note"],
                row["status"],
            ),
        )
        cid = db.execute("SELECT last_insert_rowid()").fetchone()[0]
        for text, lang, kind in aliases_for(row):
            db.execute(
                "INSERT INTO aliases(character_id,text,text_norm,lang,kind) VALUES(?,?,?,?,?)",
                (cid, text, normalize(text), lang, kind),
            )

    meta = {
        "built_at": date.today().isoformat(),
        "source": "character.md",
        "policy": "alternate forms, costumes and units are separate records",
        "series_count": str(len(series_rows)),
        "character_count": str(len(characters)),
    }
    db.executemany("INSERT INTO meta(key,value) VALUES(?,?)", meta.items())
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"wrote {TARGET} ({len(characters)} characters, {len(series_rows)} series)")


if __name__ == "__main__":
    build()
