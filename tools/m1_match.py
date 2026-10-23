#!/usr/bin/env python3
"""Reference Japanese -> Danbooru matcher for the M1 database.

This is the build-time reference implementation. It uses:
  1. a trie of Japanese aliases stored in promptdb.sqlite
  2. longest-match + kana-boundary guard
  3. a small set of compositional rules (colour + noun, size + noun)
  4. conservative inference (solo, multiple_girls)

The app will port the same behaviour to Kotlin in M2.
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

from build_m1 import COLORS, EYE_COLOR, HAIR_COLOR, NOUNS
from m1_common import TERM, boundary_ok, db_path, normalize

# A matched tag plus the span that produced it.
@dataclass(frozen=True)
class Match:
    tag: str
    surface: str
    source: str
    start: int
    end: int
    confidence: float


def _tag_for_colour(colour: str, noun: str) -> str:
    if noun == "hair":
        prefix = HAIR_COLOR.get(colour, colour)
    elif noun == "eyes":
        prefix = EYE_COLOR.get(colour, colour)
    else:
        prefix = colour
    return f"{prefix}_{noun}"


def _build_composite_rules(tags: set[str]) -> list[tuple[re.Pattern[str], str]]:
    groups: dict[str, set[str]] = defaultdict(set)
    for surface, colour in COLORS:
        norm_surface = normalize(surface)
        groups[colour].add(norm_surface)
        if norm_surface.endswith("い"):
            groups[colour].add(norm_surface[:-1])
    rules: list[tuple[re.Pattern[str], str]] = []
    for colour, surfaces in groups.items():
        colour_re = "(?:" + "|".join(
            re.escape(s) for s in sorted(surfaces, key=len, reverse=True)
        ) + ")"
        for noun_surface, noun in NOUNS:
            tag = _tag_for_colour(colour, noun)
            if tag not in tags:
                continue
            # あかいかみ / あかいながいかみ / しろいふりるのしゃつ / ぴんくのどれす
            rules.append(
                (
                    re.compile(
                        colour_re + r".{0,4}?" + re.escape(normalize(noun_surface))
                    ),
                    tag,
                )
            )
    return rules


SIZE_RULES: tuple[tuple[str, str], ...] = (
    (r"(?:とても|非常に|すごく)長[いく].{0,10}?髪", "very_long_hair"),
    (r"長[いく].{0,10}?髪", "long_hair"),
    (r"短[いく].{0,10}?髪", "short_hair"),
    (r"髪.{0,10}?短く", "short_hair"),
    (r"髪.{0,10}?長く", "long_hair"),
    (r"中くらいの.{0,4}?髪", "medium_hair"),
    (r"(?:大きな|大きい|豊満な).{0,4}?胸", "large_breasts"),
    (r"(?:中くらいの|普通の).{0,4}?胸", "medium_breasts"),
    (r"(?:小さな|小さい).{0,4}?胸", "small_breasts"),
    (r"長い.{0,4}?袖", "long_sleeves"),
    (r"(?:短い|半).{0,2}?袖", "short_sleeves"),
    (r"(?:しんぷる|単色|無地|単純).{0,6}?背景", "simple_background"),
    (r"背景.{0,4}?(?:しんぷる|単色|無地|単純)", "simple_background"),
)

# Colour/clothing specific tags imply their generic parent on Danbooru.
SUFFIX_PARENT: dict[str, str] = {
    "_dress": "dress",
    "_shirt": "shirt",
    "_skirt": "skirt",
    "_ribbon": "ribbon",
    "_panties": "panties",
    "_bra": "bra",
    "_thighhighs": "thighhighs",
    "_pantyhose": "pantyhose",
    "_socks": "socks",
    "_gloves": "gloves",
    "_hat": "hat",
    "_wings": "wings",
    "_bow": "bow",
    "_necktie": "necktie",
    "_vest": "vest",
    "_coat": "coat",
    "_swimsuit": "swimsuit",
    "_bikini": "bikini",
    "_yukata": "yukata",
    "_kimono": "kimono",
    "_apron": "apron",
    "_shoes": "shoes",
    "_boots": "boots",
    "_scarf": "scarf",
    "_necklace": "necklace",
    "_earrings": "earrings",
    "_sailor_collar": "sailor_collar",
    "_leotard": "leotard",
    "_bodysuit": "bodysuit",
    "_camisole": "camisole",
    "_blouse": "blouse",
    "_tail": "tail",
    "_sword": "sword",
    "_weapon": "weapon",
}

PARENT_OF: dict[str, tuple[str, ...]] = {
    "large_breasts": ("breasts",),
    "medium_breasts": ("breasts",),
    "small_breasts": ("breasts",),
    "very_long_hair": ("long_hair",),
    "cat_ears": ("animal_ears",),
    "dog_ears": ("animal_ears",),
    "fox_ears": ("animal_ears",),
    "rabbit_ears": ("animal_ears",),
    "serafuku": ("school_uniform", "sailor_collar"),
    "panties": ("underwear",),
    "bra": ("underwear",),
    "on_back": ("lying",),
    "on_stomach": ("lying",),
    "seiza": ("sitting",),
    "kneeling": ("sitting",),
    "light_smile": ("smile",),
    "laughing": ("smile",),
    "crying": ("tears",),
    "2girls": ("multiple_girls",),
    "3girls": ("multiple_girls",),
    "4girls": ("multiple_girls",),
    "5girls": ("multiple_girls",),
    "6+girls": ("multiple_girls",),
    "cover_page": ("cover",),
    "doujin_cover": ("cover",),
    "holding_sword": ("holding", "sword", "weapon"),
    "holding_weapon": ("holding", "weapon"),
    "sword": ("weapon",),
    "gun": ("weapon",),
    "spear": ("weapon",),
    "knife": ("weapon",),
    "bow_(weapon)": ("weapon",),
    "armored_dress": ("armor",),
    "breastplate": ("armor",),
    "faulds": ("armor",),
    "gauntlets": ("armor",),
    "shoulder_armor": ("armor",),
    "on_chair": ("chair", "sitting"),
    "office_chair": ("chair",),
    "on_bed": ("bed",),
    # Parent categories that are commonly present alongside the specific tag
    # in Danbooru's summary tags.
    "bikini": ("swimsuit",),
    "one-piece_swimsuit": ("swimsuit",),
    "school_swimsuit": ("swimsuit",),
    "kimono": ("japanese_clothes",),
    "yukata": ("japanese_clothes",),
    "beret": ("hat",),
    "mob_cap": ("hat",),
    "witch_hat": ("hat",),
    "santa_hat": ("hat",),
    "military_uniform": ("military",),
    "sniper_rifle": ("rifle", "gun", "weapon"),
    "rifle": ("gun", "weapon"),
    "cleavage": ("breasts",),
    "profile": ("from_side",),
    "ribbon": ("bow",),
    "single_braid": ("braid",),
    "twin_braids": ("braid", "long_hair"),
    "star_(sky)": ("starry_sky",),
    "starry_sky": ("sky",),
    "peach": ("fruit", "food"),
    "fruit": ("food",),
    "mosaic_censoring": ("censored",),
    "ejaculation": ("cum",),
    "pussy": ("vaginal",),
    "leotard_aside": ("clothing_aside",),
    "breasts_out": ("nipples",),
    "topless": ("breasts", "nipples"),
    "thigh_boots": ("boots",),
}

FEMALE_CUES = {
    "skirt",
    "dress",
    "miniskirt",
    "pleated_skirt",
    "long_skirt",
    "serafuku",
    "breasts",
    "large_breasts",
    "medium_breasts",
    "small_breasts",
    "panties",
    "bra",
    "underwear",
    "thighhighs",
    "pantyhose",
    "hair_ribbon",
    "hair_ornament",
    "maid",
    "apron",
    "swimsuit",
    "bikini",
    "yukata",
    "kimono",
    "zettai_ryouiki",
    "cleavage",
    "high_heels",
    "twintails",
    "nude",
    "topless",
    "breasts_out",
    "pussy",
    "vaginal",
    "leotard_aside",
}
MALE_CUES = {
    "1boy",
    "male",
    "male_focus",
    "faceless_male",
    "beard",
    "muscular",
    "penis",
    "testicles",
    "erection",
    "ejaculation",
    "cum",
}
MULTIPLE_TAGS = {"multiple_girls", "2girls", "3girls", "4girls", "5girls", "6+girls"}


class Matcher:
    def __init__(self, path: Path | None = None):
        self.path = path or db_path()
        if not self.path.exists():
            raise SystemExit(f"database not found: {self.path}\nRun: python3 tools/build_m1.py")
        self.tags: set[str] = set()
        self.tag_counts: dict[str, int] = {}
        self.root: dict = {}
        self.composite_rules: list[tuple[re.Pattern[str], str]] = []
        self._load()

    def _load(self) -> None:
        con = sqlite3.connect(self.path)
        cur = con.cursor()
        for name, count in cur.execute("SELECT name, post_count FROM tags"):
            self.tags.add(name)
            self.tag_counts[name] = count
        rows = cur.execute(
            "SELECT t.name, a.surface, a.norm, a.source, a.confidence, a.priority "
            "FROM aliases a JOIN tags t ON t.id=a.tag_id WHERE a.lang='ja'"
        )
        for name, surface, norm, source, confidence, priority in rows:
            node = self.root
            for ch in norm:
                node = node.setdefault(ch, {})
            node.setdefault(TERM, []).append(
                (priority, confidence, self.tag_counts.get(name, 0), name, surface, source)
            )
        con.close()
        self.composite_rules = _build_composite_rules(self.tags)

    def _dictionary_matches(self, text: str) -> list[Match]:
        out: list[Match] = []
        i = 0
        while i < len(text):
            node = self.root
            j = i
            best = None
            while j < len(text) and text[j] in node:
                node = node[text[j]]
                j += 1
                if TERM in node:
                    if best is None or j - i > best[0]:
                        best = (j - i, i, j, node[TERM])
            if best:
                _, start, end, candidates = best
                if boundary_ok(text, start, end, candidates[0][4]):
                    priority, confidence, count, tag, surface, source = max(
                        candidates,
                        key=lambda c: (c[0], c[1], c[2], len(c[4])),
                    )
                    out.append(Match(tag, surface, source, start, end, confidence))
                    i = end
                    continue
            i += 1
        return out

    def _rule_matches(self, text: str, occupied: list[tuple[int, int]], tags: set[str]) -> list[Match]:
        out: list[Match] = []
        for pattern, tag in self.composite_rules + list(
            (re.compile(p), t) for p, t in SIZE_RULES
        ):
            if tag in tags:
                continue
            for m in pattern.finditer(text):
                start, end = m.span()
                # Skip only when the rule is wholly inside an existing match
                # (e.g. 赤い髪 -> red_hair). A rule may deliberately span over
                # a matched colour/clothing tag (長い赤髪 -> long_hair + red_hair).
                if any(a <= start and end <= b for a, b in occupied):
                    continue
                out.append(Match(tag, m.group(0), "composed", start, end, 0.8))
                tags.add(tag)
                break
        return out

    def _add_inferred(
        self,
        matches: list[Match],
        tags: set[str],
        tag: str,
        confidence: float,
        surface: str = "",
    ) -> None:
        if tag in tags or tag not in self.tags:
            return
        matches.append(Match(tag, surface, "inferred", 0, 0, confidence))
        tags.add(tag)

    def _expand_parents(self, matches: list[Match], tags: set[str]) -> None:
        queue = list(tags)
        while queue:
            tag = queue.pop()
            parents: list[str] = list(PARENT_OF.get(tag, ()))
            for suffix, parent in SUFFIX_PARENT.items():
                if tag.endswith(suffix) and tag != parent:
                    parents.append(parent)
            for parent in parents:
                if parent in tags or parent not in self.tags:
                    continue
                matches.append(Match(parent, "", "parent", 0, 0, 0.7))
                tags.add(parent)
                queue.append(parent)

    def _infer(self, text: str, matches: list[Match], tags: set[str]) -> None:
        # Count inference must run before gender inference: "二人の少女" is
        # 2girls, not 1girl + solo. Only infer girls when there is a female
        # cue and no male cue/person.
        multiple = tags & MULTIPLE_TAGS
        male_present = "1boy" in tags or bool(tags & MALE_CUES)
        female_context = bool(tags & FEMALE_CUES) or "1girl" in tags
        if not multiple:
            if re.search(r"二人|2人|ふたり|もう一人|両者", text):
                if female_context and not male_present:
                    self._add_inferred(matches, tags, "2girls", 0.7, "two people")
                    self._add_inferred(matches, tags, "multiple_girls", 0.65, "two people")
                elif male_present and not female_context:
                    self._add_inferred(matches, tags, "2boys", 0.6, "two people")
            elif re.search(r"三人|3人|みたり", text):
                if female_context and not male_present:
                    self._add_inferred(matches, tags, "3girls", 0.65, "three people")
                    self._add_inferred(matches, tags, "multiple_girls", 0.6, "three people")
            elif "それぞれ" in text and "と" in text and female_context and not male_present:
                # Two named characters are often described with それぞれ
                # rather than 二人.
                self._add_inferred(matches, tags, "2girls", 0.6, "それぞれ")
                self._add_inferred(matches, tags, "multiple_girls", 0.55, "それぞれ")

        multiple = tags & MULTIPLE_TAGS
        # Gender inference from strong visual cues. Danbooru captions usually
        # use 1girl for a single female subject, even when the character name is
        # the only explicit subject.
        if "1girl" not in tags and "1boy" not in tags and not multiple:
            if female_context and not tags & MALE_CUES:
                self._add_inferred(matches, tags, "1girl", 0.65, "female cues")
            elif tags & MALE_CUES and not female_context:
                self._add_inferred(matches, tags, "1boy", 0.6, "male cues")

        # Strong anatomical cues can reveal the other participant in a scene
        # where only one person was named explicitly.
        strong_male = tags & {
            "penis",
            "testicles",
            "erection",
            "ejaculation",
            "cum",
            "male_focus",
            "faceless_male",
            "beard",
            "muscular",
        }
        strong_female = tags & {
            "breasts",
            "large_breasts",
            "medium_breasts",
            "small_breasts",
            "pussy",
            "vaginal",
            "panties",
            "bra",
            "underwear",
        }
        if strong_male and "1boy" not in tags:
            self._add_inferred(matches, tags, "1boy", 0.6, "male anatomy")
        if strong_female and "1girl" not in tags:
            self._add_inferred(matches, tags, "1girl", 0.6, "female anatomy")

        # Explicit sex scenes are normally heterosexual unless marked yuri.
        if "sex" in tags and not (tags & {"yuri", "lesbian"}):
            if "1girl" not in tags:
                self._add_inferred(matches, tags, "1girl", 0.55, "sex scene")
            if "1boy" not in tags:
                self._add_inferred(matches, tags, "1boy", 0.55, "sex scene")

        # Nudity/toplessness of a female subject usually exposes the chest.
        if "1girl" in tags and (tags & {"nude", "topless"}):
            self._add_inferred(matches, tags, "breasts", 0.6, "female nudity")
            self._add_inferred(matches, tags, "nipples", 0.55, "female nudity")

        multiple = tags & MULTIPLE_TAGS
        if multiple & {"2girls", "3girls", "4girls", "5girls", "6+girls"}:
            self._add_inferred(matches, tags, "multiple_girls", 0.6, "multiple girls")
        if "1girl" in tags and "1boy" in tags:
            self._add_inferred(matches, tags, "hetero", 0.5, "1girl+1boy")
        if re.search(r"百合|ゆり|レズ", text) and "1boy" not in tags:
            self._add_inferred(matches, tags, "yuri", 0.7, "yuri")
        elif multiple and "1boy" not in tags and re.search(r"抱きつ|キス|口づけ", text):
            self._add_inferred(matches, tags, "yuri", 0.55, "intimate girls")

        # A single person with no multiple-person tag is usually tagged solo.
        # For a lone boy, require an explicit solo cue to avoid treating a
        # two-person scene where only the boy was recognised as solo.
        people = tags & {"1girl", "1boy"}
        multiple = tags & MULTIPLE_TAGS
        if len(people) == 1 and not multiple:
            if "1girl" in people:
                self._add_inferred(matches, tags, "solo", 0.55, "single person")
            elif re.search(r"一人|独り|単独|1人|ひとり", text):
                self._add_inferred(matches, tags, "solo", 0.55, "single person")

    def match(self, text: str) -> list[Match]:
        text = normalize(text)
        matches = self._dictionary_matches(text)
        occupied = [(m.start, m.end) for m in matches]
        tags = {m.tag for m in matches}
        matches.extend(self._rule_matches(text, occupied, tags))
        self._expand_parents(matches, tags)
        self._infer(text, matches, tags)

        # One match per tag, keeping the most confident / earliest occurrence.
        best: dict[str, Match] = {}
        for m in matches:
            old = best.get(m.tag)
            if old is None or (m.confidence, -m.start) > (old.confidence, -old.start):
                best[m.tag] = m
        return sorted(best.values(), key=lambda m: (m.start, -m.confidence, m.tag))

    def unmatched(self, text: str) -> str:
        text = normalize(text)
        matches = self.match(text)
        spans = sorted((m.start, m.end) for m in matches if m.end > m.start)
        out, pos = [], 0
        for start, end in spans:
            if start > pos:
                out.append(text[pos:start])
            pos = max(pos, end)
        if pos < len(text):
            out.append(text[pos:])
        return " / ".join(x for x in out if x)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("text", nargs="+")
    parser.add_argument("--db", type=Path, default=None)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    matcher = Matcher(args.db)
    text = " ".join(args.text)
    matches = matcher.match(text)
    if args.json:
        print(
            json.dumps(
                {
                    "input": text,
                    "tags": [m.tag for m in matches],
                    "matches": [
                        {
                            "tag": m.tag,
                            "surface": m.surface,
                            "source": m.source,
                            "confidence": m.confidence,
                        }
                        for m in matches
                    ],
                    "unmatched": matcher.unmatched(text),
                },
                ensure_ascii=False,
                indent=2,
            )
        )
    else:
        print(", ".join(m.tag for m in matches))
        print("UNMATCHED:", matcher.unmatched(text))


if __name__ == "__main__":
    main()
