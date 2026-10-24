# M4 — OpenRouter AI補完

- 日付: 2026-10-24
- 状態: **実装完了（ユーザー環境で実通信確認済み。自動テストはモック）**
- 対象: `app/src/main/java/com/prompter/app/` / `app/src/test/java/com/prompter/app/`
- APK: `Prompter-0.4.1-m4-debug.apk`（versionCode 5）

## 目的

オフライン変換を土台に、必要なときだけOpenRouterのモデルで自然文とタグを整える。
APIキーはBYOKとし、アプリ・履歴・ログ・APK・CIのどこにも平文で残さない。

## 実装

### 設定画面（`SettingsScreen.kt` / `AiSettings.kt`）

- 右上の「設定」からAI設定を開く。
- OpenRouter APIキーを入力（既存キーは表示しない。変更する場合だけ再入力）。
- API Base URLを変更可能。既定は `https://openrouter.ai/api/v1`。
- モデルIDを直接入力できる。プリセット:
  - 既定: `openrouter/free`（無料モデル自動ルーター）
  - バランス: `google/gemini-3.1-flash-lite`
  - 低コスト: `google/gemini-2.5-flash-lite`
  - 高品質: `openai/gpt-5.6-luna`
- `google/gemma-4-31b-it:free` は無料枠のレート制限が厳しいためプリセットから外した。モデルIDの直接入力では利用できる。
- 旧既定のGemini 3.1 Flash Lite、または旧無料プリセットのGemmaを保存している場合は、初回起動時に一度だけ `openrouter/free` へ移行する。カスタムモデルは維持する。
- モデルIDは2026-10-24時点のOpenRouter `/api/v1/models` で存在を確認済み。`openrouter/free` もstructured outputs対応。
- APIキー削除は確認ダイアログ付き。

### キーの保存

- Android KeystoreのAES-256-GCM鍵でAPIキーを暗号化。
- 暗号文だけをアプリ専用SharedPreferencesに保存。
- `android:allowBackup="false"` のためバックアップ経由でも持ち出さない。
- 履歴DB、AIキャッシュ、ログ、クラッシュレポートにはキーを保存しない。
- 復号できない場合はキーを破棄し、再登録を促す。

### OpenRouterクライアント（`OpenRouterClient.kt`）

- `POST {baseUrl}/chat/completions`。
- ヘッダー:
  - `Authorization: Bearer <key>`
  - `Content-Type: application/json`
  - `HTTP-Referer` / `X-Title`
- 既定はJSON Schemaのstructured outputs:
  - `pixai_prompt`: string
  - `tags`: string[]
  - `required` / `additionalProperties: false`
- `json_schema` 非対応モデル向けに `json_object` へ1回フォールバック。
- Markdownのコードフェンスで返されたJSONも抽出。
- 401 / 402 / 403 / 404 / 429 / 5xxを日本語メッセージに変換。
- APIキーやリクエスト本文はログ・例外メッセージに含めない。

### プロンプト

- 80〜150語（目標約110語）。
- `Subject + Action + Scene + Mood + Style`。
- オフライン辞書タグを優先し、存在しないタグを作らないよう指示。
- 成人向けタグをフィルタ・検閲しないよう指示。
- 出力はJSONのみ。

### 検証（`AiPromptValidator.kt`）

- AIタグを小文字・アンダースコア形式に正規化。
- `(tag:1.2)` 形式の重みはタグ名に戻す。
- 重複を除去。
- 同梱辞書（5,003タグ）に存在するタグだけを採用。
- 辞書外タグは除去し、UIに件数を表示。
- 有効タグが0件の場合はオフラインタグへフォールバック。
- 自然文の語数が80〜150語の範囲外なら警告を表示（生成自体は保持）。

### キャッシュ（`AiCache.kt`）

