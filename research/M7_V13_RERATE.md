# M7 v13 / v13.1 人手再採点キット（同一8ケース）

- 日付: 2026-10-28
- 目的: v13/v13.1の忠実さ・タグ整合性の修正が、v9の人手評価（忠実さ 2.88/5）を実際に上回ったかを、**同じ8ケース**で確認する
- 現行データ: v13.1（`0.7.5-m7`、付録A）。v13の公式結果は §9 に保存
- 判定の主軸: 入力への忠実さ **3.5 / 5 以上**、かつ重大な矛盾ゼロ
- このファイル1つで採点が完結する（付録Aに評価対象の全文を収録）

---

## 0. 前提（固定条件）

| 項目 | 値 |
|---|---|
| APK | `0.7.5-m7`（versionCode 20、debug、arm64-v8a） |
| モデル | Gemma 4 E2B IT（LiteRT-LM、CPU 4 threads） |
| 生成設定 | temperature 0.3、seed 42 |
| プロンプト | `local:gemma-4-E2B-v13`（11〜12文、210〜240語、各文18語以上。v13.1でも変更なし） |
| 辞書 | 5,006タグ / 別名14,618件（v13.1で補完タグ3件と別名17件を追加） |
| 端末 | M08 / Android 16 / 12GB RAM |
| ケース | 20, 21, 22, 24, 25, 26, 27, 29（v9と同一） |
| 対象テキスト | 付録A（履歴ID 148/149/150/151/152/153/154/155 の保存済み出力） |
| 生データ | [`research/eval/m7_v131_8cases.json`](eval/m7_v131_8cases.json) |
| v9ベースライン | [`research/PIXAI_EVAL.md`](PIXAI_EVAL.md)（忠実さ 2.88、総合 3.63） |

**重要**: 評価するのは付録Aの固定テキスト。アプリで再生成すると sampling により別の文になるため、
再現性のためにも再実行はしない。再実行した場合は別評価として記録する。

---

## 1. 採点の流れ（6ステップ）

1. **付録Bを開かない。** 先に読むと判定が誘導される。付録Bは採点後の照合用。
2. 各ケースを **入力 → 出力** の順に読む。出力だけを先に読まない。
3. 4軸（意図・自然さ・構造・忠実さ）を **1〜5の整数**で採点する。
4. タグ整合性を **別枠**で監査する（4軸の平均には入れない。v9との比較を壊さないため）。
5. 重大違反フラグ（後述）を付ける。
6. 8ケース終了後に集計し、**そのあとで** v9のスコアと付録Bを見る。

- 1ケースあたり2〜3分。8ケースを一度にやらず、4ケース×2バッチに分けてよい。
- 採点は整数のみ。0.5刻みは使わない（v9と同じ粒度で比較するため）。
- 出力を書き直さない・補わない。**書かれている内容だけ**を評価する。
- 全ケースを同じ点に寄せない。各ケースは独立に判定する。

---

## 2. 評価軸（4軸 + 別枠1）

| 軸 | 何を見るか | 配点 |
|---|---|---:|
| 意図の保持 | 入力の狙い（主題・雰囲気・構図）がPixAI向けの絵として伝わるか | 1–5 |
| 英語の自然さ | 英文として自然か。定型句の反復、不自然な語順、意味の破綻 | 1–5 |
| 構造 | 主題→動作→情景→光→画風の順序と過不足 | 1–5 |
| 入力への忠実さ | 指定要素の保持と、指定外要素の追加・矛盾 | 1–5 |
| タグ整合性（別枠） | タグが入力・自然文と一致するか（矛盾／欠落／過剰） | 1–5 |

**総合**は4軸の平均を参考にしつつ、「このプロンプトをPixAIに渡したときに意図通りになるか」で整数評価する。
平均から±1の範囲で調整してよいが、理由を1行残す。

---

## 3. 5段階の基準

### 3.1 軸ごとのアンカー

| 点 | 意図の保持 | 英語の自然さ | 構造 | 入力への忠実さ |
|---:|---|---|---|---|
| 5 | 狙いを完全に捉え、余計な解釈がない | 母語話者として自然。定型句の反復が目立たない | 5要素が過不足なく、順序立っている | 指定要素を全て保持。追加は許容範囲のみ |
| 4 | ほぼ同じ。焦点がわずかにずれる | 自然だがやや定型的 | 1要素が薄い | 軽微な追加または軽微な欠落が1つ |
| 3 | 主題は伝わるが、構図か雰囲気が変質 | 通じるが不自然な箇所が1〜2 | 要素が欠ける、または順序が乱れる | 重大1件、または軽微が複数 |
| 2 | 主題の一部しか伝わらない | 機械的で読みにくい | 列挙的で流れがない | 重大が複数 |
| 1 | 意図が読み取れない | 破綻している | 構造がない | 入力と矛盾する |

### 3.2 忠実さの要素分類（この評価の中心）

**重大（生成結果を壊す／設定の改変）**

- 人数: 1人↔2人以上、分割構図での人数解釈の誤り
- 髪: 指定と異なる髪色・長さ・型。**未指定なのに色・長さ・型を特定する**（「髪」とだけ書くのは可）
- 目: 指定と異なる目色。未指定なのに色を特定する（「視線」「瞳」とだけ書くのは可）
- 服: 指定と異なる種類・色。未指定なのに服の種類や色を断定
- 露出: 「胸元を隠す」「露出が少ない」に対する `cleavage` や露出描写
- 人物の追加: 入力にいない人物、別人格の創作
- メタ説明: "not specified" など、指定がないことを本文で説明する

