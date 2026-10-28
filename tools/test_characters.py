#!/usr/bin/env python3
"""Tests for lossless, ambiguity-preserving character catalog construction."""
import csv
import sqlite3
import tempfile
import unittest
from pathlib import Path

from build_characters import build
from character_search_spike import first_two
from m1_common import normalize


class CharacterCatalogTest(unittest.TestCase):
    def test_search_returns_top_two_titles_without_claiming_names(self):
        html = '''
          <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2Fone&amp;rut=x">Cinderella &amp; Nikke</a>
          <a class="result__a" href="https://example.org/two">日本語Wiki</a>
          <a class="result__a" href="https://example.org/three">Third result</a>
        '''
        self.assertEqual(first_two(html), [
            ("Cinderella & Nikke", "https://example.org/one"),
            ("日本語Wiki", "https://example.org/two"),
        ])

    def test_search_rejects_unsafe_link_and_empty_results(self):
        self.assertEqual(first_two('<a class="result__a" href="javascript:alert(1)">Bad</a>'), [])
        self.assertEqual(first_two('<html><body>No results</body></html>'), [])
        self.assertEqual(first_two('''<a class="result__a" href="javascript:bad">Bad</a>
          <a class="result__a" href="https://example.org/two">Second</a>
          <a class="result__a" href="https://example.org/three">Third</a>'''),
          [("Second", "https://example.org/two")])

    def test_ambiguous_names_and_curated_aliases(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source, aliases, output = root / "source.csv", root / "aliases.tsv", root / "db.sqlite"
            with source.open("w", encoding="utf-8", newline="") as stream:
                writer = csv.writer(stream)
                writer.writerow(["cinderella_(nikke)", "4", "585", "anachiro_(nikke)"])
                writer.writerow(["cinderella_(disney)", "4", "112", ""])
                writer.writerow(["goddess_of_victory:_nikke", "3", "16634", ""])
            aliases.write_text("シンデレラ\tcinderella_(nikke)\n", encoding="utf-8")
            count, _ = build(source, aliases, output)
            self.assertEqual(count, 2)
            with sqlite3.connect(output) as db:
                candidates = {r[0] for r in db.execute(
                    "SELECT tag FROM aliases WHERE norm = ?", (normalize("Cinderella"),))}
                self.assertEqual(candidates, {"cinderella_(nikke)", "cinderella_(disney)"})
                japanese = db.execute("SELECT tag FROM aliases WHERE norm = ?",
                                      (normalize("シンデレラ"),)).fetchall()
                self.assertEqual(japanese, [("cinderella_(nikke)",)])
                self.assertEqual(db.execute("SELECT COUNT(*) FROM characters").fetchone()[0], 2)
            with self.assertRaises(FileExistsError):
                build(source, aliases, output)

    def test_unknown_curated_tag_fails_before_creating_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source, aliases, output = root / "source.csv", root / "aliases.tsv", root / "db.sqlite"
            source.write_text("character,4,10,\n", encoding="utf-8")
            aliases.write_text("名前\tmissing\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                build(source, aliases, output)
            self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
