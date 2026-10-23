# PixAI DiT のプロンプト仕様（2026-10-23 調査）

## 1. 結論

- PixAI の DiT 系（Tsubaki.3 / Tsubaki.2 / Tsubaki.1）は、**タグ列より英語の自然言語**に向いている。
- 基本式は **Subject + Action + Scene + Mood + Style**。
- 1〜3段落の説明的な英文が推奨。商用例は 300 語超もあるが、通常利用は **60〜180語** で十分。
- 品質タグ（`masterpiece, best quality` など）は使えるが、DiT では必須ではない。自然文を壊さない範囲で任意に入れる。
- ネガティブプロンプトはモデル・モードによる。
  - Tsubaki.2 は Lite / Standard でカスタム不可、Pro / Ultimate のみ。
  - PixAI には既定のネガティブがあるため、アプリでは**既定で非表示**にする。
- Tsubaki.3 は画像内の日本語文字を描けるが、**プロンプト理解が日本語対応という意味ではない**。出力は英語にする。

## 2. 公式ドキュメントから確認したこと

### 2.1 モデル一覧

PixAI Docs の "Other PixAI Models" より:

| モデル | アーキテクチャ | プロンプト |
|---|---|---|
| Tsubaki.3 | DiT | 自然言語 |
| Tsubaki.2 | DiT | 自然言語 |
| Tsubaki.1 | DiT | 自然言語 |
| Haruka v2 | SDXL | タグベース |
| Hoshino v2 / v1 | SDXL | タグベース |
| Reference Pro | DiT | 自然言語（画像編集） |

- Tsubaki.2 は 2026年3月リリース。マルチキャラクター、スタイルプリセット、4モード（Lite / Standard / Pro / Ultimate）。
- Tsubaki.3 は最新のフラッグシップ。マルチキャラクター、漫画、ポスター、複雑な構図、照明、フォトリアルまで対応。
- Tsubaki.2 のネガティブは Pro / Ultimate のみ。Lite / Standard はシステム既定。

### 2.2 プロンプトの基本式

PixAI Docs "Prompt Basics" より:

```
Prompt = Subject + Action + Scene
```

上級:

```
Prompt = Subject + Action + Scene + Mood + Style
```

- Subject: キャラクター、服装、特徴
- Action: 何をしているか。風・雨・煙などの環境の動きも含む
- Scene: 場所
- Mood: 時間帯、光、空気感
- Style: 画風、色、レンズ、被写界深度

公式例:

```
pixai_mio wearing a flowing white summer dress, turning around as the breeze
catches her skirt and makes it billow outward. She stands in a sunlit sunflower
field with golden petals stretching endlessly toward the horizon, afternoon
light warming the scene.
```

上級例では、これに "golden hour warmth", "dreamy haze", "painterly anime style",
"soft pastel tones", "cinematic shallow depth of field" が加わる。

### 2.3 構図・ショット

"Advanced Prompting" より:

- ショットタイプを明示すると情報量を制御できる。
  - Medium Shot: 腰上、バランス型
  - Cowboy Shot: 太もも上、動きと表情
  - Wide Shot: 風景重視
  - Close Up: 表情・目・手
  - Choker Shot: 顔だけ、感情のアップ
- キャラクター主役なら縦長、景色主役なら横長。
- 矛盾（立ち + 座り、青いドレス + 赤いドレス、横向き + 表情を見せたい）を避ける。
- 変更するときは、古い属性を残さない。

### 2.4 品質・スタイルブロック

"Tsubaki.3 Prompt Templates" より、公式のスタイルブロック例:

- `masterpiece, best quality, amazing quality, Anime Screencap, anime coloring`
- `Hyper-detailed anime illustration in 8K, vivid anime coloring, ...`
- `Extreme chiaroscuro lighting, crushingly deep blacks, ...`
- `pure flat coloring, no shading, ...`
- `crisp clean lineart, sharp outlines, flat cel-shading, ...`
- `Classic monochrome manga style ...`
- `3d, blender (medium), game cg, official art, ...`
- `8-bit pixel art, extremely limited palette, ...`

→ アプリでは「スタイル」をプリセット化し、ユーザーが選んだ場合だけ自然文の末尾に足す。
品質タグを大量に足すと自然文が壊れるため、既定ではオフ。

### 2.5 ネガティブ

PixAI Docs "Prompt Basics" より:

- PixAI モデルには既定のネガティブがある。
- Tsubaki.2 のカスタムネガティブは Pro / Ultimate のみ。
- 既定の例:
  ```
  lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit,
  fewer digits, cropped, worst quality, low quality, jpeg artifacts, signature,
  watermark, blurry
  ```