**軽微（減点するが上限規則の対象外）**

- 具体的な家具・小道具・料理工程・部屋の装飾の追加（入力に無い）
- 入力に無いカメラ用語（`from_below` など）の付与
- 表情の微細化（入力の感情と矛盾しない範囲）
- 同じ内容の言い換え・重複

**許容（減点しない）**

- 光・影・色調・質感・空気感の補完
- 入力に構図指定がない場合の、無難な視点の補完
- 英語として必要な冠詞・代名詞・接続

※ 自然文とタグの片方にしか無い要素は、忠実さでは減点せず §4 のタグ監査で扱う。

### 3.3 忠実さの上限規則（判定のばらつき防止）

- 重大が **1件** → 忠実さは **最大3**
- 重大が **2件以上** → 忠実さは **最大2**
- 軽微のみ **4件以上** → 忠実さは **最大3**
- 上記に当てはまらなければ 4〜5

迷ったら **低い方** に倒す。理由は必ず1行で書く（例: 「ID25: 入力に無い from_below を付与（軽微）」）。

---

## 4. タグ整合性の監査（別枠）

自然文とは独立に、タグだけを見て次を数える。

| 区分 | 定義 | 例 |
|---|---|---|
| 矛盾 | タグが入力または自然文と逆のことを言う | 胸元を隠す入力に `cleavage`、1人入力に `2girls` |
| 欠落 | 入力で明示された要素のタグが無い | トレーとジュースが入力にあるのにタグが `holding` のみ |
| 過剰 | 入力に無い要素のタグがある | 未指定の `school_uniform`、未指定色の `white_dress` |

| 点 | 基準 |
|---:|---|
| 5 | 矛盾・欠落・過剰が実質ゼロ |
| 4 | 軽微な欠落が1つ（小物・料理名など） |
| 3 | 矛盾1つ、または欠落が2つ以上 |
| 2 | 矛盾が2つ以上 |
| 1 | 人数・露出など、生成を破壊する矛盾がある |

- タグは `1girl, solo` のような重複を許容する（減点しない）。
- 欠落は「入力に明記され、自然文にも書かれているのにタグに無い」場合に数える。
  自然文にもタグにも無い場合は、忠実さ側の欠落として扱う（二重計上しない）。
- **重大なタグ矛盾**（人数・露出・未指定の服色）は、忠実さの重大違反としても数える。

---

## 5. 記録フォーマット

### 5.1 4軸（必須）

| ID | 意図 | 自然さ | 構造 | 忠実さ | 総合 | 重大 | 一言コメント |
|---|---:|---:|---:|---:|---:|---|---|
| 20 | | | | | | | |
| 21 | | | | | | | |
| 22 | | | | | | | |
| 24 | | | | | | | |
| 25 | | | | | | | |
| 26 | | | | | | | |
| 27 | | | | | | | |
| 29 | | | | | | | |
| **合計** | /40 | /40 | /40 | /40 | /40 | 件 | |
| **平均** | | | | | | | |

### 5.2 タグ監査（別枠）

| ID | 矛盾 | 欠落 | 過剰 | タグ点 | 具体例 |
|---|---|---:|---:|---:|---|
| 20 | | | | | |
| 21 | | | | | |
| 22 | | | | | |
| 24 | | | | | |
| 25 | | | | | |
| 26 | | | | | |
| 27 | | | | | |
| 29 | | | | | |
| **平均** | | | | /5 | |

### 5.3 重大違反フラグ

ケースごとにこのブロックをコピーし、該当するものを列挙する（無ければ「なし」）。

- [ ] 人数の誤り
- [ ] 髪色・髪型・髪の長さの改変／未指定の断定
- [ ] 目色の改変／未指定の断定
- [ ] 服の種類・色の改変／未指定の断定
- [ ] 露出指示への違反
- [ ] 人物の追加
- [ ] メタ説明（"not specified" など）

---

## 6. 合否判定

### 6.1 必須条件（すべて満たすこと）

| # | 条件 | しきい値 |
|---:|---|---|
| 1 | 忠実さ平均 | **3.5以上**（合計28/40以上） |
| 2 | 総合平均 | 3.5以上 |
| 3 | 意図・自然さ・構造の各平均 | いずれも3.5以上（v9からの回帰なし） |
| 4 | 重大な忠実さ違反 | **0件** |
| 5 | タグ整合性平均 | 4.0以上、かつ重大なタグ矛盾0件 |
| 6 | 語数 | 8/8が180〜260語 |
| 7 | 拒否・日本語混入 | 0/8、0/8 |

条件6・7は付録Aの実測値で既に満たしている（平均230.4語、8/8が範囲内、拒否0、日本語混入0）。
採点者は条件1〜5に集中してよい。

### 6.2 判定

| 判定 | 条件 | 次の行動 |
|---|---|---|
| **合格** | 6.1を全て満たす | v13をM7のローカル標準として確定。残課題は次マイルストーンへ |
| **条件付き合格** | 忠実さ3.25〜3.375（合計26〜27/40）、またはタグ平均3.5〜3.875。重大0 | 該当ケースだけ修正し、そのケースのみ再評価 |
| **不合格** | 忠実さ3.125以下（合計25/40以下）、または重大1件以上 | v14で忠実さルールを再調整。同じ8ケースで再々評価 |

- 平均だけでなく **どのケースで落としたか** を必ず残す。1ケースの重大が平均を押し上げていても不合格になり得る。
- v9との比較は、採点完了後に付録Cで行う。

