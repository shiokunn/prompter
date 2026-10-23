# M1 辞書パイプライン評価レポート

- 生成日: 2026-10-23
- DB: `app/src/main/assets/promptdb.sqlite`（2.18MB）
- タグ: 5,003 / 別名: 14,587
- 日本語別名を持つタグ: 3,857（77.1%）
- 評価データ: `dartags/danbooru-2408-blind-captions` の `summary_ja` + `summary_tags`

## Go / No-Go

**GO**（fresh final holdout、test 100件）

| 指標 | test | 目標 |
|---|---:|---:|
| Recall | **0.5194** | ≥ 0.50 |
| Precision | **0.7617** | ≥ 0.75 |
| Invalid rate | **0.000%** | < 2% |

最終評価は、チューニングに使った 200 件とは別に取得した
`research/eval/eval_final.jsonl` の test 100 件で実施した。
学習サンプル・旧評価セットとは source ID / 入力文が重複していないことを確認済み。

## 参考: チューニング用 200 件

`research/eval/eval_200.jsonl` の test 100 件では Recall 0.5521 /
Precision 0.8060 / Invalid 0.000%。ただし、この test はエラー分析に
一度使っているため、Go 判定には使わない。

## 主な改善

- 単漢字別名の境界判定（`水` が `水銀燈` 内で誤爆する問題など）
- かな別名の助詞境界判定（`ドラゴン` が `ペンドラゴン` 内で誤爆する問題など）
- 正規タグ・公式別名を `lang=en` 扱いにし、日本語 trie から除外
- 色付き服飾タグから汎用親タグを展開（`blue_dress` → `dress` など）
- `1girl` / `1boy` / `solo` / `2girls` / `multiple_girls` / `hetero` / `yuri` の保守的推論
- 複数人表現（`二人`, `両者`, `もう一人`, `それぞれ`）の検出
- `マント` → `cape`、`双髪` → `twin_braids`、`和紙傘` → `oil-paper_umbrella` など
  日本語→Danbooru タグの対応修正
- `school`, `the_pose`, `camera` などの汎用タグで誤爆しやすい機械別名を除外

## 既知の限界

- `summary_tags` はモデル生成の要約タグであり、元画像の全タグではない。
- キャラクター名・版権名は general タグ評価から除外している。
- LLM による別名生成・逆翻訳検証は未適用。
- ML Kit Translation / ローカル LLM はまだ接続していない。
- fresh holdout の Precision は 0.762 で、目標に対して余裕が小さい。
- fresh holdout の dev 50 件は Recall 0.460 / Precision 0.715 で、
  小標本のため test より低い。

## ファイル

- `research/M1_FINAL_REPORT.md` — fresh holdout の詳細
- `research/M1_TUNING_REPORT.md` — チューニング用 200 件の詳細
- `research/eval/eval_final_report.json` — fresh holdout の機械可読レポート
- `research/eval/eval_final_results.jsonl` — 行別の予測・誤り
- `research/eval/eval_final.jsonl` — fresh holdout 本体

## 次の段階

M1 は完了。M2 で Compose UI、Room、Trie 検索、オフラインの PixAI プロンプト
合成を実装する。LLM 別名生成は OpenRouter キーが用意できた時点で追加する。