- シーン別の追加例:
  - キャラ: `bad anatomy, bad hands, missing fingers, extra limbs, deformed`
  - 顔アップ: `bad face, ugly face, deformed eyes, asymmetric eyes`
  - 風景: `human, person, text, watermark`
  - アニメ調: `photorealistic, 3d render, photo`
  - フォトリアル: `anime, cartoon, illustration, drawing`

→ アプリの PixAI DiT モードでは、ネガティブ欄を「詳細設定」に隠し、既定は空にする。
→ ユーザーが明示した場合だけ、PixAI の既定候補を提案する。

## 3. アプリへの実装方針

### 3.1 出力モード

| モード | 出力 | 既定 |
|---|---|---|
| PixAI DiT | 英語の自然文（60〜180語、1〜3段落） | ◎ |
| PixAI DiT + タグ | 自然文 + 末尾に LoRA トリガー/主要タグ | ○ |
| タグ | Danbooru canonical tags | ○ |
| 和訳 | タグ → 日本語 | ○ |

- PixAI DiT モードでは **重み構文 `(tag:1.2)` を出さない**。
- PixAI DiT モードでは **タグを主役にしない**。タグは「材料」として内部保持し、必要なときだけ表示する。
- コピーは自然文だけを基本。タグ付きコピーは別ボタン。

### 3.2 自然文の組み立て

AI モード:

```
Subject + Action + Scene + Mood + Style
```

をシステムプロンプトで指示し、以下の JSON を返させる。

```json
{
  "prompt": "A silver-haired girl ...",
  "tags": ["1girl", "silver_hair", "twintails"],
  "negative": [],
  "notes": "曖昧な点があれば日本語で短く"
}
```

- `prompt` は英語のみ。日本語混入をチェック。
- 80〜150語を既定。`短め` 40〜70語、`詳しめ` 150〜250語。
- 改行は 1〜3 段落。箇条書き禁止。
- ユーザーが指定していない要素を足さない。
- 複数キャラはキャラごとに分けて書く。
- 成人向け表現は、ユーザーの入力にある場合だけ、そのまま英語化する。拒否・説教をしない。

オフラインモード:

- 辞書で抽出した概念をスロットに埋める。
- 未一致の日本語は ML Kit で英訳し、形容詞/副詞として挿入。
- テンプレート例:

```
A {subject} with {hair}, {eyes}, wearing {outfit}, {pose} in {scene}.
{lighting}. {mood}. Rendered in {style}, {composition}.
```

- 品質は AI モードより落ちる。UI に「オフライン下書き」と表示し、AI で整える導線を出す。

### 3.3 タグモード

- PixAI Haruka v2 / SDXL / Pony / Illustrious / A1111 / ComfyUI 向け。
- Danbooru canonical tag をカンマ区切り。
- 重みは A1111 なら `(tag:1.2)`、NovelAI なら `{tag}` / `1.2::`。
- PixAI Haruka v2 は SDXL なのでタグベース。ただし PixAI の DiT とは別プリセットにする。

### 3.4 避けること

- PixAI DiT にタグ列だけを渡して「自然文モード」と称すること。
- 品質タグを大量に付けること。
- ユーザーが指定していないキャラクター名・版権名を勝手に足すこと。
- 成人向けタグを勝手に検閲すること（個人用・設定で制御）。
- ネガティブを常に表示すること（PixAI では既定で不要な場合が多い）。

## 4. 未検証・確認したいこと

1. Tsubaki.3 が日本語プロンプトをどこまで理解するか（公式は英語推奨。アプリは英語出力で回避）。
2. Tsubaki.3 のカスタムネガティブ対応（Tsubaki.2 は Pro / Ultimate のみ）。
3. PixAI のプロンプト最大長（長すぎる自然文が切られないか）。
4. PixAI の LoRA トリガーワードを自然文に混ぜる最適な位置。
5. PixAI 側の NSFW フィルタと、どのタグがブロックされるか。
6. Tsubaki.3 の「Style」プリセットと、アプリ側のスタイル文が競合しないか。

## 5. 参考 URL

- Tsubaki.3: https://docs.pixai.art/docs/models/tsubaki3
- Tsubaki.3 Prompt Templates: https://docs.pixai.art/docs/models/tsubaki3-prompts
- Tsubaki.3 Prompts in Action: https://docs.pixai.art/docs/models/tsubaki3-workflow
- Other PixAI Models: https://docs.pixai.art/docs/models/other-models
- Prompt Basics: https://docs.pixai.art/docs/prompts/prompt-basics
- Advanced Prompting: https://docs.pixai.art/docs/prompts/prompt-advanced
- PixAI Blog DiT guide: https://blog.pixai.art/en/dit-model-prompt-writing-guide/

（2026-10-23 時点）