---

## 7. 評価の禁止事項

- v9のスコアやケース別コメントを、採点前に見る
- 出力を自分で書き直して「こう書くべき」で採点する
- 入力に無い要素を「雰囲気づくり」として見逃さない。逆に、入力にある要素を「常識的な補完」として減点しない
- 8ケースを読んだ後の印象で、全ケースを同じ点にする
- タグ整合性を4軸の平均に混ぜる（v9と比較できなくなる）
- 画像生成結果の良し悪しを推測して点数に入れる（今回はテキストのみの評価）

---

## 8. 評価用プロンプト

### 8.1 正式版（人手評価者に渡す）

以下をそのままコピーして渡す。付録Aも一緒に渡すこと。

```text
あなたは日本語→英語プロンプト変換アプリの評価者です。
添付の「付録A」にある8ケースについて、各ケースの入力（日本語）と出力（英語の自然文＋タグ）を読み、
以下の基準で採点してください。最終判定は人手で行うため、遠慮のない評価をお願いします。

【評価対象】
- 8ケース: 20, 21, 22, 24, 25, 26, 27, 29
- 付録Aの固定テキストを評価する。付録B（ケース別チェックポイント）は採点が終わるまで見ない。
- 出力は修正しない。書かれている内容だけを評価する。
- 採点は1〜5の整数のみ（0.5刻みは不可）。

【採点する4軸】
1. 意図の保持: 入力の狙い（主題・雰囲気・構図）が、PixAIに渡すプロンプトとして伝わるか。
   5=完全 / 4=ほぼ同じ / 3=構図か雰囲気が変質 / 2=主題の一部のみ / 1=意図不明
2. 英語の自然さ: 英文として自然か。不自然な語順、意味の破綻、定型句の反復を見る。
   5=母語話者として自然 / 4=やや定型的 / 3=不自然な箇所が1〜2 / 2=読みにくい / 1=破綻
3. 構造: 主題→動作→情景→光→画風の順序と過不足。
   5=過不足なし / 4=1要素が薄い / 3=要素の欠落か順序の乱れ / 2=列挙的 / 1=構造なし
4. 入力への忠実さ（最重要）: 指定要素の保持と、指定外要素の追加・矛盾。
   5=全保持・追加は許容範囲 / 4=軽微な追加か欠落が1つ / 3=重大1件か軽微が複数 / 2=重大が複数 / 1=入力と矛盾

【忠実さの判定ルール】
- 重大: 人数の誤り / 髪色・髪型・長さの改変、未指定なのに色・長さ・型を特定する /
  目色の改変、未指定なのに色を特定する / 服の種類・色の改変や未指定の断定 /
  「胸元を隠す」への露出描写 / 人物の追加 / "not specified" などのメタ説明
  （未指定でも「髪」「視線」とだけ書くのは可）
- 軽微: 入力に無い家具・小物・料理工程・装飾の追加 / 入力に無いカメラ用語 /
  表情の微細化（感情と矛盾しない範囲）/ 重複した言い換え
- 許容（減点しない）: 光・影・色調・質感・空気感の補完、構図未指定時の無難な視点、
  英語として必要な冠詞・代名詞
- 上限規則: 重大1件 → 忠実さは最大3。重大2件以上 → 最大2。軽微のみ4件以上 → 最大3。
- 迷ったら低い方に倒し、理由を1行で書く。

【タグ整合性（別枠。4軸の平均には入れない）】
タグだけを見て、次を数える。
- 矛盾: タグが入力・自然文と逆（例: 胸元を隠すのに cleavage、1人なのに 2girls）
- 欠落: 入力で明示され、自然文にも書かれている要素のタグが無い
  （自然文にもタグにも無い場合は、忠実さ側の欠落として数える）
- 過剰: 入力に無い要素のタグ（未指定の服、未指定色など）
5=実質ゼロ / 4=軽微な欠落1つ / 3=矛盾1つか欠落2つ以上 / 2=矛盾2つ以上 / 1=生成を壊す矛盾
`1girl, solo` のような重複は減点しない。

【総合】
4軸の平均を参考に、「PixAIに渡して意図通りになるか」で1〜5の整数。
平均から±1の範囲で調整してよいが、理由を1行残す。

【出力形式】
まず次の表を埋めてください（数値のみ）。

| ID | 意図 | 自然さ | 構造 | 忠実さ | 総合 | 重大(件) |
|---|---:|---:|---:|---:|---:|---:|
| 20 | | | | | | |
| 21 | | | | | | |
| 22 | | | | | | |
| 24 | | | | | | |
| 25 | | | | | | |
| 26 | | | | | | |
| 27 | | | | | | |
| 29 | | | | | | |

次にタグ監査の表。

| ID | 矛盾 | 欠落 | 過剰 | タグ点 | 具体例 |
|---|---|---:|---:|---:|---|
| 20 | | | | | |
| 21 | | | | | |
| 22 | | | | | |
| 24 | | | | | |
| 25 | | | | | |
| 26 | | | | | |
| 27 | | | | | |
| 29 | | | | | |

最後に、各ケース1〜2行のコメントを書いてください。書き方は:
「IDxx: 保持できた点 / 失った点 / 追加された点 / 最も大きな減点理由」

【注意】
- 出力を書き直さない。提案ではなく、今あるテキストの評価を書く。
- 全ケースを同じ点にしない。ケースごとに独立に判定する。
- 画像の出来を推測して点数に入れない。テキストのみを評価する。
```

