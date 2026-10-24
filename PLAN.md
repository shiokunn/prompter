# Prompter — 日本語→PixAI DiT プロンプト変換 Android アプリ 企画書

- 最終更新: 2026-10-24
- ステータス: **M1 完了 / M2 コア完了 / M3 実装完了 / M4 実装完了（実通信確認済み） / M5 実装・実機確認完了 / M6 実装・CI完了（実機検証待ち）**
- 作業ディレクトリ: `~/prompter`
- 関連調査:
  - [`research/DECISIONS.md`](research/DECISIONS.md) — 確定した要件
  - [`research/PIXAI_DIT.md`](research/PIXAI_DIT.md) — PixAI DiT のプロンプト仕様
  - [`research/MODEL_SELECTION.md`](research/MODEL_SELECTION.md) — 4B 以下のオンデバイスモデル選定
  - [`research/SPIKE.md`](research/SPIKE.md) — 辞書・マッチングの実測
  - [`research/M1_FINAL_REPORT.md`](research/M1_FINAL_REPORT.md) — fresh holdout の M1 評価
  - [`research/M2_REPORT.md`](research/M2_REPORT.md) — M2 アプリ骨格の実装
  - [`research/M3_REPORT.md`](research/M3_REPORT.md) — M3 タグ編集・履歴の実装
  - [`research/M4_REPORT.md`](research/M4_REPORT.md) — M4 OpenRouter AI補完の実装
  - [`research/M5_REPORT.md`](research/M5_REPORT.md) — M5 ML Kit翻訳・安定debug署名の実装
  - [`research/M6_REPORT.md`](research/M6_REPORT.md) — M6 ローカルLLMの実装・実機検証手順

---

## 0. 結論（先に5行）

1. **主対象は PixAI の DiT 系（Tsubaki.3 / Tsubaki.2）**。DiT はタグ列より **英語の自然文** に向くため、既定出力を「Danbooru タグ」から「PixAI 用の英語プロンプト」に変更する。
2. ただしタグ変換は捨てない。**PixAI Haruka v2 / SDXL / Pony / Illustrious / A1111 / ComfyUI / NovelAI** 向けのタグモードを併設し、同じ日本語入力から両方を生成する。
3. オフラインの土台は **Danbooru 辞書 + ML Kit 翻訳 + テンプレート合成**。ここだけで動く。
4. AI モードは **OpenAI API 互換（既定: OpenRouter）** を任意 ON。API キーは BYOK。AI を使わない人向けに、**ML Kit翻訳（実装済み）** と、将来の **Gemma 4 E2B（2.58GB）** の任意ダウンロードを用意する。
5. 配布は **自分用 APK**。実機は **arrows Alpha**（12GB RAM / 512GB / Android 15・16）。成人向けタグは **そのまま扱う**。MVP は **5〜6週間**、最初に辞書品質を固める。

---

## 1. 解きたい課題

画像生成 AI は英語プロンプトの方が意図を制御しやすい。しかし日本語話者にとって、

- 日本語で考えた内容を英語プロンプトに変換するのが面倒
- 汎用翻訳は「自然な英文」にはなるが、**PixAI DiT が求める構図・光・画風の構造**にならない
- PixAI DiT はタグ列より自然文が得意だが、**何をどの順で書けばいいか**が分かりにくい
- モデルごとにタグ形式・重み・ネガティブの構文が違う
- スマホで画像生成するとき、ブラウザ翻訳とコピペを往復するのが苦痛
- オフラインやプライバシーを重視したい
- AI を使いたくない人もいる

という課題がある。

### 1.1 PixAI DiT で特に困る例

日本語:

```
銀髪ツインテールの少女が、制服姿で夕暮れの教室に立っている。
窓から差し込むオレンジ色の光、少し物憂げな表情。
```

汎用翻訳:

```
A silver-haired twin-tailed girl stands in a classroom at sunset.
Orange light comes through the window, with a slightly melancholic expression.
```

これは英文としては正しい。しかし PixAI DiT では、

- 誰が主役か
- 何をしているか
- どこにいるか
- 光はどこから来ているか
- どんな画風・レンズ・構図か

が弱い。PixAI の公式基本式に合わせると、次のように **Subject + Action + Scene + Mood + Style** で組み直す必要がある。

```
A silver-haired girl with long twin tails stands in a quiet classroom at sunset.
She wears a navy school uniform with a short pleated skirt and a loosely tied ribbon.
Warm orange sunlight pours through the tall windows, casting long shadows across the
desks and lighting the edge of her hair. Her expression is calm and slightly
melancholic, as if she is lost in thought. Medium shot, eye level, shallow depth
of field, cinematic anime illustration, soft film grain, muted warm tones.
```

この「翻訳 + 構造化 + 画風指定」を1画面でやるのが本アプリ。

### 1.2 タグ形式が必要な場面

PixAI でも Haruka v2 は SDXL のタグベース。ComfyUI / A1111 / NovelAI / Pony / Illustrious もタグ形式が主流。したがって、

- PixAI DiT: 自然文
- PixAI Haruka v2 / SDXL 系: タグ
- NovelAI: タグ + 独自重み
- Flux: 自然文
- Midjourney: 自然文 + `--no`

を同じ入力から出し分ける。

---

## 2. 提案するプロダクト

**仮称: Prompter**（「プロンプトを作る人」）

日本語の自然言語を入力すると、

1. **PixAI DiT 用の英語自然文プロンプト**
2. **Danbooru タグ**
3. **タグの日本語訳**
4. **任意の AI 補完（OpenRouter / ローカル LLM）**

を生成し、モデル別の形式でコピー・共有できる Android アプリ。

### 2.1 変換モード

| モード | 入力 | 出力 | 主な用途 |
|---|---|---|---|
| **PixAI DiT** | 日本語の文章 | 英語の自然文（60〜180語） | Tsubaki.3 / Tsubaki.2 |
| **タグ** | 日本語の文章・単語 | Danbooru canonical tags + 重み | Haruka v2 / SDXL / Pony / A1111 / ComfyUI / NovelAI |
| **ハイブリッド** | 日本語の文章 | 自然文 + LoRA トリガー/主要タグ | PixAI DiT でキャラ再現を強めたいとき |
| **タグ和訳** | 英語タグ | 日本語の意味・説明 | 他人のプロンプトの解読 |
| **AI 補完** | 上記の入力 | 構造化 JSON → 各形式 | 複雑な構図・複数キャラ・画風 |

- メイン画面は「PixAI DiT」を既定にする。
- 出力形式はセグメントで切り替える。
- AI 補完は**明示的に ON にしたときだけ**呼ぶ。入力中の自動呼び出しはしない（コストとプライバシーのため）。

### 2.2 コアバリュー

1. **PixAI DiT に最適化した構造化**: `Subject + Action + Scene + Mood + Style` で英文を組み立てる。汎用翻訳の「英文」で終わらせない。
2. **タグ正規化**: `silver-haired` → `silver_hair`、`camera gaze` → `looking_at_viewer`。Danbooru の canonical tag に寄せる。
3. **1入力から複数サービス**: PixAI DiT / PixAI SDXL / A1111 / ComfyUI / NovelAI / Flux / Midjourney の構文差を吸収。
4. **オフライン**: 辞書と ML Kit モデルを端末に入れれば、機内モードでも下書きできる。
5. **AI の選択肢**: OpenRouter の任意モデル、またはオンデバイスの Gemma 4 E2B。特定ベンダーに閉じない。
6. **修正学習**: ユーザーが「この日本語はこのタグ」と直すと、ローカル辞書に反映。
7. **スマホ完結**: コピー、共有、`ACTION_PROCESS_TEXT` からの起動。

### 2.3 既存手段との比較

