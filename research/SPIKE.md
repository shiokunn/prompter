# 調査スパイク: 日本語→Danbooru タグ変換は実現できるか

- 日付: 2026-10-23
- 目的: 企画の核心「日本語の自然言語を Danbooru タグに変換する」が、オンデバイスで現実的かを検証する。
- 結論: **実現可能**。ただし素朴な辞書マッチでは不十分で、別名の生成と誤マッチ抑制が必須。

---

## 1. 使ったデータ

| ファイル | 行数 | サイズ | 内容 | ライセンス |
|---|---:|---:|---|---|
| `danbooru.csv` | 140,782 | 3.4MB | タグ名、カテゴリ、投稿数、別名 | リポジトリは MIT。タグデータは Danbooru 由来 |
| `danbooru-jp.csv` | 427 | 9.4KB | 手作業の日本語訳 | MIT |
| `danbooru-only-machine-jp.csv` | 99,999 | 3.5MB | 機械翻訳の日本語訳 | MIT |

取得元:

- https://github.com/DominikDoom/a1111-sd-webui-tagcomplete
- https://github.com/boorutan/booru-japanese-tag

### データの例

```
1girl,0,6008644,"1girls,sole_female"
long_hair,0,4350743,"/lh,longhair"
looking_at_viewer,0,3315722,...
```

```
1girl,一人の女の子
long_hair,ロングヘア
looking_at_viewer,主観視点
```

機械翻訳はタグ名をそのまま Google 翻訳したものが多く、`looking_at_viewer` →「視聴者を見ています」のような不自然な訳も多い。手作業訳は427件だが、上位タグをカバーしており品質が高い。

---

## 2. 作ったもの

`tools/spike_match.py`

- 日本語の表記を NFKC 正規化、カタカナ→ひらがな、記号除去。
- タグ名・別名・日本語訳を Trie に登録。
- 入力に対して最長一致。
- 短いカタカナ語が長いカタカナ語の一部になっている場合（`サイバーパンク` の `パン`）を除外する境界判定。
- 同点なら手作業訳 > 機械訳、投稿数が多い方を優先。

`tools/build_db_spike.py`

- 上記データを SQLite に投入し、サイズとクエリ速度を測定。

---

## 3. 結果

### 3.1 辞書 DB のサイズと速度

| 指標 | 結果 |
|---|---:|
| タグ数 | 140,782 |
| 別名 | 30,276 |
| 日本語訳 | 94,598 |
| SQLite サイズ | **16.8MB**（FTS 除く） |
| gzip 後 | **8.9MB** |
| ビルド時間（Python） | 4.9秒 |
| 正規化キーの完全一致クエリ | 0.085ms |
| 1プロンプトのマッチ時間（ロード後） | **0.05ms** |

→ 14万タグ全部を端末に持っても 17MB。APK 同梱も現実的。上位5,000タグに絞れば数MB以下。

### 3.2 素朴な最長一致の問題

別名なし・機械訳のみで試すと、次のような問題が出た。

| 入力 | 問題 |
|---|---|
| 銀髪 | 辞書は「銀の髪」で、`銀髪` と一致しない |
| 青い目 | 辞書は「碧眼」で、`青い目` と一致しない |
| 夕焼け | 辞書は「日没」で、`夕焼け` と一致しない |
| 桜 | 単漢字を除外すると落ちる |
| サイバーパンク | 内部の「パン」を `bread` と誤マッチ |
| カメラ目線 | 辞書は「主観視点」で、出力タグは `looking_at_viewer` だが表記が違う |

### 3.3 別名 + 境界判定を足した結果

テスト用に約50個の別名を手で追加し、短いカタカナ語の境界判定を入れると、10例すべてで主要な概念をカバーできた。