### 8.2 補助版（LLMに差分チェックさせる用・正式判定には使わない）

人手採点の前に、見落としを減らすための下書き。**点数は出させない。**

```text
以下の8ケースについて、入力と出力を比較し、差分だけを列挙してください。採点はしないでください。

各ケースごとに、次の4分類で箇条書きにしてください。
1. 保持: 入力で明示され、出力（自然文またはタグ）に正しく反映されている要素
2. 欠落: 入力で明示されているが、出力に無い要素
3. 追加: 入力に無いのに出力に含まれる要素（身体的特徴・服・人数・小物・工程・カメラ用語）
4. 矛盾: 入力と逆のことを言っている箇所（人数、露出、色、髪型など）

ルール:
- 「おそらく」「暗黙に」といった推測で補わない。テキストに書かれていることだけを根拠にする。
- 自然文とタグは分けて書き、両方が一致しているかも見る。
- 出力を書き直さない。修正案も書かない。
- 各項目に重大度（重大/軽微）を付ける。
- 判定や点数は書かない。差分の列挙だけにする。

対象: 20, 21, 22, 24, 25, 26, 27, 29（入力と出力は別添）
```

---

## 9. v13 公式結果（2026-10-28、修正前）

### 9.1 4軸

| ID | 意図 | 自然さ | 構造 | 忠実さ | 総合 | 重大 |
|---|---:|---:|---:|---:|---:|---:|
| 20 | 5 | 4 | 5 | 4 | 4 | 0 |
| 21 | 5 | 4 | 5 | 4 | 4 | 0 |
| 22 | 5 | 4 | 5 | 4 | 4 | 0 |
| 24 | 4 | 4 | 5 | 3 | 4 | **1** |
| 25 | 4 | 4 | 5 | 4 | 4 | 0 |
| 26 | 5 | 4 | 5 | 3 | 4 | **1** |
| 27 | 5 | 5 | 5 | 4 | 5 | 0 |
| 29 | 5 | 5 | 5 | 3 | 4 | 0 |
| **合計** | **38** | **34** | **40** | **29** | **33** | **2** |
| **平均** | **4.75** | **4.25** | **5.00** | **3.625** | **4.125** | — |

### 9.2 タグ監査

| ID | 矛盾 | 欠落 | 過剰 | タグ点 | 主な内容 |
|---|---:|---:|---:|---:|---|
| 20 | 0 | 0 | 0 | 5 | 入力要素をほぼ保持 |
| 21 | 0 | 2+ | 0 | **3** | 水色インカラー、振り向き、衣装の色レイヤー |
| 22 | 0 | 1 | 0 | 4 | `long_hair` 欠落 |
| 24 | 0 | 0 | 0 | 5 | 人数・ベッド・座る・微笑み |
| 25 | 0 | 2+ | 2 | **3** | `omurice`・3分割が欠落、`from_below` / `from_side` が過剰 |
| 26 | 0 | 0 | 0 | 5 | 主要要素を保持 |
| 27 | 0 | 2+ | 0 | **3** | `tray` / `juice` / 壁・分割構図 |
| 29 | 0 | 2+ | 0 | **3** | `omurice`・3分割・工程 |
| **平均** | | | | **3.875** | |

### 9.3 判定

| 条件 | 実測 | 判定 |
|---|---:|---|
| 忠実さ ≥3.5 | 3.625 | ✅ |
| 総合 ≥3.5 | 4.125 | ✅ |
| 意図・自然さ・構造 ≥3.5 | 4.75 / 4.25 / 5.00 | ✅ |
| 重大違反 0 | **2件** | ❌ |
| タグ平均 ≥4.0 | **3.875** | ❌ |
| 語数・拒否・日本語混入 | 230.4語・8/8・0/0 | ✅ |

**最終判定: 不合格（ただし合格線直前）**

- 忠実さは v9 の 2.88 → **3.625**（+0.745）で、目標の改善幅は超えた。意図・自然さ・構造も回帰なし。
- 落としたのは平均点ではなくゲート条件。重大2件（ID24・ID26）と、タグ平均 3.875。
- ID24/26 はどちらも「入力に髪の指定がないのに、肩に落ちる／スタイルされた髪として長さ・型を断定した」もの。
- タグは ID22 `long_hair`、ID21 水色インカラー、ID27 `tray` / `juice`、ID25/29 `omurice`・3分割が落ちた。

### 9.4 v13.1で実施したスコープ（この2点に絞る）

1. **未指定ヘアを具体化しない**
   - `FidelityGuard` に、入力に髪の言及がない場合のヘア長・型・質感表現の除去を追加。
   - 対象は ID24 の "strands gently falling around her shoulders"、ID26 の
     "falling naturally around her shoulders in a soft, unstyled manner"。
2. **入力の明示要素をタグから落とさない**
   - `ロングヘアー` → `long_hair`（既存 alias は `ロングヘア` のみで、長音付きが漏れていた）。
   - `インカラー` → `colored_inner_hair`、`トレー` → `tray`、`トレーを持って` → `holding_tray`。
   - `オムライス` → `omurice`、`ジュース` → `juice`、`3分割` / `縦半分` → `split_screen`。
   - `omurice` / `juice` / `split_screen` は上位5,003件の外にあるため、辞書の補完タグに追加する。
3. **未指定カメラタグの除去**
   - 入力に構図・アングルの言及がない場合、`from_below` / `from_side` / `from_above` を除去（ID25）。
4. 29の具体的な料理工程は今回は修正しない（忠実さ3でも平均は3.5を超えるため）。プロンプト側の改善はv15以降。