| 手段 | できること | 足りないこと |
|---|---|---|
| PixAI の Prompt Helper / Autocomplete | PixAI 内でプロンプト支援 | 日本語入力からの構造化、他サービス、オフライン、履歴、修正学習 |
| Gboard の入力中翻訳 | 一般的な日英翻訳 | PixAI の構文、タグ正規化、モデル別プリセット |
| Web の和英プロンプトツール（Tag Recipe 等） | 日本語→タグ、辞書検索 | PixAI DiT の自然文、オフライン、共有/選択メニュー、修正学習 |
| A1111 拡張（sd-ai-prompt-translator 等） | PC 上でプロンプト翻訳 | スマホ、PixAI、画像生成アプリ横断 |
| NovelAI の漢字入力候補 | NovelAI 内でタグ候補 | NovelAI 専用 |
| Danbot などのタグ変換 LLM | 高品質なタグ生成 | PixAI DiT の自然文、エディタ、プリセット、オフライン辞書、モバイル UI |
| OpenRouter / ChatGPT を直接使う | 高品質な変換 | PixAI 用の型、タグ検証、履歴、コピー、オフライン |

→ **「PixAI DiT の自然文 + タグ + モデル別整形 + 修正学習」を1画面で完結**させるのが差別化。単なる翻訳アプリにはしない。

### 2.4 やらないこと（MVP）

- 画像生成そのもの（PixAI / Stable Diffusion の API 連携もしない）
- SNS 投稿・ギャラリー
- カスタム IME（キーボード）の全面実装
- 多ユーザー・課金・クラウド同期
- 全タグの完全な日本語化（上位タグに絞る）
- 成人向けタグのフィルタ・年齢制限（個人用のため。設定で制御）
- Play Store 配布

---

## 3. 技術的実現性

### 3.1 結論

**可能。** 要素技術はすべて既存のもので揃う。難しいのはアプリの実装ではなく、

1. **日本語表現 → PixAI 向けの構造化された英文** の品質
2. **日本語表現 → Danbooru タグ** の辞書品質
3. **誤マッチの抑制**
4. **小さいローカルモデルで自然文を破綻させないこと**

の4点。M1 で辞書を、M6 でローカルモデルを検証する。

### 3.2 要素技術の比較

| 要素 | 選択肢 | 評価 | 採用 |
|---|---|---|---|
| オフライン翻訳 | ML Kit Translation（Google） | 50+言語、約30MB/言語、API 23+、無料、ダウンロード後オフライン | ◎ フォールバック |
| クラウド LLM | **OpenAI API 互換（OpenRouter 既定）** | モデル選択自由、JSON Schema、安価、BYOK | ◎ AI モード |
| ローカル LLM | **Gemma 4 E2B + LiteRT-LM** | 2.58GB、実効2.3B、Apache-2.0、Android 公式 Kotlin API | ◎ 任意パック |
| ローカル LLM（高品質） | Gemma 4 E4B | 3.65GB、実効4.5B。12GB 端末向け。4B 超に注意 | ○ 任意 |
| タグ特化モデル | DanbotNL-2408-260M | 260M、タグ専用。自然文は不得意 | △ 将来のタグ補助 |
| 辞書 | Danbooru CSV + 日本語 CSV + wiki 別名 | 14万タグ、9.4万日本語、DB 16.8MB | ◎ |
| マッチング | Trie / Aho-Corasick + 境界判定 | スパイクで 0.05ms/プロンプト（ロード後） | ◎ |
| 保存 | Room / SQLite | Android 標準。FTS4 で検索 | ◎ |
| UI | Jetpack Compose | 既存の LocalTTS-App と同じ構成が使える | ◎ |
| ビルド | GitHub Actions | Termux に Android SDK を入れずに APK を作れる | ◎ |

**なぜネイティブ（Compose）か**: PWA/Capacitor なら UI の反復は速いが、ML Kit のオンデバイス翻訳、Room、`ACTION_PROCESS_TEXT`、LiteRT-LM の JNI/GPU、将来の IME との相性はネイティブが上。テキスト入力・変換・共有が中心なので Compose を選ぶ。

### 3.3 実測スパイク（2026-10-23）

`tools/spike_match.py` で、公開 CSV から簡易辞書を作り、日本語プロンプト10例を変換した。

#### データ規模

| 項目 | 数 |
|---|---:|
| Danbooru タグ（a1111 tagcomplete） | 140,782 |
| 日本語訳（機械翻訳） | 99,999 |
| 日本語訳（手作業、高品質） | 427 |
| 別名（aliases） | 30,276 |
| SQLite サイズ | 16.8MB（gzip 8.9MB、FTS 除く） |
| 変換時間（ロード後） | 約0.05ms/プロンプト |

#### 素朴な最長一致の問題

- 「銀髪」「青い目」「夕焼け」「桜」など、表記ゆれで落ちる。
- 「サイバーパンク」の中の「パン」を `bread` と誤マッチする。
- 単漢字（桜・雨・夜）を辞書から外すと取りこぼす。

#### 別名を足した結果

テスト用に約50個の別名（銀髪→`silver_hair`、カメラ目線→`looking_at_viewer`、俯瞰→`from_above` など）と、短いカタカナ語の境界判定を追加すると、10例すべてで主要な被写体・服装・背景・構図をカバーできた。ただし「黒髪ロング」の `long_hair` のように、複合語の一部はまだ落ちる。

```
JA: 白いワンピースを着た金髪の少女が、草原に座って空を見上げている
MATCH: white_dress, blonde_hair, 1girl, grass, sitting, looking_up
```

ただし、この別名はテスト用に手で作ったもの。**この別名を上位5,000タグ分に自動生成し、検証するのが開発の肝** になる。

#### 汎用 MT との比較

| 日本語 | 汎用 MT の出力例 | 必要なタグ | PixAI DiT に必要なこと |
|---|---|---|---|
| 夏の海辺で浴衣を着た女の子、夕焼け、逆光 | Girl in yukata, sunset, backlight on summer beach | `1girl, yukata, sunset, backlighting, beach, summer` | 光の方向、波、風、ショット、画風を文章化 |
| 手を振っている、笑顔、カメラ目線 | Waving, smiling, camera gaze | `waving, smile, looking_at_viewer` | `looking at the viewer` を自然文に |
| 水彩風の柔らかいタッチ、淡い色合い | Soft watercolor touch, light shades | `watercolor_(medium), pastel_colors` | 画風・色・筆致を Style 層に |
| サイバーパンク風の街並み、ネオン、雨、夜 | Cyberpunk, Neon, Rain, Night | `cyberpunk, cityscape, neon_lights, rain, night` | ネオンの反射、雨の質感、レンズを文章化 |

→ 汎用 MT は「自然な英語」を返すが、PixAI DiT の構造には足りない。**翻訳の後に「PromptPlan」への構造化**が必要。

### 3.4 PixAI DiT の仕様（調査結果）

詳細は [`research/PIXAI_DIT.md`](research/PIXAI_DIT.md)。

- DiT は **自然言語プロンプト** を推奨。タグ列は不利。
- 基本式: `Subject + Action + Scene + Mood + Style`。
- 1〜3段落、60〜180語が実用域。公式の商用例は 300 語超もある。
- 品質タグは使えるが必須ではない。自然文を壊さない範囲で任意。
- ネガティブはモデル・モード依存。Tsubaki.2 は Lite/Standard でカスタム不可。アプリでは既定で非表示。
- Tsubaki.3 は日本語文字を画像内に描けるが、プロンプト理解が日本語対応という意味ではない。**出力は英語**にする。

### 3.5 オンデバイスモデル

詳細は [`research/MODEL_SELECTION.md`](research/MODEL_SELECTION.md)。

**結論: Gemma 4 E2B を本命、E4B を 12GB 端末向けの任意高品質パックにする。**

