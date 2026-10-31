# Prompter

日本語で情景を書くだけで、アニメ画像生成向けの**英語プロンプト**と **Danbooru タグ**に変換する Android アプリです。PixAI などの自然文入力に対応した画像生成サービスに貼り付けて使います。

```
長い黒髪の少女が、青いドレスで夜の街の木の椅子に座っている
        ↓
An anime-style illustration of a solo girl with long black hair.
The character is wearing a blue dress. The character is sitting.
The scene is set in a city, on a wooden chair, and at night. ...
```

## 特長

- **オフラインで動く辞書変換** — Danbooru の投稿数上位 10,000 タグと日本語別名を端末内に収録。通信なしで候補を提示します。
- **自然文のプロンプト生成** — タグを並べるだけではなく、情景が伝わる英文に組み立てます。
- **キャラクター指定** — 版権キャラクターモードをオンにすると、作品名とキャラクター名から候補を検索できます。候補をタップして確定する方式です。
- **AI 補完（任意）** — 辞書で拾えない表現を、OpenRouter（クラウド）または端末内 AI（Gemma 4 E2B）で補えます。どちらも設定から有効化します。
- **履歴とお気に入り** — 変換したプロンプトを端末内に保存し、再利用できます。

## 動作環境

| 項目 | 要件 |
| --- | --- |
| OS | Android 10（API 29）以上 |
| CPU | arm64-v8a（64bit ARM）または x86_64（エミュレータ・ChromeOS など） |
| メモリ | 通常の利用は 3GB 以上を推奨 |
| 端末内 AI | 8GB 以上の RAM と約 2.6GB の空き容量 |
| ストレージ | APK 約 125MB（AI モデルと翻訳モデルは別途） |

32bit 端末（armeabi-v7a / x86）には対応していません。端末内 AI を使わなくても、辞書変換と翻訳は利用できます。

## インストール

1. [Releases](https://github.com/shiokunn/prompter/releases) から最新の `Prompter-x.y.z.apk` をダウンロードします。
2. 端末の設定で、提供元不明のアプリのインストールを許可します。
3. APK を開いてインストールします。

公開版のアプリケーション ID は `com.prompter.app.release` です。開発版（`com.prompter.app`）とは別アプリとして共存でき、データも共有しません。公開版の署名鍵が変わると上書き更新できないため、今後も同じ鍵で署名して配布します。

## 使い方

1. 「情景」に日本語で状況を書きます。人物・服装・場所・時間・ポーズなどを含めると安定します。
2. 特定のキャラクターを出したい場合は「キャラクター」の丸いボタンで版権キャラクターモードをオンにし、名前を入力して候補をタップします。
3. 「プロンプトを作成」を押すと英文プロンプトが生成されます。
4. 「コピー」を押し、画像生成サービスのプロンプト欄に貼り付けます。

辞書にない語はそのまま残るか、翻訳・AI 補完の対象になります。意図しない語が混ざった場合は、情景の書き方を変えるか、キャラクター候補を確定してから再生成してください。

## 設定

- **生成方法** — 辞書のみ / OpenRouter / 端末内 AI を切り替えます。
- **OpenRouter** — API キーを入力するとクラウド補完を利用できます。キーは端末内で暗号化して保存されます。
- **端末内 AI** — Gemma 4 E2B（約 2.6GB）をダウンロードすると、通信なしで補完できます。SHA-256 で破損を検証します。
- **翻訳** — 日本語と英語の翻訳モデルを ML Kit からダウンロードします。初回のみ通信が必要です。
- **辞書** — 収録タグ数とバージョンを確認できます。

## プライバシー

- 入力した情景と生成結果は端末内にのみ保存されます。解析や利用統計の送信は行いません。
- OpenRouter を有効にした場合のみ、入力の一部が設定した API へ送信されます。
- 端末内 AI と辞書変換はオフラインで動作します。
- Android の自動バックアップは無効です。機種変更時の引き継ぎは行われません。

## ビルド

必要なもの: JDK 21、Android SDK 37（build-tools 37.0.0）、Python 3.11 以上。

```bash
# 単体テスト（辞書パイプラインと Kotlin）
python3 -m unittest tools.test_m1 -v
./gradlew testDebugUnitTest

# 開発用 APK
./gradlew assembleDebug
```

`app/src/main/assets/promptdb.sqlite` は `tools/build_m1.py` で生成した辞書です。再生成する場合は元データを `research/data/` に配置してください。

### リリース署名

公開版は GitHub Actions の `release-apk` ワークフローでビルドします。次の Secrets を登録しておく必要があります。

| Secret | 内容 |
| --- | --- |
| `PROMPTER_KEYSTORE_BASE64` | リリース用 keystore の Base64 |
| `PROMPTER_STORE_PASSWORD` | keystore のパスワード |
| `PROMPTER_KEY_ALIAS` | 鍵のエイリアス |
| `PROMPTER_KEY_PASSWORD` | 鍵のパスワード |

`v0.10.1` のようなタグを push すると、テスト・署名・APK 検証を経て GitHub Release が作成されます。ワークフローはアプリケーション ID、同梱 ABI、16KB ページ境界への整合、署名を検証します。

## テスト

- `tools/test_m1.py` — 辞書マッチング、タグ選定、プロンプト組み立ての回帰テスト。
- `app/src/test/` — Robolectric を使った Compose UI とロジックの単体テスト。
- `app/src/androidTest/` — 実機・エミュレータでの起動スモークテスト。`device-matrix` ワークフローが Android 10〜16 の x86_64 エミュレータで実行します。

## 既知の制限

- 辞書は投稿数上位 10,000 タグです。それ以外の語は翻訳か AI 補完に依存します。
- キャラクター検索は同梱のキャラクター辞書に収録された作品・人物が対象です。収録外の名前は候補に出ません。
- 端末内 AI は CPU 実行のため、生成に数十秒かかる場合があります。端末や空きメモリによっては起動できません。
- 32bit 端末と Android 9 以下はサポート対象外です。

## 謝辞とライセンス

本アプリのソースコードは [MIT License](LICENSE) で公開しています。

同梱・依存する主なソフトウェアとデータ:

- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM) と [Gemma 4 E2B](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)（Apache-2.0）
- [ML Kit Translation](https://developers.google.com/ml-kit/language/translation)（Google の利用条件に従います）
- [Jetpack Compose](https://developer.android.com/jetpack/compose)、[Room](https://developer.android.com/jetpack/androidx/releases/room)（Apache-2.0）
- Danbooru のタグ情報を元にした辞書データ。タグ名と別名は Danbooru コミュニティの成果物です。

それぞれのライセンスは同梱物の条件に従います。