> 9.1〜9.3 は **v13（0.7.4-m7）** の出力に対する結果。v13.1の修正内容と再評価用データは §10 を参照。

---

## 10. v13.1（0.7.5-m7）の修正と再評価データ

v13の不合格を受けて、プロンプトは変えず、決定論的なガードと辞書だけを修正した。

### 10.1 修正内容

| 種別 | 変更 | 対象 |
|---|---|---|
| ガード | 入力に髪の言及がない場合、髪の長さ・型・質感の断定を除去 | ID24・ID26 |
| ガード | 入力に構図・アングルの言及がない場合、`from_below` / `from_side` / `from_above` を除去 | ID25 |
| ガード | 色名がある入力で `greyscale` / `monochrome` を除去 | ID21 |
| ガード | 入力由来の `tray` / `juice` / `omurice` / `split_screen` などをAIが落とした場合に復元 | ID25・ID27・ID29 |
| 辞書 | `ロングヘアー` → `long_hair`（長音付きの別名漏れ） | ID22 |
| 辞書 | `インカラー` → `colored_inner_hair` | ID21 |
| 辞書 | `フリルつき` → `frills`、`振り向いている` → `looking_back` | ID21 |
| 辞書 | `トレー` → `tray`、`トレーを持って` → `holding_tray`、`ジュース` → `juice` | ID27 |
| 辞書 | `オムライス` → `omurice`、`3分割` / `縦半分` → `split_screen` | ID25・ID27・ID29 |
| 辞書 | 補完タグ `omurice` / `juice` / `split_screen` を追加（5,003→5,006） | ID25・ID27・ID29 |

- プロンプト（v13）とtemperature/seedは変更していない。AIキャッシュが効くケースは出力が同一で、ガードだけが再適用される。
- 29の「野菜を切る」「卵を焼く」など具体的な料理工程は残した（忠実さ3でも平均3.5は超えるため）。次回プロンプト改善の候補。

### 10.2 再評価データ（8ケース）

| ID | 語数 | 主な差分 |
|---|---:|---|
| 20 | 232 | 変更なし（キャッシュ） |
| 21 | 195 | `colored_inner_hair` / `frills` / `looking_back` を追加、`greyscale` を除去 |
| 22 | 206 | `long_hair` を復元 |
| 24 | 216 | 髪の長さ・型の断定を除去（「Her hair frames her face」） |
| 25 | 214 | `from_below` / `from_side` を除去、`omurice` / `split_screen` を追加 |
| 26 | 227 | 髪の長さ・型の断定を除去（「Her hair is depicted simply, in keeping with ...」） |
| 27 | 230 | `tray` / `juice` / `split_screen` を追加 |
| 29 | 235 | `omurice` / `split_screen` を追加（本文の工程追加は残る） |

- 平均 **219.4語**、180〜260語 **8/8**、最小195語、最大235語。
- 全8ケースがAIキャッシュ利用のため生成時間は約3.1〜3.2秒。初回生成時は35〜40秒。
- 未指定ヘアの重大違反は **0件** の見込み。タグの主要欠落（`long_hair` / インカラー / `tray` / `juice` / `omurice` / `split_screen`）は解消。
- 正式判定には、この付録Aの8ケースを §1〜§8 の同じ基準で人手再採点する。

### 10.3 再採点で特に見る点

1. ID24・ID26 に、髪の長さ・型・質感の断定が残っていないか（重大0の確認）。
2. ID21 の `greyscale` が消え、水色インカラーと矛盾しないか。
3. ID25 の `from_below` / `from_side` が消えているか。
4. ID27 の `tray` / `juice` がタグと自然文の両方にあるか。
5. ID25・29 の `omurice` / `split_screen` が入っているか。
6. ID29 の具体的な料理工程は、軽微な追加として許容範囲か（忠実さ3のままでも平均は3.5を超える）。

---

## 付録A: 評価対象の全文（v13.1 / 0.7.5-m7）

各ケースは「入力（日本語）」「タグ」「出力（英語自然文）」の順。語数は出力の単語数。
v13（0.7.4-m7、履歴ID 123〜131）の結果は §9 に、v13.1 の変更点は §10 に記載する。

### A.0 一覧

| ID | 履歴ID | 入力の要点 | 語数 |
|---|---|---|---|
| 20 | 148 | 長い黒髪、青いドレス、椅子に座る | 232 |
| 21 | 149 | 黒髪ボブ、水色インカラー、黒フリル+白レース、上半身、黒目、左から、上目遣い | 195 |
| 22 | 150 | 明るい黄色の髪、紫の目、ロングヘア | 206 |
| 24 | 151 | 二人の少女、ベッド、座る、軽い微笑み | 216 |
| 25 | 152 | 白髪赤目、ニケのシンデレラ、メイド服、オムライス、胸元を隠す、3分割、ロング | 214 |
| 26 | 153 | 白いドレス、野原、立つ、こちらを見る | 227 |
| 27 | 154 | 縦半分、壁、右=学生服/白ボブ、左=メイド/トレー/ジュース | 230 |
| 29 | 155 | 白髪赤目、ニケのシンデレラ、メイド服、オムライス、3分割、料理工程 | 235 |

---

### A-20 ケース20（履歴ID 148、232語）

**入力**

```text
長い黒髪の少女が青いドレスを着て、椅子に座っている。。。。。。。。。。。。。。。。。。。。
```

**タグ**

```text
1girl, solo, long_hair, black_hair, blue_dress, sitting, on_chair, chair, dress
```