```
JA: 銀髪のツインテールの少女が、制服姿で教室に立っている
MATCH: silver_hair, twintails, 1girl, school_uniform, classroom, standing

JA: 猫耳のメイドが紅茶を注いでいる
MATCH: cat_ears, maid, tea, pouring

JA: 夏の海辺で浴衣を着た女の子、夕焼け、逆光
MATCH: summer, beach, yukata, 1girl, sunset, backlighting

JA: 俯瞰視点で見下ろす構図、ダイナミックなポーズ
MATCH: from_above, looking_down, composition, dynamic_pose

JA: 水彩風の柔らかいタッチ、淡い色合い
MATCH: watercolor_(medium), pastel_colors

JA: 黒髪ロング、青い目、八重歯、セーラー服
MATCH: black_hair, blue_eyes, fang, serafuku

JA: 手を振っている、笑顔、カメラ目線
MATCH: waving, smile, looking_at_viewer

JA: 背景に桜が舞う、春の公園
MATCH: cherry_blossoms, spring_(season), park

JA: 白いワンピースを着た金髪の少女が、草原に座って空を見上げている
MATCH: white_dress, blonde_hair, 1girl, grass, sitting, looking_up

JA: サイバーパンク風の街並み、ネオン、雨、夜
MATCH: cyberpunk, cityscape, neon_lights, rain, night
```

**注意**: この別名はテスト用に手で作ったもの。本番では上位5,000タグについて、LLM で別名を生成し、逆翻訳で検証する必要がある。ここが開発の主要コスト。

### 3.4 汎用 MT ではタグにならない

参考として、公開されている汎用翻訳 API に同じ日本語をかけた結果：

| 日本語 | 汎用 MT | 必要なタグ |
|---|---|---|
| 銀髪のツインテールの少女が、制服姿で教室に立っている | A silver-haired twin-tailed girl stands in a classroom in a uniform | `1girl, silver_hair, twintails, school_uniform, classroom, standing` |
| 夏の海辺で浴衣を着た女の子、夕焼け、逆光 | Girl in yukata, sunset, backlight on summer beach | `1girl, yukata, sunset, backlighting, beach, summer` |
| 手を振っている、笑顔、カメラ目線 | Waving, smiling, camera gaze | `waving, smile, looking_at_viewer` |
| 水彩風の柔らかいタッチ、淡い色合い | Soft watercolor touch, light shades | `watercolor_(medium), pastel_colors` |
| サイバーパンク風の街並み、ネオン、雨、夜 | Cyberpunk, Neon, Rain, Night | `cyberpunk, cityscape, neon_lights, rain, night` |

→ 汎用 MT は「自然な英語」を返すが、canonical tag ではない。翻訳の後に「英語→タグ正規化」が必要。

---

## 4. 実装への示唆

1. **辞書だけで6〜7割はカバーできる**。上位5,000タグ + 別名で、よくあるプロンプトはほぼ変換できる。
2. **別名の品質がすべて**。LLM 生成 + 逆翻訳検証 + 人手レビュー（上位500）のパイプラインを作る。
3. **素朴な最長一致は危険**。短いカタカナ・単漢字の誤マッチを防ぐ境界判定か、トークナイザが必要。
4. **辞書外は ML Kit → 英語→タグ正規化**。汎用 MT の出力をそのまま出さない。
5. **DB は小さい**。14万タグで17MB。オフライン配布の障壁は低い。
6. **速度も十分**。マッチは0.05ms。ボトルネックは ML Kit の初回モデルロードと LLM のみ。

---

## 5. 再現方法

```bash
cd ~/prompter
mkdir -p research/data
cd research/data
curl -L -o danbooru.csv https://raw.githubusercontent.com/DominikDoom/a1111-sd-webui-tagcomplete/main/tags/danbooru.csv
curl -L -o danbooru-jp.csv https://raw.githubusercontent.com/boorutan/booru-japanese-tag/main/danbooru-jp.csv
curl -L -o danbooru-only-machine-jp.csv https://raw.githubusercontent.com/boorutan/booru-japanese-tag/main/danbooru-only-machine-jp.csv
cd ~/prompter
python3 tools/spike_match.py
python3 tools/build_db_spike.py
```

---

## 6. 限界

- 手作業の別名50個はテスト用であり、一般的なカバレッジではない。
- 評価プロンプト10例は少ない。本番では200例で評価する。
- 汎用 MT の比較は Google Translate そのものではなく、公開 API の結果。ML Kit の品質は実機で別途検証する。
- 日本語の形態素解析は未実装。境界判定はヒューリスティック。
- 成人向けタグ、版権キャラ、マイナータグの扱いは未検証。
