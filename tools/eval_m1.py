#!/usr/bin/env python3
"""Evaluate the M1 matcher on the held-out Danbot 200-caption set."""
from __future__ import annotations

import argparse
import json
import sqlite3
from collections import Counter, defaultdict
from pathlib import Path

from m1_common import db_path
from m1_match import Matcher


def category_map(path: Path) -> dict[str, int]:
    con = sqlite3.connect(path)
    out = {name: category for name, category in con.execute("SELECT name, category FROM tags")}
    con.close()
    return out


def evaluate(records: list[dict], matcher: Matcher, categories: dict[str, int]) -> dict:
    tp = fp = fn = 0
    invalid = 0
    output_total = 0
    records_with_match = 0
    per_record = []
    missed = Counter()
    false_pos = Counter()
    by_rating: dict[str, dict] = defaultdict(lambda: {"tp": 0, "fp": 0, "fn": 0, "n": 0})

    for rec in records:
        matches = matcher.match(rec["input"])
        got = [m.tag for m in matches]
        got_general = [tag for tag in got if categories.get(tag) == 0]
        expected = set(rec["expected_tags"])
        got_set = set(got_general)
        r_tp = len(expected & got_set)
        r_fp = len(got_set - expected)
        r_fn = len(expected - got_set)
        tp += r_tp
        fp += r_fp
        fn += r_fn
        invalid += sum(1 for tag in got if tag not in categories)
        output_total += len(got)
        if got_set:
            records_with_match += 1
        missed.update(expected - got_set)
        false_pos.update(got_set - expected)
        rating = rec.get("rating") or "?"
        bucket = by_rating[rating]
        bucket["tp"] += r_tp
        bucket["fp"] += r_fp
        bucket["fn"] += r_fn
        bucket["n"] += 1
        per_record.append(
            {
                "source_id": rec.get("source_id"),
                "rating": rating,
                "input": rec["input"],
                "expected": sorted(expected),
                "got": got,
                "got_general": sorted(got_set),
                "missed": sorted(expected - got_set),
                "false_positives": sorted(got_set - expected),
                "tp": r_tp,
                "fp": r_fp,
                "fn": r_fn,
            }
        )

    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    macro_recall = sum(
        (r["tp"] / (r["tp"] + r["fn"])) if (r["tp"] + r["fn"]) else 0.0 for r in per_record
    ) / len(per_record)
    macro_precision = sum(
        (r["tp"] / (r["tp"] + r["fp"])) if (r["tp"] + r["fp"]) else 0.0 for r in per_record
    ) / len(per_record)
    return {
        "records": len(records),
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "recall": round(recall, 4),
        "precision": round(precision, 4),
        "f1": round(f1, 4),
        "macro_recall": round(macro_recall, 4),
        "macro_precision": round(macro_precision, 4),
        "invalid_tags": invalid,
        "output_tags": output_total,
        "invalid_rate": round(invalid / output_total, 5) if output_total else 0.0,
        "records_with_match": records_with_match,
        "avg_output_general_tags": round(
            sum(len(r["got_general"]) for r in per_record) / len(per_record), 2
        ),
        "top_missed": missed.most_common(30),
        "top_false_positives": false_pos.most_common(30),
        "by_rating": {
            rating: {
                **vals,
                "recall": round(vals["tp"] / (vals["tp"] + vals["fn"]), 4)
                if vals["tp"] + vals["fn"]
                else 0.0,
                "precision": round(vals["tp"] / (vals["tp"] + vals["fp"]), 4)
                if vals["tp"] + vals["fp"]
                else 0.0,
            }
            for rating, vals in sorted(by_rating.items())
        },
        "per_record": per_record,
    }