**出力（自然文）**

```text
A solitary young girl with long black hair is elegantly seated upon a simple chair in a quiet, dimly lit interior setting. Her long black hair cascades down her shoulders, exhibiting a smooth texture that catches the ambient light subtly around her face. She is wearing a flowing blue dress that drapes softly around her body, emphasizing the gentle folds of the fabric as she remains still in her posture. Her expression is serene and contemplative, conveying a sense of quiet solitude within the dimly perceived atmosphere. Her eyes gaze softly toward some unseen point in the distance, reflecting the subdued lighting of the room around her. The fabric of her dress shows subtle variations in tone where the light catches the material, highlighting the texture of the material itself. Her posture is relaxed yet poised, with her hands resting gently in her lap, demonstrating a calm and introspective demeanor. The background consists of a plain, indistinct setting, offering no distracting elements that draw attention away from the central figure. Soft, diffused light streams in from an unseen source, casting gentle shadows that define the contours of her silhouette. The camera adopts a medium shot, framing the girl from the waist up to capture the details of her attire and expression effectively. The overall art style is a detailed anime rendering, emphasizing the delicate lines and the rich shading of the scene.
```

### A-21 ケース21（履歴ID 149、195語）

**入力**

```text
黒い髪でボブカットで水色のインカラーをした女の子。服装は黒のフリルつきレースと上から白いレースを重ねてきている。上半身をアップで描く。笑顔と普通の表情の真ん中。黒い目。左側から見た構図。少し振り向いている。上目遣い
```

**タグ**

```text
black_hair, bob_cut, colored_inner_hair, 1girl, upper_body, smile, black_eyes, looking_back, looking_up, from_side, frills, lace, solo
```

**出力（自然文）**

```text
A single girl with black bob-cut hair featuring striking light blue inner-color highlights is captured in a close-up shot focusing on her upper body. Her face displays a faint smile, conveying a subtle and engaging demeanor to the viewer. She possesses deep black eyes that reflect a soft, contemplative mood within the composition. Her attire consists of a black outfit adorned with intricate frills and lace, layered with an additional white lace overlay that adds textural contrast. The composition is framed from a side view, positioning her slightly turned to look back towards the viewer with an upward gaze. Her hands are positioned subtly, contributing to the overall delicate and composed presentation of the scene. The background remains indistinct, allowing the focus to remain entirely on the detailed textures of her clothing and facial expression. Soft, diffused lighting illuminates her features, casting gentle shadows that enhance the depth of her expression and the intricate details of her layered garments. The overall atmosphere is one of quiet elegance and gentle introspection, perfectly suited for this intimate portrait. The rendering style should emulate a high-quality anime aesthetic, emphasizing smooth shading and detailed fabric textures throughout the image.
```

### A-22 ケース22（履歴ID 150、206語）

**入力**

```text
明るい黄色の髪の毛、紫の目、ロングヘアー、
```

**タグ**

```text
blonde_hair, purple_eyes, long_hair, 1girl, solo
```

**出力（自然文）**

```text
A single girl with bright blonde hair and striking purple eyes is captured in a serene portrait, showcasing her long hair cascading down her shoulders in a smooth, flowing manner. Her long hair possesses a luminous quality, catching the light with subtle highlights that emphasize its length and texture against the background. The girl possesses deep purple eyes that hold a mysterious and captivating gaze, reflecting a quiet introspection within her expression. Her posture is relaxed yet elegant, with her body language suggesting a gentle confidence and an air of quiet solitude. The setting is softly illuminated, creating a gentle ambiance that enhances the overall ethereal quality of the scene. Soft, diffused light gently washes over her form, casting subtle, soft shadows that add depth to the composition without harshness. The camera angle is a medium close-up, framing her face and the expanse of her long hair beautifully within the frame. The art style employs a detailed anime rendering technique, emphasizing the fine strands of her blonde hair and the delicate structure of her features. This rendering style ensures a high level of visual fidelity, making every element of her appearance stand out vividly. The overall atmosphere is one of tranquil beauty and quiet, mesmerizing allure.
```

### A-24 ケース24（履歴ID 151、216語）

**入力**

```text
二人の少女がベッドに座って、軽く微笑んでいる
```

**タグ**

```text
2girls, bed, multiple_girls, smile, on_bed, sitting, light_smile
```

**出力（自然文）**

```text
Two girls are seated comfortably on a large bed, sharing a moment of quiet contentment with a gentle, light smile on their faces. Their features are rendered with delicate anime detail, showcasing soft contours and subtly shaded skin tones that catch the ambient illumination of the room. Her hair frames her face, and her eyes convey a warm, subtle expression of happiness. The other girl mirrors this feeling, her expression a light smile that suggests deep, shared affection between the two figures in the scene. Both girls are positioned sitting upright on the surface of the bed, their postures relaxed yet engaged in the shared quietude of the moment. Her hands rest lightly upon the bed surface, conveying a sense of peaceful repose and gentle connection with her companion. The setting is a spacious bedroom, with the bed dominating the foreground, suggesting a cozy and intimate atmosphere for this shared experience. Warm, diffused lighting streams into the room, casting soft shadows that enhance the gentle contours of their forms. The camera angle is a medium shot, capturing both girls symmetrically within the frame to emphasize their close proximity and shared mood. The overall art style utilizes a detailed anime rendering technique, ensuring every line and shadow contributes to the tranquil and affectionate mood of the scene.
```

### A-25 ケース25（履歴ID 152、214語）

**入力**

