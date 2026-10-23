# M1 辞書パイプライン評価レポート

- 生成日: 2026-10-23T13:05:25.394922+00:00
- DB: `/data/data/com.termux/files/home/prompter/app/src/main/assets/promptdb.sqlite`（2.18MB）
- タグ: 5,003 / 別名: 14,587
- 日本語別名を持つタグ: 3,857 （77.1%）
- 評価セット: `research/eval/eval_200.jsonl` （dev 100 / test 100）

## 指標

| 指標 | dev | test（held-out） | 目標 |
|---|---:|---:|---:|
| Recall | 0.527 | **0.552** | ≥ 0.50 |
| Precision | 0.783 | **0.806** | ≥ 0.75 |
| F1 | 0.630 | **0.655** | — |
| Macro Recall | 0.528 | 0.537 | — |
| Macro Precision | 0.792 | 0.812 | — |
| Invalid rate | 0.000% | **0.000%** | < 2% |
| 出力 general タグ/件 | 10.8 | 11.2 | — |

## Go / No-Go

**GO**: Recall 0.552 / Precision 0.806 / Invalid 0.000%

> 現時点の DB は、既存の手作業訳・機械訳・手動キュレーション・色+名詞の合成別名で構築している。
> LLM による別名生成と逆翻訳検証はまだ適用していない。

## test の主な取りこぼし

| タグ | 回数 |
|---|---:|
| `looking_at_viewer` | 10 |
| `1boy` | 9 |
| `large_breasts` | 9 |
| `pussy` | 9 |
| `1girl` | 7 |
| `solo` | 7 |
| `short_hair` | 7 |
| `2girls` | 7 |
| `holding` | 7 |
| `penis` | 7 |
| `multiple_girls` | 6 |
| `long_hair` | 6 |
| `censored` | 5 |
| `on_back` | 5 |
| `nipples` | 5 |
| `breasts` | 5 |
| `hetero` | 5 |
| `:d` | 5 |
| `loli` | 5 |
| `ass` | 4 |

## test の主な誤出力

| タグ | 回数 |
|---|---:|
| `standing` | 12 |
| `1girl` | 9 |
| `holding` | 9 |
| `ribbon` | 6 |
| `solo` | 6 |
| `lying` | 6 |
| `nipples` | 5 |
| `back` | 5 |
| `smile` | 5 |
| `on_bed` | 5 |
| `breasts` | 4 |
| `bow` | 4 |
| `black_eyes` | 4 |
| `bare_arms` | 4 |
| `hug` | 3 |
| `yellow_eyes` | 3 |
| `pantyhose` | 3 |
| `hetero` | 3 |
| `purple_eyes` | 2 |
| `on_head` | 2 |

## 評価セット

- `research/eval/eval_200.jsonl`
- 入力: Danbot の `summary_ja`
- 正解: `summary_tags` のうち、DB に存在する general タグ
- dev は別名・ルール調整用、test は最終評価用

## 注意

- `summary_tags` はモデル生成の要約タグで、元画像の全タグではない。
- キャラクター名・版権名は general タグ評価から除外している。
- ML Kit 翻訳や LLM 別名は未適用。次段階で追加する。