| モデル | 実効/総 | サイズ | 評価 |
|---|---:|---:|---|
| **Gemma 4 E2B** | 2.3B / 5.1B | 2.58GB CPU / 2.01GB GPU | ◎ 公式 LiteRT-LM、Apache-2.0、多言語、自然文向き |
| Gemma 4 E4B | 4.5B / 8B | 3.65GB CPU / 2.97GB GPU | ○ 高品質だが 4B 超。12GB 端末限定 |
| Qwen3-4B | 4B | 約2.5GB Q4 | △ 日本語は強いが LiteRT-LM 公式外。llama.cpp が必要 |
| Phi-4-mini | 3.8B | 3.91GB | △ 日本語が弱い |
| Danbot 260M | 0.26B | 524MB | △ タグ専用。自然文は不可 |

- arrows Alpha は 12GB RAM なので E2B は余裕。E4B も動くが見込みで 15〜23秒/120語。
- まず E2B を実装し、実機で品質・速度を測る。E4B はその後。
- モデルは APK に同梱せず、初回に任意ダウンロード。GPU がダメなら CPU にフォールバック。

### 3.6 まだ検証していない点

| 項目 | なぜ重要 | 検証方法 |
|---|---|---|
| 辞書の日本語別名の品質 | PixAI 自然文の材料になる | 500タグで生成→逆翻訳→人手レビュー |
| ML Kit のアニメ用語の翻訳品質 | オフライン下書きの品質 | 実機で50文を翻訳し、タグ Recall を測る |
| 英語→タグ正規化の精度 | 汎用 MT の出力をタグに変える | 100文で正解タグとの一致率 |
| Gemma 4 E2B の日本語→PixAI 自然文 | ローカル AI の価値 | 30プロンプトで人手評価 |
| Mali-G615 で LiteRT-LM GPU が動くか | 生成速度 | 実機で GPU/CPU を比較 |
| E2B の 2.58GB ダウンロード | UX | DownloadManager、再開、SHA-256 |
| OpenRouter のモデル別 JSON Schema 対応 | 安定したパース | 3モデルでテスト |
| PixAI DiT の最大プロンプト長 | 長い自然文が切られないか | 100〜300語で生成し、PixAI 上で確認 |
| 成人向けタグのプロバイダ遮断 | AI 出力が欠ける | OpenRouter のモデル/プロバイダを変えて確認 |
| AGP 9.3 + Compose の初回 CI | ビルドが通るか | 新規プロジェクトで `assembleDebug` |

これらは M1〜M6 の間に潰す。特に辞書品質と E2B の品質は、UI を作り込む前に確認する。

### 3.7 カスタムキーボードは必要か

**MVP では不要。**

- アプリ内の入力欄で変換すれば「即変換」の体験は成立する。
- 他アプリへは `ACTION_PROCESS_TEXT`（テキスト選択メニュー）と `ACTION_SEND`（共有）で送れる。
- カスタム IME はキーボード全体の実装が必要で、審査・プライバシー要件も重い。
- 個人用 APK なら Play Store 審査は不要だが、実装コストは高い。v3 以降。

---

## 4. アーキテクチャ

```
┌─────────────────────────────────────────────┐
│ Jetpack Compose UI                           │
│ ・日本語入力 ・PixAI自然文 ・タグチップ       │
│ ・AI ON/OFF ・モデル選択 ・コピー/共有        │
└───────────────┬─────────────────────────────┘
                │ StateFlow / collectLatest
┌───────────────▼─────────────────────────────┐
│ PromptViewModel                              │
│ ・入力デバウンス（ローカルのみ）              │
│ ・AI は明示ボタン                            │
└───────────────┬─────────────────────────────┘
                │
┌───────────────▼─────────────────────────────┐
│ PromptConverter（ドメイン層）                 │
│  1. 正規化（NFKC / かな / 記号）             │
│  2. 辞書最長一致（Trie）                     │
│  3. PromptPlan に構造化                      │
│     subject / character / outfit / pose /    │
│     scene / mood / style / composition       │
│  4. オフライン合成 or AI 補完                │
│  5. タグ検証（DB 照合）                      │
│  6. 構文整形（PixAI / A1111 / NAI / Flux…）  │
└───┬───────────────┬───────────────┬─────────┘
    │               │               │
┌───▼────────┐ ┌────▼──────────┐ ┌──▼──────────────┐
│ Room       │ │ ML Kit        │ │ AI 層            │
│ tags       │ │ Translator    │ │ ・OpenRouter     │
│ aliases    │ │ （任意）       │ │ ・OpenAI 互換    │
│ prompts    │ └───────────────┘ │ ・Gemma 4 E2B    │
│ corrections│                   │   （LiteRT-LM）  │
│ ai_cache   │                   └──────────────────┘
└────────────┘
```

### 4.1 変換パイプライン（PixAI DiT）

入力:

```
銀髪ツインテールの少女が、制服姿で夕暮れの教室に立っている。
窓から差し込むオレンジ色の光、少し物憂げな表情。
```

1. 正規化 → 辞書マッチ:
   - `銀髪` → `silver_hair`
   - `ツインテール` → `twintails`
   - `少女` → `1girl`
   - `制服` → `school_uniform`
   - `教室` → `classroom`
   - `夕暮れ` → `sunset`
   - `窓` → `window`
   - `物憂げ` → `melancholic`（候補）
2. PromptPlan:
   - subject: 1girl
   - character: silver hair, twintails
   - outfit: school uniform
   - pose: standing
   - scene: classroom, window
   - mood: sunset, orange light, melancholic
   - style: cinematic anime illustration, soft light
   - composition: medium shot, eye level, shallow depth of field
3. オフライン or AI:
   - オフライン: テンプレートで 80〜150語の英文を合成。
   - AI: `PromptPlan` と候補タグを LLM に渡し、自然文を整形。
4. 出力:
   - PixAI DiT: 英文自然文。タグは折りたたみ。
   - タグ: `1girl, silver_hair, twintails, school_uniform, classroom, sunset, window, standing, melancholic`

### 4.2 変換パイプライン（タグ）

入力:

```
黒髪ロング、青い目、八重歯、セーラー服、教室、笑顔
```

1. 辞書: `black_hair`, `long_hair`, `blue_eyes`, `fang`, `serafuku`, `classroom`, `smile`
2. 候補: `long_hair` は「黒髪ロング」の複合語から補完。
3. 出力: `1girl, black_hair, long_hair, blue_eyes, fang, serafuku, classroom, smile`
4. プリセット:
   - A1111/ComfyUI: `1girl, black_hair, ...`
   - NovelAI: `1girl, black_hair, ...`
   - PixAI Haruka v2: SDXL タグとしてカンマ区切り。

### 4.3 形態素解析の扱い

- MVP は **最長一致 + 境界ヒューリスティック**。
- 短いカタカナ語が長いカタカナ語の内部に現れる場合（`サイバーパンク` の `パン`）を除外する。
- それでも誤マッチが残る場合、次の順で導入を検討:
  1. Android の `BreakIterator`（追加ライブラリ不要）
  2. TinySegmenter（軽量・純 Java）
  3. Kuromoji / Sudachi（高精度だが辞書サイズが増える）
- 誤マッチは「候補UIで直せる」ことを前提にし、完璧な分割を目指さない。修正結果を `corrections` に保存して学習する。

### 4.4 モデル別の出力プリセット

| プリセット | 出力形式 | 重み | ネガティブ | 注意 |
|---|---|---|---|---|
| **PixAI DiT（Tsubaki.3/2）** | 英語自然文 60〜180語 | なし | 既定は空。詳細設定に隠す | タグ列にしない |
| **PixAI Haruka v2（SDXL）** | カンマ区切りタグ | `(tag:1.2)` | 別フィールド | SDXL タグ |
| **A1111 / ComfyUI** | カンマ区切りタグ | `(tag:1.2)` | 別フィールド | 括弧エスケープ |
| **SDXL / Pony / Illustrious** | カンマ + スペース | `(tag:1.2)` | 別フィールド | booru タグ |
| **NovelAI V4/V5** | カンマ区切り | `{tag}` / `[tag]` / `1.2::` | Undesired Content | 括弧重みは使わない |
| **Flux** | 自然文 | なし | 実質不要 | タグ列より文章 |
| **Midjourney** | 自然文 + パラメータ | `tag::2` | `--no tag` | 括弧重みは使わない |