```text
白い髪の毛で赤い目をした勝利の女神:ニケのシンデレラが、全身をしっかりメイド服を着てオムライスを作っている。（露出が非常に少ない、胸元を隠す）
背景は真ん中を中心とした3分割（右下、左下から真ん中に伸びる分割線と上端の真ん中から真ん中に伸びる分割線）。それぞれの画面で、シンデレラが、一生懸命料理を作っているシーンを一場面ごとに違う料理工程で見せる。
画面の中央にできたオムライスを視聴者に笑顔で差し出しているようにする。髪型はロングヘアーにする
```

**タグ**

```text
split, white_hair, 1girl, solo, red_eyes, full_body, maid, omurice, covering_breasts, split_screen, food, smile, long_hair
```

**出力（自然文）**

```text
A beautiful goddess figure, resembling Cinderella with striking red eyes and long white hair, is diligently preparing an omurice while wearing a complete maid outfit. Her long white hair cascades down her back, catching the light as she focuses intently on the culinary task before her. She is dressed in a detailed maid uniform, ensuring that the neckline is modestly covered to conceal her chest area completely. The composition is a dynamic split screen, dividing the scene into three distinct sections centered around the middle area. In each segment, the goddess is shown engaged in a different stage of the omurice preparation process, showcasing various cooking steps sequentially. Across the center of the main frame, she presents the finished omurice directly to the viewer with a warm and inviting smile. The lighting is bright and focused, casting soft shadows that emphasize the texture of the food and the details of her attire. The camera angle is a medium shot, framing the entire split composition to clearly illustrate the different cooking phases occurring simultaneously. The art style should feature a high-quality anime rendering with meticulous attention to fabric folds and the sheen on the cooked rice. The atmosphere is one of focused effort mixed with cheerful hospitality as she offers the meal to the audience.
```

### A-26 ケース26（履歴ID 153、227語）

**入力**

```text
白いドレスを着た少女が野原に立って、こちらを見ている
```

**タグ**

```text
white_dress, solo, 1girl, field, standing, looking_at_viewer, dress
```

**出力（自然文）**

```text
A solitary young girl stands gracefully in an open field, directly gazing intently toward the viewer with a serene and captivating expression. She is clad in a flowing white dress that appears ethereal against the expansive, open landscape surrounding her. Her hair is depicted simply, in keeping with the delicate nature of her attire. Her eyes are wide and fixed upon the observer, conveying a deep sense of quiet contemplation and gentle curiosity about the presence before her. The fabric of her white dress exhibits subtle, soft folds where it gathers around her form, suggesting a light, airy texture that catches the ambient light beautifully. Her posture is upright and composed, with her hands resting gently at her sides, maintaining a poised and expectant stillness in the vastness of the field. The setting is a wide, open field stretching out endlessly under a soft, diffused sky, providing a simple and uncluttered backdrop for the central figure. Warm, gentle sunlight illuminates the scene from the side, casting soft, subtle shadows that enhance the three-dimensional form of the girl and the texture of her dress. The camera angle is a medium shot, framing her from the waist up to emphasize her direct gaze and the surrounding open space. The overall art style should be a high-detail anime rendering, emphasizing smooth shading and delicate line work throughout the entire composition.
```

### A-27 ケース27（履歴ID 154、230語）

**入力**

```text
画面を真ん中で縦半分にした構図で、割った部分の線は壁として描く。右半分には壁に寄りかかっているような姿勢で、学生服を着ている白いボブカットの女の子を描く。左半分にはメイド服を着て、左手でトレーを持って、ジュースを乗せて持っている白いボブカットの女の子を描く。
```

**タグ**

```text
split_screen, 2girls, multiple_girls, school_uniform, white_hair, maid, holding_tray, juice, holding, bob_cut, tray
```

**出力（自然文）**

```text
A striking split-screen composition divides the frame vertically down the center, where the dividing line itself is rendered as a solid wall structure. On the right side, a girl with white bob-cut hair leans casually against this wall in a relaxed and contemplative posture, wearing a school uniform. Her white bob-cut hair frames a face with a neutral expression, and the fabric of her school uniform shows subtle folds where it meets the wall surface. Conversely, the left half features a second girl, dressed in a maid outfit, who is actively holding a tray with her left hand while simultaneously balancing a glass of juice upon it. This girl also possesses white bob-cut hair, and her posture suggests a gentle, attentive demeanor as she presents the beverage. The lighting casts distinct shadows across the dividing line, emphasizing the separation between the two distinct scenes. The atmosphere is bright yet balanced, highlighting the contrast between the casual lean and the service-oriented action on the opposite side. The camera angle is a direct, centered view, ensuring both figures are clearly visible within the defined composition space. The art style should emulate detailed anime rendering, showcasing fine textures in the fabric of the uniform and the sheen on the glass of juice. This detailed rendering captures the specific details of the white hair strands and the texture of the maid outfit fabric meticulously.
```

### A-29 ケース29（履歴ID 155、235語）

**入力**

```text
白い髪の毛で赤い目をしたニケのシンデレラが、メイド服を着てオムライスを作っている。
背景は真ん中を中心とした3分割。それぞれの画面で、シンデレラが、一生懸命料理を作っているシーンを一場面ごとに違う料理工程で見せる
```

**タグ**

```text
cooking, white_hair, 1girl, solo, red_eyes, maid, omurice, split_screen, food
```

**出力（自然文）**

