#!/usr/bin/env python3
"""Build a character catalog from the local Danbooru tag export.

The export does not describe copyright relationships.  Parenthesized suffixes
are therefore kept as part of the tag, never guessed into a copyright tag.
Japanese aliases are accepted only from the explicitly curated TSV.
"""
from __future__ import annotations

import argparse
import csv
import sqlite3
from pathlib import Path

from m1_common import DATA, normalize

ROOT = Path(__file__).resolve().parent.parent
ALIASES = ROOT / "research" / "aliases" / "character_aliases.tsv"


def build(source: Path, aliases: Path, output: Path) -> tuple[int, int]:
    """Build to a new file; fail instead of silently losing curated aliases."""
    if output.exists():
        raise FileExistsError(f"Output already exists: {output}")
    characters: dict[str, tuple[int, list[str]]] = {}
    with source.open(encoding="utf-8", newline="") as stream:
        for row in csv.reader(stream):
            if len(row) < 3 or row[1] != "4":
                continue
            tag = row[0].strip()
            if not tag:
                continue
            characters[tag] = (int(row[2]), row[3].split(",") if len(row) > 3 else [])

    curated: list[tuple[str, str]] = []
    with aliases.open(encoding="utf-8", newline="") as stream:
        for row in csv.reader(stream, delimiter="\t"):
            if not row or row[0].startswith("#"):
                continue
            if len(row) != 2 or not all(row):
                raise ValueError(f"Invalid alias row: {row!r}")
            surface, tag = row
            if tag not in characters:
                raise ValueError(f"Unknown character tag in curated aliases: {tag}")
            curated.append((surface, tag))

    output.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(output)
    try:
        con.executescript("""
            CREATE TABLE characters (
                tag TEXT PRIMARY KEY, post_count INTEGER NOT NULL
            );
            CREATE TABLE aliases (
                norm TEXT NOT NULL, surface TEXT NOT NULL,
                tag TEXT NOT NULL REFERENCES characters(tag), source TEXT NOT NULL,
                PRIMARY KEY (norm, tag, source)
            );
            CREATE INDEX aliases_norm ON aliases(norm);
        """)
        con.executemany("INSERT INTO characters VALUES (?, ?)",
                        ((tag, data[0]) for tag, data in sorted(characters.items())))
        rows: set[tuple[str, str, str, str]] = set()
        for tag, (_, official) in characters.items():
            # Keep the full canonical tag and the unqualified name: the latter
            # deliberately returns multiple candidates when names collide.
            base = tag.split("_(", 1)[0]
            for surface, kind in [(tag, "canonical"), (base, "base"),
                                  *((alias.strip(), "official") for alias in official)]:
                if surface and normalize(surface):
                    rows.add((normalize(surface), surface, tag, kind))
        for surface, tag in curated:
            rows.add((normalize(surface), surface, tag, "curated"))
        # A normalized alias can have multiple surface spellings.  Select one
        # deterministically per (norm, tag, source), but keep distinct tags.
        for row in sorted(rows):
            con.execute("INSERT OR IGNORE INTO aliases VALUES (?, ?, ?, ?)", row)
        con.commit()
        count = con.execute("SELECT COUNT(*) FROM aliases").fetchone()[0]
        return len(characters), count
    finally:
        con.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DATA / "danbooru.csv")
    parser.add_argument("--aliases", type=Path, default=ALIASES)
    parser.add_argument("--output", type=Path, required=True,
                        help="New output path; existing files are never overwritten")
    args = parser.parse_args()
    characters, aliases = build(args.source, args.aliases, args.output)
    print(f"characters={characters} aliases={aliases} output={args.output}")


if __name__ == "__main__":
    main()