- PixAI DiT では **重みスライダーを出さない**。タグモードだけ。
- コピーは「本文のみ」「タグ付き」「ネガティブ込み」を選べる。
- プリセットは表示形式だけでなく、コピー時の文字列を変える。

---

## 5. データ設計

### 5.1 データソース

| データ | 提供元 | ライセンス/注意 | 用途 |
|---|---|---|---|
| Danbooru タグ + 別名 + 投稿数 | [a1111-sd-webui-tagcomplete](https://github.com/DominikDoom/a1111-sd-webui-tagcomplete)（MIT） | タグデータは Danbooru 由来 | タグ辞書の土台 |
| 日本語訳（手作業） | [booru-japanese-tag](https://github.com/boorutan/booru-japanese-tag)（MIT） | 427件。高品質 | Tier A |
| 日本語訳（機械） | 同上 | 99,999件。誤訳あり | Tier C（候補提示） |
| Danbooru wiki の別名 | Danbooru API / [HF dataset](https://huggingface.co/datasets/isek-ai/danbooru-wiki-2024) | wiki 本文のライセンスに注意 | Tier B |
| 合成キャプション | [Danbot dataset](https://huggingface.co/datasets/dartags/danbooru-2408-blind-captions) | 6.33M ペア。評価に利用 | 評価 |
| タグ特化モデル | [DanbotNL-2408-260M](https://huggingface.co/dartags/DanbotNL-2408-260M)（Apache-2.0） | 2024/08 まで。要変換 | 将来のタグ補助 |
| ローカル LLM | [Gemma 4 E2B LiteRT](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)（Apache-2.0） | 2.58GB | オフライン AI |

**注意**: 個人用なら問題になりにくいが、公開・配布する場合は Danbooru の利用規約、各データのライセンス、成人向けタグの扱いを確認する。MVP は私的利用（APK 配布）を前提にする。

a1111 の `danbooru.csv` は約14万タグで、Danbooru 全体（約100万）の一部。人気タグ中心なので MVP には十分だが、マイナーな版権キャラは拾えない。必要なら Danbooru 公式 API / ダンプを追加する。

### 5.2 辞書の品質ティア

| Tier | 内容 | 信頼度 | 使い方 |
|---|---|---|---|
| A | 手作業翻訳 427件 + 手動キュレーション | 最高 | 自動変換に使う |
| B | LLM 生成 + 逆翻訳検証済みの別名（上位5,000タグ） | 高 | 自動変換に使う |
| C | Danbooru wiki の `other_names`（日本語含む） | 中 | 候補提示、条件付き |
| D | 機械翻訳 99,999件 | 低 | 検索・候補提示のみ |
| E | ユーザー修正 | 最高（ローカル） | 次回以降の自動変換に使う |

### 5.3 別名生成パイプライン（重要）

1. 上位5,000タグ（投稿数順）を抽出。
2. Danbooru wiki の説明・別名・タグ名を LLM に渡し、日本語の別名を3〜8個生成。
   - 例: `looking_at_viewer` → `カメラ目線`, `こちらを見ている`, `視聴者を見つめる`
   - 例: `from_above` → `俯瞰`, `上から見下ろす`, `見下ろし`
3. **逆翻訳検証**: 生成した日本語を英訳し、元のタグまたはその別名に戻るか確認。戻らないものは捨てる。
4. **重複解消**: 同じ日本語が複数タグに紐づく場合（`リボン`→`ribbon`/`bow`/`hair_ribbon`）は、投稿数と文脈でランク付けし、候補として保持。
5. **人手レビュー**: 上位500タグだけ目視確認。残りは信頼度付き。
6. **DB ビルド**: SQLite + 正規化インデックス + Trie 用の上位語リスト。

LLM 生成はビルド時のみ。アプリ実行時には使わない（オフライン・高速・無料）。

#### 別名生成プロンプトの例

```
あなたは Danbooru タグと日本語の画像生成プロンプトの専門家です。
タグ名と wiki の説明から、日本語話者が実際に使いそうな
検索語・言い換えを3〜8個生成してください。

制約:
- 自然な日本語にする
- タグの意味を変える表現は禁止
- キャラクター名・版権名は含めない
- 成人向けタグも除外しない。必要なら露骨な語もそのまま使う
- JSON のみ: {"aliases": ["...", "..."]}

タグ: silver_hair
説明: Hair that is silver in color...
```

生成後、各別名を英訳し、元のタグまたは別名に戻るかで検証する。`銀髪`→`silver hair`→`silver_hair` なら合格、`銀色の髪`→`silver colored hair`→`silver_hair` も合格。`銀髪`→`gray hair` なら不合格。

### 5.4 スキーマ案

```sql
CREATE TABLE tags (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,       -- silver_hair
  category INTEGER,                -- general/artist/copyright/character/meta
  post_count INTEGER,
  wiki_summary_ja TEXT,            -- 任意。上位タグのみ
  is_adult INTEGER DEFAULT 0       -- 表示用。フィルタには使わない
);

CREATE TABLE aliases (
  tag_id INTEGER,
  alias TEXT,                      -- silver-haired, 銀髪, ぎんぱつ
  lang TEXT,                       -- en/ja
  source TEXT,                     -- manual/llm/wiki/machine
  confidence REAL
);

CREATE TABLE jp_terms (
  tag_id INTEGER,
  surface TEXT,                    -- 表示用
  norm TEXT,                       -- 正規化済み
  source TEXT,
  confidence REAL
);

CREATE TABLE prompts (
  id INTEGER PRIMARY KEY,
  created_at INTEGER,
  input TEXT,
  output TEXT,
  output_mode TEXT,                -- pixai_dit/tags/hybrid
  preset TEXT,
  model TEXT,
  pinned INTEGER DEFAULT 0
);

CREATE TABLE corrections (
  input_norm TEXT,
  tag_id INTEGER,
  created_at INTEGER
);

CREATE TABLE ai_cache (
  key TEXT PRIMARY KEY,            -- SHA-256(input + mode + model + prompt_version)
  response_json TEXT,
  created_at INTEGER
);

CREATE TABLE settings (
  key TEXT PRIMARY KEY,
  value TEXT
);
```

- 全文検索は FTS4（Android 互換）。
- 上位5,000語の Trie は起動時にメモリ展開（数MB）。
- 全14万タグは SQLite 検索。英字の前方一致・あいまい検索に使う。
- `ai_cache` は設定から削除可能。プロンプト本文を含むため、バックアップ対象外。

### 5.5 DB の配布

- **個人用 MVP は APK にフル DB を同梱**する（SQLite 16.8MB、APK では gzip され約9MB）。
- Room の `createFromAsset` で初回起動時にコピー。
- 将来、配布する場合は seed DB（上位5,000タグ、1〜3MB）+ 初回ダウンロードに切り替える。
- 更新: GitHub Releases に `promptdb-vN.sqlite.gz` と manifest を置き、設定画面から更新。
- 成人向けタグも DB に含める。フィルタしない。

---

## 6. UI / UX

### 6.1 メイン画面（ワイヤーフレーム）

```
┌──────────────────────────────────────┐
│ Prompter              [AI OFF] [設定] │
│ [PixAI DiT] [タグ] [ハイブリッド] [和訳] │
├──────────────────────────────────────┤
│ 日本語入力                              │
│ 銀髪ツインテールの少女が、制服姿で...    │
│                              [クリア] [貼付] │
├──────────────────────────────────────┤
│ PixAI DiT プロンプト        [コピー] [共有] │
│ A silver-haired girl with long twin   │
│ tails stands in a quiet classroom at  │
│ sunset. She wears a navy school...    │
│ [AIで整える] [短く/詳しく] [タグを見る(8)] │
├──────────────────────────────────────┤
│ 候補: long_hair / braid / ponytail     │
│ 履歴                                   │
└──────────────────────────────────────┘
```

- AI OFF: ローカル下書きが即時表示。バッジ「オフライン」。
- AI ON: 「AIで整える」を押したときだけ API を呼ぶ。結果に「AI」バッジ。
- AI の結果は「元に戻す」でローカル下書きに戻せる。
- 生成中はスケルトン + キャンセル。
- 出力は選択・部分コピーできる。

### 6.2 インタラクション

- **ローカル変換は350msデバウンス**。辞書結果は即時。
- **AI 変換は明示ボタン**。入力中の自動 API 呼び出しはしない。
- **タグチップ**:
  - タップ: 重みスライダー（0.5〜1.5）— タグモードのみ
  - 長押し: 候補の切り替え、無効化、削除
  - ドラッグ: 並び替え
- **候補**: 曖昧な日本語（リボン、立ち、目線など）は複数候補を表示し、ユーザーが選ぶ。選んだ結果は `corrections` に保存。
- **コピー**: 常時表示。押すと「コピーしました」。
- **共有**: `ACTION_SEND` で PixAI / NovelAI / WebUI などの共有先へ。
- **タグ和訳**: タグをタップすると日本語の意味と wiki 要約。
- **履歴**: 直近50件。ピン留め、再編集、削除、JSON エクスポート。
- **設定**: プリセット、品質タグ、AI プロバイダ、API キー、モデル、ローカルモデル、DB 更新、成人向けタグ、プライバシー。

### 6.3 AI 設定画面

```
AI 補完
  [ OFF / ON ]
  プロバイダ: [OpenRouter ▾] [カスタム OpenAI 互換]
  Base URL: https://openrouter.ai/api/v1
  API キー: [••••••••] [保存] [テスト]
  モデル: [google/gemini-3.1-flash-lite ▾] [一覧更新]
  温度: 0.2
  最大出力: 800 tokens
  推定コスト: 約0.1円/回

オフライン AI
  [Gemma 4 E2B (2.58GB)] [ダウンロード]
  [Gemma 4 E4B (3.65GB)] [ダウンロード]  ※12GB以上
  バックエンド: [自動 / GPU / CPU]
```

- API キーは保存後にマスク表示。ログに出さない。
- 「テスト」は短い固定プロンプトで接続確認。
- モデル一覧は OpenRouter の `/models` から取得。価格・コンテキスト長を表示。
- カスタム Base URL は HTTPS を基本。ローカル HTTP は明示許可制（v2）。

### 6.4 オンボーディング

1. ようこそ。PixAI DiT 用の自然文と、タグ形式の両方を作れることを説明。
2. よく使うサービスを選択（既定: PixAI DiT）。
3. 翻訳モデル（日→英、英→日、各約30MB）を Wi-Fi でダウンロード。スキップ可能。
4. AI 補完を使うか（OpenRouter / ローカル / 使わない）。
5. 成人向けタグの注意書き。個人用であること、PixAI 側でブロックされる場合があること。
6. 完了。すぐに日本語を入力できる。

### 6.5 エラー・空状態

- モデル未ダウンロード: 「翻訳モデルをダウンロードすると、辞書にない言葉も変換できます」。
- オフラインで AI モード: 「AI 補完はオフラインでは使えません。ローカル下書きに切り替えます」。
- API キー未設定: 「AI を使うには設定で API キーを入力してください」。
- 残高不足: 「OpenRouter のクレジットを確認してください」。
- レート制限: 「少し待って再試行してください」。
- 候補なし: 「タグが見つかりませんでした。フリーテキストとして残す / 別の言い方にする」。
- 誤マッチ: チップの「×」で削除、訂正を保存。
- ローカルモデルが RAM 不足: 「この端末では E2B を推奨します」。

---

## 7. AI 補完モード

### 7.1 接続方式

- **OpenAI API 互換** の `/chat/completions` を使う。
- 既定: OpenRouter
  - Base URL: `https://openrouter.ai/api/v1`
  - 認証: `Authorization: Bearer <API_KEY>`
  - 任意ヘッダ: `HTTP-Referer`, `X-Title: Prompter`
- カスタム:
  - Base URL とモデル名を自由に設定。
  - Ollama / LM Studio / 自前プロキシを想定。
  - `response_format` 非対応でも動くようフォールバック。
- API キーは **Android Keystore** で保護。`allowBackup="false"`、バックアップ除外。
- API キーをログ・クラッシュレポート・履歴に出さない。

### 7.2 既定モデル候補

2026-10-24 時点の OpenRouter カタログより。モデルは設定で変更できる。

| 用途 | モデル ID | 入力/出力（$ / 1M） | 備考 |
|---|---|---:|---|
| **既定（無料自動）** | `openrouter/free` | 0 / 0 | 無料モデル自動ルーター。structured outputs対応。無料枠のレート制限は選択先依存 |
| **バランス** | `google/gemini-3.1-flash-lite` | 0.25 / 1.50 | 構造化出力対応、1M context |
| **安価** | `google/gemini-2.5-flash-lite` | 0.10 / 0.40 | 構造化出力対応。コスト最小 |
| **高品質** | `openai/gpt-5.6-luna` | 0.20 / 1.20 | 構造化出力対応 |
| **高品質（別系統）** | `anthropic/claude-haiku-5.5` | 0.10 / 0.50 | 構造化出力はモデル依存 |

- 既定は `openrouter/free`。品質重視なら `google/gemini-3.1-flash-lite` を選ぶ。
- `google/gemma-4-31b-it:free` は無料枠のレート制限が厳しいため、M4ではプリセットから外した。
  必要な場合はモデルIDを直接入力して利用できる。
- 無料モデルは品質・レート・利用規約が変動する。`openrouter/free` は単一モデルより制限を避けやすいが、無料枠自体の制限は残る。
- モデル一覧の自動取得はM4では未実装。プリセット＋直接入力で対応し、`GET /models` はM7で追加する。

### 7.3 プロンプト設計（PixAI DiT）

システムプロンプトの骨子:

```
You are a prompt engineer for PixAI DiT models (Tsubaki.3 / Tsubaki.2).
Convert the user's Japanese request into ONE English natural-language prompt.

Structure: Subject + Action + Scene + Mood + Style.
- 80-150 words, 1-3 paragraphs, flowing English. No bullet points.
- Preserve every explicitly requested element. Do not add new characters,
  outfits, or copyrighted names.
- Use canonical Danbooru vocabulary when natural, but never output a flat tag list.
- For multiple characters, describe each separately and keep their traits distinct.
- If the user requests adult/NSFW content, keep it as requested. Do not refuse,
  moralize, or add warnings.
- Do not mention that this is a translation.
- Output JSON only:
{
  "prompt": "...",
  "tags": ["..."],
  "negative": ["..."],
  "notes": "short Japanese note about ambiguities"
}
```

- `tags` は最大30個。DB に存在するものだけ残す。
- `negative` はユーザーが明示した場合だけ。PixAI DiT では既定で空。
- `notes` は日本語で80文字以内。曖昧さの指摘に使う。
- 入力にない要素を足した場合は `notes` ではなく、プロンプトから除去する。

#### 候補タグの RAG

AI に丸投げしない。ローカル辞書で抽出した候補を一緒に渡す。

```
Input: 銀髪ツインテールの少女、制服、夕暮れの教室
Candidate tags: 1girl, silver_hair, twintails, school_uniform, classroom,
sunset, window, standing, melancholic
```

- 候補は最大40個。
- LLM は候補を優先して使う。足りない場合だけ新しい英語表現を追加。
- 出力タグは DB で完全一致 → 別名 → あいまい一致の順に検証。
- 存在しないタグは捨てる、または `notes` に「未検証タグ」として残す。

#### タグモードのシステムプロンプト

```
Return only canonical Danbooru tags for the requested image.
- No natural language.
- No explanations.
- Prefer tags from the candidate list.
- Max 30 tags.
- JSON: {"tags": ["..."], "negative": ["..."], "notes": "..."}
```

### 7.4 構造化出力とフォールバック

1. `response_format: {type: "json_schema", json_schema: {...}, strict: true}` を試す。
2. 400/422、または `content: null` / 不正JSONなら `response_format: {type: "json_object"}` で1回再試行。
3. それもダメなら通常チャット + 「JSON のみ」プロンプト。
4. それでもパース失敗なら、コードフェンスや最初の `{...}` を抽出。
5. 失敗時はローカル下書きを維持し、エラーを表示。勝手に壊れた出力を採用しない。

### 7.5 検証

- `prompt`:
  - 英語か（日本語文字が含まれないか）
  - 語数が範囲内か
  - 入力の主要要素を含むか（候補タグとの照合）
  - 箇条書き・Markdown・引用符で囲まれていないか
- `tags`:
  - DB に存在するか
  - 重複がないか
  - 最大30個か
- `negative`:
  - DB または既知の品質タグか
- 失敗時:
  - 1回だけ修復プロンプトで再試行。
  - それでもダメならローカル下書き + 「AI 出力を採用しませんでした」。

### 7.6 コスト感

- 1回の変換: 入力 800〜1,500 tokens、出力 200〜400 tokens。
- `gemini-3.1-flash-lite` の場合:
  - 入力: 1,500 × $0.25/1M = $0.000375
  - 出力: 300 × $1.50/1M = $0.00045
  - 合計 約 $0.000825 ≈ **0.12円/回**
- 1日10回 × 30日 = 300回で約36円/月。
- 無料モデルは0円だが、レート制限・品質変動・構造化出力非対応に注意。
- OpenRouter のクレジット購入時には手数料がかかる場合がある。実装時に最新規約を確認。

### 7.7 プライバシー

- 既定はオフライン。AI は明示的に ON にしたときだけ送信。
- 送信前に「入力・候補タグ・モデル名」を確認できるプレビューを任意で表示。
- 送信内容（プロンプト）をアプリのログに残さない。
- OpenRouter はプロバイダにルーティングするため、データポリシーはプロバイダ依存。設定画面に注意書きとリンクを置く。
- 成人向けプロンプトは、プロバイダによってはブロック・記録される可能性がある。個人用でも、送信前に警告する。
- API キーは Keystore。`android:allowBackup="false"`。クラッシュログにキー・本文を含めない。
- 履歴・AI キャッシュは端末内のみ。エクスポートはユーザー操作時だけ。

### 7.8 ローカル LLM フォールバック

- AI モードが OFF、または API 失敗時に、Gemma 4 E2B がダウンロード済みならローカルで生成できる。
- ローカルも「AIで整える」ボタンで明示実行。
- 出力形式は同じ JSON（または区切り文字形式）。DB 検証は同じ。
- 生成中は「オンデバイスで生成中」と表示。ネットワーク不要。
- 品質が足りない場合は、ローカル下書き + 「AI で整える」導線に戻す。

---

## 8. MVP スコープ

### Must（これがないと意味がない）

- 日本語入力 → **PixAI DiT 用の英語自然文**（オフライン下書き + AI 補完）
- 日本語入力 → **Danbooru タグ**（辞書 + ML Kit）
- タグチップの表示、削除、重み変更、並び替え（タグモード）
- コピー / 共有
- プリセット（PixAI DiT、PixAI Haruka v2 / SDXL、A1111/ComfyUI、NovelAI）
- オフライン動作（モデルダウンロード後）
- 履歴（直近20〜50件）
- 候補選択と修正の保存
- AI モード（OpenRouter / OpenAI 互換、任意 ON、BYOK）
- 成人向けタグをそのまま扱う設定

### Should（あると強い）

- ローカル LLM（Gemma 4 E2B、任意ダウンロード）
- タグ→日本語
- `ACTION_PROCESS_TEXT` / `ACTION_SEND` 対応
- 品質タグ・ネガティブプロンプトのプリセット
- トークン数表示（CLIP 77トークンの目安）
- カスタム辞書のインポート/エクスポート
- 複数キャラの構造化（Character 1 / Character 2）
- スタイルプリセット（アニメ / 水彩 / 3D / モノクロ）
- AI 出力の長さ選択（短め / 標準 / 詳しめ）

### Won't（MVP ではやらない）

- 画像生成 API 連携
- カスタム IME
- フローティングバブル（`SYSTEM_ALERT_WINDOW`）
- アクセシビリティサービスによる全アプリ翻訳
- クラウド同期・アカウント
- Play Store 配布
- 成人向けタグのフィルタ

### 受け入れ基準

1. 「銀髪ツインテールの少女が、制服姿で夕暮れの教室に立っている。窓から差し込むオレンジ色の光、少し物憂げな表情」→ **80〜150語の英語自然文** が出る。銀髪・ツインテール・制服・教室・夕暮れ・窓・物憂げが含まれる。タグ列になっていない。
2. 同じ入力のタグモード → `1girl, silver_hair, twintails, school_uniform, classroom, sunset, window, standing` に近いタグが出る。
3. 上記を1タップでコピーできる。PixAI DiT は本文のみ、タグモードはタグのみ。
4. 機内モードでも、モデルダウンロード済みならローカル下書きができる。
5. AI モードで OpenRouter の API キーを設定し、モデルを選び、「AIで整える」で自然文が生成できる。
6. AI 出力の存在しないタグは DB 検証で除去される。
7. Gemma 4 E2B をダウンロードし、機内モードで自然文を生成できる（目標15秒以内）。
8. 「サイバーパンク」が `bread` に誤変換されない。
9. 成人向けタグを含む入力が、そのまま出力に残る（フィルタされない）。PixAI 側でブロックされる場合の注意書きが出る。
10. アプリ起動からローカル下書きまで2秒以内（ウォーム時）。
11. API キーがログ・履歴・クラッシュレポートに出ない。
12. `ACTION_PROCESS_TEXT` で他アプリの選択テキストを Prompter に送れる。

---

## 9. マイルストーン

| Phase | 期間 | 内容 | 完了条件 |
|---|---:|---|---|
| M0 調査スパイク | 完了 | データ取得、簡易マッチャー、DB サイズ測定、PixAI/モデル調査 | 調査ファイルが揃う |
| **M1 辞書パイプライン** | 完了 | 別名生成、逆翻訳検証、SQLite ビルド、評価セット | 上位5,000タグの DB と評価レポート。fresh holdout で Recall 0.519 / Precision 0.762 / Invalid 0% |
| **M2 アプリ骨格** | コア完了 | Compose、Trie、PixAI オフライン合成、CI、Kotlin/Robolectric テスト | 日本語→PixAI 自然文がオフラインで動く（APK `0.2.0-m1`） |
| **M3 エディタ / 履歴** | 実装完了 | 自然文/タグ切替、タグチップ、重み、並び替え、削除、コピー、履歴・お気に入り（SQLite） | 編集・履歴がテスト済み（APK `0.3.0-m3`、実機確認待ち） |
| **M4 OpenRouter AI** | 実装完了 | OpenAI 互換クライアント、設定画面、Keystore、JSON Schema、フォールバック、タグ検証、キャッシュ、既定 `openrouter/free` | 実通信確認済み（ユーザー環境、APK `0.4.2-m4`、CIはモック） |
| **M5 ML Kit / タグ和訳** | 実装・実機確認完了 | ML Kit翻訳フォールバック、英語→タグ正規化、タグ→日本語、安定debug署名 | CI成功、実機でモデルDL・機内モード翻訳・上書き更新を確認（APK `0.5.0-m5`） |
| **M6 ローカル LLM** | 実装・CI完了（実機検証待ち） | LiteRT-LM 0.18.1、E2B CPU ダウンロード、SHA-256、クラウド/ローカル切替、失敗時フォールバック | 実機でダウンロード・初期化・機内モード生成・品質/負荷を確認（APK `0.6.0-m6`） |
| **M7 統合・仕上げ** | 3〜4日 | 選択メニュー、共有、オンボーディング、設定、署名、リリース | 実機で1週間使える |

**合計: 約5〜6週間**（1日1〜2時間、週末多めの想定）。M1 を先にやるのが重要。アプリから作ると、辞書品質の問題で手戻りする。

### 9.1 最初の1週間（M1）の具体タスク

| Day | タスク | 成果物 |
|---|---|---|
| 1 | 公開 CSV の取得、上位5,000タグの抽出、DB スキーマ確定 | `build_db.py` の雛形、タグ一覧 |
| 2 | 500タグで LLM 別名生成のプロンプトを試す | 別名候補 JSON、品質メモ |
| 3 | 逆翻訳検証、重複解消、Tier 付け | 検証済み別名 DB |
| 4 | 5,000タグに拡大、SQLite ビルド | `promptdb-v1.sqlite` |
| 5 | 200プロンプトで Recall / Precision / Invalid rate を測る | 評価レポート、Go/No-Go 判断 |

- OpenRouter の API キーが無い間は、既存の手作業訳 + 機械訳 + 手動キュレーションでパイプラインを完成させる。キーが入り次第、LLM 別名生成を回す。
- Go の条件: 辞書のみで Recall 50% 以上、Precision 75% 以上、Invalid rate 2% 未満。届かなければ、別名生成のプロンプトかデータソースを見直す。
- PixAI 自然文の評価は、M2 のオフライン合成で20例を人手評価（5段階で3.5以上）。

### 9.2 M6 のローカルモデル実装

実装済み（`0.6.0-m6`、CI成功、実機検証待ち）:

1. LiteRT-LM `0.18.1` の Kotlin API を組み込む。
2. E2B の CPU 版（2,588,147,712 bytes）を固定revision・SHA-256付きで任意ダウンロード。
3. 設定で「クラウド（OpenRouter）」と「ローカルLLM（端末内）」を切り替える。
4. CPU / 4 threads、4,096 context、768 output tokens で生成する。
5. モデル削除・再ダウンロード・SHA-256 検証・Wi-Fi 限定を実装する。
6. 失敗・拒否・日本語混入時はオフライン結果を保持し、AIカードにエラーを出す。

実機検証待ち:

1. arrows Alpha で DownloadManager の完走と Hugging Face リダイレクトを確認。
2. 初回ロード時間、生成時間、RAM、発熱、バッテリーを測る。
3. 機内モードで生成できることを確認。
4. 成人向け入力、複数キャラ、風景、日常シーンで拒否率と品質を確認。
5. 30プロンプト程度で品質・速度・メモリを測り、Go/No-Go を決める。

M6.1以降:

- GPU 版（2.0GB）を試し、Mali-G615 で動くか確認。失敗時は CPU へフォールバック。
- E2B が目標に届けば既定のオフライン AI に。届かなければベータ扱い。
- 余裕があれば E4B（3.65GB）を 12GB 端末限定で追加。
- 生成が遅い場合、`sendMessageAsync` でストリーミング表示。

---

## 10. 評価方法

### 10.1 評価セット

- Danbot データセット（6.33M の日本語キャプション↔タグ）から、タグ数が5〜20個の投稿を200件サンプル。
- 元のタグを正解とし、日本語キャプションを入力にする。
- 手動で20件を追加（構図、照明、スタイル、複数キャラ、成人向けタグ、ネガティブ表現）。
- PixAI DiT 用に、要素リスト付きの30件を別途作成。

### 10.2 指標

| 指標 | 意味 | 目標 |
|---|---|---|
| Tag Recall | 正解タグのうち出力に含まれた割合 | 辞書のみ 60% / ML Kit 併用 75% / AI 85% |
| Tag Precision | 出力タグのうち正解に含まれる割合 | 80% 以上 |
| Invalid rate | Danbooru に存在しないタグの割合 | 2% 未満 |
| PixAI Fidelity | 元の要素をどれだけ保持したか（人手 1〜5） | オフライン 3.0 / E2B 3.5 / AI 4.0 |
| English quality | 英文として自然か（人手 1〜5） | 3.5 以上 |
| Latency | 入力から結果表示まで | 辞書 300ms / ML Kit 1.5s / AI 3s / E2B 15s |
| Correction count | ユーザーが手で直したタグ数 | 1プロンプトあたり2個以下 |

### 10.3 テスト

- JVM ユニットテスト: 正規化、Trie、英語→タグ正規化、構文整形、JSON パース。
- ゴールデンテスト: 100入力の期待タグ（代替候補を許容）。
- PixAI 自然文テスト: 30入力の要素リストと人手評価。
- 実機テスト: ML Kit のモデルダウンロード、機内モード、共有、クリップボード、E2B のダウンロード/生成。
- ドッグフーディング: 1週間、実際に PixAI で画像生成して手直しを記録。

---

## 11. リスクと対策

| リスク | 影響 | 対策 |
|---|---|---|
| PixAI DiT のプロンプト仕様が変わる | プリセットが陳腐化 | プリセットをバージョン管理。公式ドキュメントを定期確認 |
| 自然文が汎用翻訳っぽくなる | 商品価値が下がる | `PromptPlan` + 公式の Subject/Action/Scene/Mood/Style を強制。人手評価 |
| 日本語→タグの辞書品質が低い | タグモードの価値が下がる | 上位5,000タグを LLM + 逆翻訳で高品質化。ユーザー修正を学習 |
| 短いカタカナ・漢字の誤マッチ | 誤変換が目立つ | 境界判定、トークナイザ、候補UI、信頼度閾値 |
| 汎用 MT がタグを返さない | 辞書外で品質が落ちる | 英語→タグ正規化層（別名、単複、曖昧一致）を必ず挟む |
| LLM の幻覚タグ | 生成に使えないタグが混ざる | DB 照合、JSON Schema、temperature 0.2、候補のみ提示 |
| LLM が入力にない要素を足す | 意図しない画像になる | 候補タグ RAG、検証、`notes`、再生成 |
| AI が成人向け表現を拒否する | タグが欠ける | システムプロンプトで拒否禁止。プロバイダ依存と明記。ローカルモデルは検閲なし |
| OpenRouter のモデルが廃止される | AI モードが止まる | モデル一覧取得、複数候補、カスタム Base URL |
| API キー漏洩 | 不正利用 | BYOK、Keystore、ログ禁止、バックアップ除外 |
| オンデバイスモデルのサイズ/RAM | 端末を選ぶ | E2B を既定。E4B は 12GB 限定。RAM/空き容量チェック |
| E2B の品質が低い | ローカル AI の価値が下がる | 30例で評価。不足なら辞書 + ML Kit を既定にし、E2B はベータ |
| Mali GPU で LiteRT-LM が動かない | 速度低下 | CPU フォールバック。GPU 版は任意 |
| 2.6GB ダウンロードが失敗する | 使えない | DownloadManager、再開、SHA-256、Wi-Fi 限定 |
| ML Kit が Google Play services に依存 | de-Googled 端末で翻訳できない | 辞書のみでも動く。設定で「翻訳なし」 |
| Android のクリップボード制限 | 自動翻訳ができない | 自動読み取りしない。貼付ボタン、共有、選択メニュー |
| データのライセンス | 公開時に問題 | 個人利用に限定。公開前に Danbooru/各データの条件を確認 |
| 成人向けタグの法的・倫理的問題 | 公開時に問題 | 個人用 APK。公開しない。設定で注意書き |
| Termux でビルドできない | 開発が止まる | GitHub Actions で APK ビルド（LocalTTS-App と同じ） |

---

## 12. コスト

| 項目 | 費用 | 備考 |
|---|---:|---|
| 開発 | 0円 | 自分の時間 |
| GitHub Actions | 0円 | 無料枠内 |
| ML Kit | 0円 | オンデバイス |
| OpenRouter | 0〜100円/月 | 0.1〜1円/回。個人利用なら小さい。無料モデルも可 |
| ローカル LLM | 0円 | モデル 2.6〜3.7GB のダウンロードのみ |
| データ | 0円 | 公開データ |
| Play Store | 0円 | 公開しない |
| プロキシ | 0円 | 個人用なので不要 |

---

## 13. ビルド・配布

### 13.1 方針

- **Termux では APK をビルドしない**。GitHub Actions でビルドし、Artifact をダウンロードして `adb install`。
- 既存の `LocalTTS-App` と同じ構成を流用:
  - JDK 21
  - Gradle 9.5（`gradle/actions/setup-gradle@v6`）
  - AGP 9.3.x
  - Kotlin 2.4.x + Compose Compiler
  - Compose BOM 2026.09.00
  - compileSdk 37 / targetSdk 36 / minSdk 29
- ABI は **arm64-v8a** を基本。LiteRT-LM のネイティブライブラリも arm64。
- デバッグ APK は universal でもよいが、リリースは arm64 のみ。

### 13.2 ワークフロー

```yaml
name: build-apk
on: workflow_dispatch
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - uses: gradle/actions/setup-gradle@v6
        with:
          gradle-version: '9.5.0'
      - run: gradle assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: prompter-debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

### 13.3 Termux 側の手順

```bash
gh workflow run build-apk.yml
gh run watch
gh run download -n prompter-debug-apk
adb install -r app-debug.apk
```

### 13.4 署名

- 最初は debug 署名。
- 配布前に `keytool` で release keystore を作成し、GitHub Secrets に Base64 で保存。
- `versionCode` を上げる。debug 署名と release 署名は異なるため、release 版は debug 版に上書きインストールできない。移行時は再インストールが必要（履歴をエクスポートしておく）。

### 13.5 リポジトリ構成（案）

```
prompter/
├── app/
│   ├── src/main/java/.../
│   │   ├── ui/            # Compose screens
│   │   ├── domain/        # converter, matcher, planner, formatter
│   │   ├── data/          # Room, repository, ML Kit
│   │   ├── ai/            # OpenAI-compatible client
│   │   └── local/         # LiteRT-LM, model manager
│   ├── src/main/assets/   # seed DB
│   └── src/test/          # unit tests
├── tools/
│   ├── build_db.py        # データパイプライン
│   ├── spike_match.py     # 調査用（残す）
│   └── eval.py            # 評価
├── research/
│   ├── DECISIONS.md
│   ├── PIXAI_DIT.md
│   ├── MODEL_SELECTION.md
│   ├── SPIKE.md
│   └── data/              # gitignore
├── PLAN.md
├── README.md
└── .github/workflows/build-apk.yml
```

### 13.6 権限とプライバシー

- 権限は **INTERNET** のみ（ML Kit モデルダウンロード、任意の AI モード、モデル/DB 更新）。
- ストレージ権限は不要。インポート/エクスポートは SAF を使う。
- `SYSTEM_ALERT_WINDOW`、アクセシビリティサービス、連絡先、位置情報は使わない。
- クリップボードはユーザーが「貼付」「コピー」を押したときだけ読み書きする。
- 既定はオフライン。AI モードは設定で明示的に有効化し、送信内容とプライバシーを表示する。
- API キーは Android Keystore で保護し、`android:allowBackup="false"` またはバックアップ除外ルールを設定する。
- クラッシュログにプロンプト本文を含めない。
- 成人向けタグはフィルタしないが、初回に注意書きを出す。

---

## 14. 確定事項と未決事項

### 14.1 確定（2026-10-23）

1. **対象**: PixAI DiT（Tsubaki.3 / Tsubaki.2）を第一。タグ形式も残す。
2. **AI**: OpenAI API 互換。既定は OpenRouter。任意 ON、BYOK。
3. **オフライン AI**: Gemma 4 E2B を本命。E4B は 12GB 端末向けの任意。
4. **配布**: 自分用 APK。arrows Alpha。
5. **成人向けタグ**: そのまま。フィルタしない。
6. **アプリ名**: 暫定 `Prompter`。
7. **IME**: MVP では作らない。共有 / 選択メニューで代替。

### 14.2 未決

1. ローカル LLM を最初の APK に入れるか、コア完成後の追加にするか。  
   推奨: M2〜M5 を先に完成させ、M6 で E2B を追加。
2. OpenRouter の API キーをいつ設定するか。  
   M1 の別名生成と M4 の AI テストで必要。無い間は手作業訳 + 機械訳で進める。
3. アプリ名を `Prompter` のままにするか。  
   実装には影響しない。
4. PixAI DiT の自然文の既定語数。  
   推奨: 80〜150語。M2 の評価で調整。
5. 品質タグを PixAI DiT の既定で入れるか。  
   推奨: オフ。必要な人だけ設定で ON。

---

## 15. 参考資料

### PixAI

- Tsubaki.3: https://docs.pixai.art/docs/models/tsubaki3
- Tsubaki.3 Prompt Templates: https://docs.pixai.art/docs/models/tsubaki3-prompts
- Tsubaki.3 Prompts in Action: https://docs.pixai.art/docs/models/tsubaki3-workflow
- Other PixAI Models: https://docs.pixai.art/docs/models/other-models
- Prompt Basics: https://docs.pixai.art/docs/prompts/prompt-basics
- Advanced Prompting: https://docs.pixai.art/docs/prompts/prompt-advanced
- DiT Model Prompt Writing Guide: https://blog.pixai.art/en/dit-model-prompt-writing-guide/

### AI / オンデバイス

- OpenRouter Structured Outputs: https://openrouter.ai/docs/guides/features/structured-outputs
- OpenRouter Models API: https://openrouter.ai/docs/api/api-reference/models/list-all-models-and-their-properties
- LiteRT-LM Overview: https://developers.google.com/edge/litert-lm/overview
- LiteRT-LM Android Kotlin: https://developers.google.com/edge/litert-lm/android
- Gemma 4 E2B (LiteRT): https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Gemma 4 E4B (LiteRT): https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm
- Gemma 4 model card: https://huggingface.co/google/gemma-4-E2B-it
- Gemma 4 docs: https://ai.google.dev/gemma/docs/core
- Qwen3-4B-Instruct-2507: https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507
- DanbotNL-2408-260M: https://huggingface.co/dartags/DanbotNL-2408-260M

### データ / 既存ツール

- ML Kit Translation: https://developers.google.com/ml-kit/language/translation/android
- a1111-sd-webui-tagcomplete: https://github.com/DominikDoom/a1111-sd-webui-tagcomplete
- booru-japanese-tag: https://github.com/boorutan/booru-japanese-tag
- ComfyUIJapaneseTagAutoCompleteCSV: https://github.com/himamon/ComfyUIJapaneseTagAutoCompleteCSV
- Tag Recipe: https://pyu224.github.io/tagdb-updater/
- Danbooru wiki dataset: https://huggingface.co/datasets/isek-ai/danbooru-wiki-2024
- Gboard の入力中翻訳: https://support.google.com/gboard/answer/7421372
- NovelAI の重み構文: https://docs.novelai.net/en/image/strengthening-weakening/

（URL は 2026-10-23 時点で確認）
