#!/usr/bin/env python3
"""Fetch a held-out Japanese-caption evaluation set from Danbot 2408.

Dataset: dartags/danbooru-2408-blind-captions (MIT)
  https://huggingface.co/datasets/dartags/danbooru-2408-blind-captions

The dataset has 6.33M rows. We fetch the first 100 valid rows as a dev split
and 100 rows from separate offsets as a test split. Training/alias mining uses
different offsets, so the test rows are not used to tune the lexicon.
"""
from __future__ import annotations

import argparse
import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from build_m1 import SUPPLEMENT_TAGS
from m1_common import canonical_map, canonicalize, load_tags, top_tags

DATASET = "dartags/danbooru-2408-blind-captions"
API = "https://datasets-server.huggingface.co/rows"


def fetch(offset: int, length: int = 100) -> list[dict]:
    query = urllib.parse.urlencode(
        {
            "dataset": DATASET,
            "config": "default",
            "split": "train",
            "offset": offset,
            "length": length,
        }
    )
    url = f"{API}?{query}"
    last_error: Exception | None = None
    for attempt in range(6):
        try:
            with urllib.request.urlopen(url, timeout=90) as response:
                data = json.load(response)
            return [row["row"] for row in data["rows"]]
        except Exception as exc:  # network / rate limit
            last_error = exc
            time.sleep(2 + attempt * 3)
    raise RuntimeError(f"failed to fetch offset={offset}: {last_error}")


def build_record(row: dict, split: str, allowed: set[str], mapping: dict[str, str]) -> dict | None:
    text = (row.get("summary_ja") or "").strip()
    if len(text) < 20 or len(text) > 1200:
        return None
    expected_all: list[str] = []
    for raw in row.get("summary_tags") or []:
        tag = canonicalize(str(raw), mapping)
        if not tag or tag not in allowed:
            continue
        if tag not in expected_all:
            expected_all.append(tag)
    return {
        "source": DATASET,
        "source_id": row.get("id"),
        "split": split,
        "rating": row.get("rating"),
        "input": text,
        "summary_en": (row.get("summary_en") or "").strip(),
        "expected_tags": expected_all,
        "expected_all": expected_all,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("research/eval/eval_200.jsonl"))
    parser.add_argument("--dev-offset", type=int, default=0)
    parser.add_argument("--test-offsets", type=str, default="1500000,3000000,4500000")
    args = parser.parse_args()

    tags = load_tags()
    top = top_tags(tags)
    top_names = {t.name for t in top}
    for name in SUPPLEMENT_TAGS:
        if name in tags:
            top_names.add(name)
    general = {name for name in top_names if tags[name].category == 0}
    mapping = canonical_map(tags)

    records: list[dict] = []
    dev = fetch(args.dev_offset)
    for row in dev:
        rec = build_record(row, "dev", top_names, mapping)
        if rec is None:
            continue
        rec["expected_tags"] = [t for t in rec["expected_all"] if t in general]
        if len(rec["expected_tags"]) >= 5:
            records.append(rec)
        if sum(1 for r in records if r["split"] == "dev") >= 100:
            break

    test_offsets = [int(x) for x in args.test_offsets.split(",") if x.strip()]
    for offset in test_offsets:
        for row in fetch(offset):
            rec = build_record(row, "test", top_names, mapping)
            if rec is None:
                continue
            rec["expected_tags"] = [t for t in rec["expected_all"] if t in general]
            if len(rec["expected_tags"]) >= 5:
                records.append(rec)
            if sum(1 for r in records if r["split"] == "test") >= 100:
                break
        if sum(1 for r in records if r["split"] == "test") >= 100:
            break

    if len(records) < 200:
        raise SystemExit(f"only {len(records)} valid records; adjust offsets")
    records = records[:200]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as f:
        for rec in records:
            f.write(json.dumps(rec, ensure_ascii=False) + "\n")
    dev_n = sum(1 for r in records if r["split"] == "dev")
    test_n = len(records) - dev_n
    print(
        json.dumps(
            {
                "output": str(args.output),
                "records": len(records),
                "dev": dev_n,
                "test": test_n,
                "avg_expected_tags": round(
                    sum(len(r["expected_tags"]) for r in records) / len(records), 2
                ),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
