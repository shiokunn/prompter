# M1 辞書パイプライン評価レポート

- 生成日: 2026-10-23T13:05:25.831646+00:00
- DB: `/data/data/com.termux/files/home/prompter/app/src/main/assets/promptdb.sqlite`（2.18MB）
- タグ: 5,003 / 別名: 14,587
- 日本語別名を持つタグ: 3,857 （77.1%）
- 評価セット: `research/eval/eval_final.jsonl` （dev 50 / test 100）

## 指標

| 指標 | dev | test（held-out） | 目標 |
|---|---:|---:|---:|
| Recall | 0.460 | **0.519** | ≥ 0.50 |
| Precision | 0.715 | **0.762** | ≥ 0.75 |
| F1 | 0.560 | **0.618** | — |
| Macro Recall | 0.440 | 0.515 | — |
| Macro Precision | 0.705 | 0.776 | — |
| Invalid rate | 0.000% | **0.000%** | < 2% |
| 出力 general タグ/件 | 10.3 | 12.8 | — |

## Go / No-Go

**GO**: Recall 0.519 / Precision 0.762 / Invalid 0.000%

> 現時点の DB は、既存の手作業訳・機械訳・手動キュレーション・色+名詞の合成別名で構築している。
> LLM による別名生成と逆翻訳検証はまだ適用していない。

## test の主な取りこぼし

| タグ | 回数 |
|---|---:|
| `jewelry` | 17 |
| `holding` | 13 |
| `long_hair` | 11 |
| `flower` | 11 |
| `animal_ears` | 11 |
| `solo` | 10 |
| `1girl` | 10 |
| `looking_at_viewer` | 9 |
| `rose` | 8 |
| `horse_girl` | 8 |
| `serafuku` | 7 |
| `2girls` | 7 |
| `multiple_girls` | 7 |
| `hair_over_one_eye` | 7 |
| `simple_background` | 7 |
| `male_focus` | 7 |
| `sleeveless` | 6 |
| `shorts` | 6 |
| `fruit` | 6 |
| `jacket` | 6 |

## test の主な誤出力

| タグ | 回数 |
|---|---:|
| `standing` | 23 |
| `solo` | 12 |
| `holding` | 11 |
| `ribbon` | 11 |
| `necktie` | 10 |
| `1girl` | 9 |
| `bare_arms` | 7 |
| `necklace` | 5 |
| `showering` | 5 |
| `back` | 4 |
| `tiles` | 4 |
| `blue_necktie` | 4 |
| `bow` | 4 |
| `lying` | 4 |
| `star_(symbol)` | 4 |
| `english_text` | 3 |
| `feet` | 3 |
| `looking_at_viewer` | 3 |
| `breasts` | 3 |
| `on_chair` | 3 |

## 評価セット

- `research/eval/eval_final.jsonl`
- 入力: Danbot の `summary_ja`
- 正解: `summary_tags` のうち、DB に存在する general タグ
- dev は別名・ルール調整用、test は最終評価用

## 注意

- `summary_tags` はモデル生成の要約タグで、元画像の全タグではない。
- キャラクター名・版権名は general タグ評価から除外している。
- ML Kit 翻訳や LLM 別名は未適用。次段階で追加する。