def markdown(report: dict) -> str:
    dev = report["dev"]
    test = report["test"]
    stats = report["db_stats"]
    lines = [
        "# M1 辞書パイプライン評価レポート",
        "",
        f"- 生成日: {report['generated_at']}",
        f"- DB: `{stats['output']}`（{stats['size_bytes'] / 1024 / 1024:.2f}MB）",
        f"- タグ: {stats['tags']:,} / 別名: {stats['aliases']:,}",
        f"- 日本語別名を持つタグ: {stats['tags_with_japanese_alias']:,} "
        f"（{stats['japanese_coverage'] * 100:.1f}%）",
        f"- 評価セット: `{report.get('eval_path', 'research/eval/eval_200.jsonl')}` "
        f"（dev {dev['records']} / test {test['records']}）",
        "",
        "## 指標",
        "",
        "| 指標 | dev | test（held-out） | 目標 |",
        "|---|---:|---:|---:|",
        f"| Recall | {dev['recall']:.3f} | **{test['recall']:.3f}** | ≥ 0.50 |",
        f"| Precision | {dev['precision']:.3f} | **{test['precision']:.3f}** | ≥ 0.75 |",
        f"| F1 | {dev['f1']:.3f} | **{test['f1']:.3f}** | — |",
        f"| Macro Recall | {dev['macro_recall']:.3f} | {test['macro_recall']:.3f} | — |",
        f"| Macro Precision | {dev['macro_precision']:.3f} | {test['macro_precision']:.3f} | — |",
        f"| Invalid rate | {dev['invalid_rate'] * 100:.3f}% | **{test['invalid_rate'] * 100:.3f}%** | < 2% |",
        f"| 出力 general タグ/件 | {dev['avg_output_general_tags']:.1f} | {test['avg_output_general_tags']:.1f} | — |",
        "",
        "## Go / No-Go",
        "",
    ]
    go = (
        test["recall"] >= 0.50
        and test["precision"] >= 0.75
        and test["invalid_rate"] < 0.02
    )
    lines.append(
        f"**{'GO' if go else 'NO-GO（現時点）'}**: "
        f"Recall {test['recall']:.3f} / Precision {test['precision']:.3f} / "
        f"Invalid {test['invalid_rate'] * 100:.3f}%"
    )
    lines += [
        "",
        "> 現時点の DB は、既存の手作業訳・機械訳・手動キュレーション・色+名詞の合成別名で構築している。",
        "> LLM による別名生成と逆翻訳検証はまだ適用していない。",
        "",
        "## test の主な取りこぼし",
        "",
        "| タグ | 回数 |",
        "|---|---:|",
    ]
    for tag, count in test["top_missed"][:20]:
        lines.append(f"| `{tag}` | {count} |")
    lines += [
        "",
        "## test の主な誤出力",
        "",
        "| タグ | 回数 |",
        "|---|---:|",
    ]
    for tag, count in test["top_false_positives"][:20]:
        lines.append(f"| `{tag}` | {count} |")
    lines += [
        "",
        "## 評価セット",
        "",
        f"- `{report.get('eval_path', 'research/eval/eval_200.jsonl')}`",
        "- 入力: Danbot の `summary_ja`",
        "- 正解: `summary_tags` のうち、DB に存在する general タグ",
        "- dev は別名・ルール調整用、test は最終評価用",
        "",
        "## 注意",
        "",
        "- `summary_tags` はモデル生成の要約タグで、元画像の全タグではない。",
        "- キャラクター名・版権名は general タグ評価から除外している。",
        "- ML Kit 翻訳や LLM 別名は未適用。次段階で追加する。",
        "",
    ]
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--eval", type=Path, default=Path("research/eval/eval_200.jsonl"))
    parser.add_argument("--db", type=Path, default=None)
    parser.add_argument("--json", type=Path, default=Path("research/eval/eval_report.json"))
    parser.add_argument("--markdown", type=Path, default=Path("research/M1_REPORT.md"))
    parser.add_argument("--results", type=Path, default=Path("research/eval/eval_results.jsonl"))
    args = parser.parse_args()

    records = [json.loads(line) for line in args.eval.open(encoding="utf-8")]
    matcher = Matcher(args.db)
    categories = category_map(args.db or db_path())
    dev = [r for r in records if r["split"] == "dev"]
    test = [r for r in records if r["split"] == "test"]
    stats = json.loads(Path("research/m1_stats.json").read_text(encoding="utf-8"))
    report = {
        "generated_at": __import__("datetime").datetime.now(
            __import__("datetime").timezone.utc
        ).isoformat(),
        "eval_path": str(args.eval),
        "db_stats": stats,
        "dev": evaluate(dev, matcher, categories),
        "test": evaluate(test, matcher, categories),
    }
    args.json.parent.mkdir(parents=True, exist_ok=True)
    args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    args.markdown.write_text(markdown(report), encoding="utf-8")
    with args.results.open("w", encoding="utf-8") as f:
        for split in ("dev", "test"):
            for rec in report[split]["per_record"]:
                f.write(json.dumps({"split": split, **rec}, ensure_ascii=False) + "\n")
    print(
        json.dumps(
            {
                "dev": {k: report["dev"][k] for k in ("recall", "precision", "f1", "invalid_rate")},
                "test": {k: report["test"][k] for k in ("recall", "precision", "f1", "invalid_rate")},
                "json": str(args.json),
                "markdown": str(args.markdown),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