- API結果を端末内SharedPreferencesに最大50件保存。
- キーは `baseUrl + model + 入力 + オフラインタグ + 辞書外テキスト` のSHA-256。
- APIキーはキャッシュに含めない。
- 同じ条件の再実行はAPIを呼ばず、キャッシュから反映。

### UI

- オフライン変換後に「AIで整える」を表示。
- キー未設定時は設定画面への導線を表示。
- オフラインでタグが0件でもAI補完を実行可能。
- AI結果の語数、辞書外タグ除去、キャッシュ利用を表示。
- AI結果の自然文は履歴にも保存。タグ削除時は自然文をオフライン再合成へ戻す。

## テスト

CIで以下を実行し、すべて成功した。

```bash
python3 -m unittest tools.test_m1 -v
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

Kotlinテストは合計25件:

- 既存13件（`PromptComposerTest` / `M1MatcherTest` / `TagEditingTest` / `HistoryStoreTest`）
- `AiPromptValidatorTest`（4件）
  - タグ正規化
  - 辞書外タグ・重複の除去
  - 語数範囲の判定
  - 成人向けタグの保持
- `OpenRouterClientTest`（4件、MockWebServer）
  - JSON Schemaリクエストと応答パース
  - `json_object` フォールバック
  - 401エラー処理（キー非漏えい）
  - MarkdownフェンスのJSON抽出
- `AiCacheTest`（1件）
  - 保存・取得・キー分離・クリア
- `AiSettingsTest`（3件）
  - 既定が無料ルーターであること
  - 旧既定モデルの一回限りの移行
  - カスタムモデルの維持

## 検証結果

- GitHub Actions run: <https://github.com/shiokunn/prompter/actions/runs/40052200048>
- `Run M1 matcher tests`: 成功
- `Run Kotlin unit tests`: 成功
- `Build debug APK`: 成功
- APK: 約31MB
- 共有ストレージ: `/storage/emulated/0/Download/Prompter/Prompter-0.4.1-m4-debug.apk`
- SHA-256: `4262ac3a379c740503e54d8c050f094eb07ce25b6959e5fa8c4c9dca98ab9c14`
- debug署名。debug keystore cacheが残っている限り上書きインストール可能。
- ユーザー環境で実機インストール・OpenRouter実通信に成功したとの報告あり。CIでは実キーを使わずMockWebServerで検証している。

## 未完了・既知の制約

- 実キーでの実通信はユーザー環境で確認済みだが、CIには実キーを置かずMockWebServerで検証している。
- 当環境からadb接続できないため、実機UIの自動確認は未実施。ユーザー環境でのインストール・実通信は確認済み。
- ストリーミング、トークン数・料金表示、接続テスト、モデル自動取得は未実装。
- AIタグは同梱する上位5,003タグの範囲でしか検証できない。それ以外の正しいDanbooruタグも除去される。
- 自然文の語数が範囲外でも警告のみで採用する。
- OpenRouter側のモデレーションはアプリでは制御できない。特に `openai/gpt-5.6-luna` はモデレーション有効。成人向けタグ自体はアプリ内でフィルタしない。
- `openrouter/free` は無料モデルを自動選択するため、品質・速度・モデレーション・レート制限は選択先に依存する。単一のGemma 4 31Bより制限を避けやすいが、無料枠自体の制限は残る。
- カスタムBase URLはHTTPSのみ許可。
- APIキーはリクエスト中はメモリ上に存在する。ログ・保存領域には残さない。

## 次の作業

1. 新しいAPKを実機に上書きインストールする。APIキーと履歴は引き継がれ、旧既定モデルは `openrouter/free` へ移行する。
2. 既定モデル `openrouter/free` で「AIで整える」を実行し、自然文・タグ・履歴を確認する。
3. 設定から `google/gemini-3.1-flash-lite` などの有料モデルへ切り替えられることを確認する。
4. APIキー削除後にAIボタンが設定導線へ戻ることを確認する。
5. M5のML Kit翻訳フォールバックへ進む。