```text
A beautiful Nikke Cinderella with pure white hair and striking red eyes is diligently preparing an omurice while wearing a detailed maid outfit. Her expression is one of intense concentration as she focuses on the intricate steps of the cooking process. The scene is dramatically divided into three distinct panels, each showcasing a different stage of the omurice preparation in a unique culinary sequence. The composition is a split-screen arrangement centered around the middle section, allowing the viewer to observe the entire process unfold sequentially. In one panel, she is meticulously preparing the rice base, demonstrating focused effort in the kitchen environment. Another segment captures her carefully assembling the omelet, showing precise movements as she handles the ingredients with care. The third visual section illustrates the final plating of the omurice, highlighting the finished dish with careful attention to presentation. Her posture throughout the entire sequence conveys a strong sense of dedication and earnest commitment to her culinary task. Soft, warm lighting illuminates the kitchen area, casting gentle shadows that emphasize the textures of the food and her focused demeanor. The overall atmosphere is one of focused, industrious activity, creating a warm and inviting domestic scene. The camera angle is set to a medium shot, framing the central figure effectively within the tripartite division of the background. The art style should feature rich, detailed anime rendering with meticulous attention to fabric folds and food textures.
```

---

## 付録B: ケース別チェックポイント（採点後に読む）

先に読むと判定が誘導される。**採点と集計が終わってから**照合に使う。

| ID | 入力で明示されている要素 | v13で確認する点（採点後） |
|---|---|---|
| 20 | 長い黒髪、青いドレス、椅子に座る | 黒髪・青いドレス・椅子・座姿の保持。室内装飾やレースの追加が軽微に収まっているか |
| 21 | 黒髪ボブ、水色インカラー、黒フリル+白レース、上半身アップ、笑顔と普通の中間、黒目、左側から、少し振り向き、上目遣い | `from_side` / `looking_up` は入力由来。`gentle` などの表情追加。水色インカラーのタグ有無 |
| 22 | 明るい黄色の髪、紫の目、ロングヘア | 明るい黄色→`blonde` の言い換えはv9で許容済み。服・ポーズ・背景の追加範囲 |
| 24 | 二人、ベッドに座る、軽く微笑む | 二人とも記述されているか。単数代名詞の混在。未指定の服装・距離感の追加。`2girls` |
| 25 | 白髪赤目、ニケのシンデレラ、メイド服全身、オムライス作成、露出少ない/胸元を隠す、3分割、各画面で違う工程、中央で差し出す、ロングヘア | `covering_breasts` と自然文の整合。`from_below` / `from_side` は入力にも自然文にも無い。`omurice` タグの有無。3分割の説明 |
| 26 | 白いドレス、野原、立つ、こちらを見る | 未指定の髪の長さ・型・色、目色を断定していないか。`looking_at_viewer` |
| 27 | 縦半分、分割線=壁、右=学生服で壁寄りかかり白ボブ、左=メイド服で左手トレー+ジュース白ボブ | `2girls` / `multiple_girls` と同一人物解釈。自然文にある `tray` / `juice` がタグに無い。左右の対応。251語の冗長さ |
| 29 | 白髪赤目、ニケのシンデレラ、メイド服、オムライス、3分割、各画面で違う料理工程 | 「野菜を切る」「卵を焼く」など具体工程の追加。単数一貫（`multiple_girls` なし）。`cooking` / `food` |

- 付録Bは「こう採点せよ」ではない。気づかなかった観点の確認用。
- ここに書かれた追加要素が気になっても、すでに付けた点は**正当な理由がある場合のみ**修正する。修正したら理由を残す。

---

## 付録C: v9ベースライン（比較用）

| 軸 | v9平均 | 目標 | v9判定 |
|---|---:|---:|---|
| 意図の保持 | 4.38 | 3.5 | 達成 |
| 英語の自然さ | 3.63 | 3.5 | 達成 |
| 構造 | 4.50 | 3.5 | 達成 |
| 入力への忠実さ | **2.88** | 3.5 | **未達** |
| 総合 | 3.63 | 3.5 | 暫定合格 |

- v9のケース別スコアは [`research/PIXAI_EVAL.md`](PIXAI_EVAL.md) を参照。
- 比較時の見方:
  - 忠実さが **+0.62以上**（2.88→3.50）改善していれば目標達成。
  - 意図・自然さ・構造が **-0.5を超えて下がった** 場合は、忠実さが上がっていても回帰として扱う。
  - ケース単位では、v9で2だった 21・25・27 が3以上になっているかを確認する。

---

## 付録D: 記録と次のアクション

1. 採点結果をこのファイルの §5 の表に記入する（または別ファイルに転記）。
2. 集計して §6 の判定を付ける。
3. 判定が「合格」なら `research/M7_LOCAL_V13_EVAL.md` の「判定（暫定）」を正式結果に更新する。
4. 「条件付き合格」「不合格」なら、落としたケースと重大項目を `research/M7_REPORT.md` の残課題へ追加する。
5. どの判定でも、**自己点検スコア（4.0〜4.5相当）は根拠にしない**。人手の結果のみを正式値とする。

### 記入欄（2026-10-28 記入済み）

- 評価者: ユーザー
- 評価日: 2026-10-28
- 忠実さ平均: **3.625**（29/40）
- 総合平均: **4.125**（33/40）
- 意図: 4.75 / 自然さ: 4.25 / 構造: 5.00
- タグ整合性平均: **3.875**（31/40）
- 重大違反: **2件**（ID24・ID26 の未指定ヘア）
- 判定: **不合格**
- 次アクション: v13.1（0.7.5-m7）の8ケースを同じ基準で再採点する（§10・付録A）
