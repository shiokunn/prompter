# Prompter 引き継ぎ指示書

- 最終更新: 2026-10-24
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 状態: **M1〜M5 完了・実機確認済み。M6は実機で生成確認（約10秒/回、拒否なし）。詳細ベンチと品質評価を継続。**
- 実機: arrows Alpha（arm64-v8a / 12GB RAM / 512GB）
- 最新コミットは `git log -1 --oneline` で確認する。

## 1. セッション開始時に確認すること

```bash
cd ~/prompter
git status --short
git log -1 --oneline
adb devices -l
```

- 作業ツリーがcleanであること。
- 端末がadbで見えること（見えなくてもCIビルドは可能）。
- APKの場所とハッシュを確認する。

```bash
ls -lh /storage/emulated/0/Download/Prompter/Prompter-0.6.0-m6-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-0.6.0-m6-debug.apk
```

## 2. 現在の到達点

| 項目 | 状態 |
|---|---|
| M1 辞書 | 完了。Recall 0.5194 / Precision 0.7617 / Invalid 0% |
| M2 オフライン変換 | 完了。Compose UI、Kotlinトライ、PixAI自然文 |
| M3 タグ編集・履歴 | 完了。重み、並び替え、削除、SQLite履歴・お気に入り |
| M4 OpenRouter AI | 完了。BYOK、Keystore暗号化、既定 `openrouter/free` |
| M5 ML Kit翻訳 | **完了・実機確認済み**。辞書外の英訳、英語→タグ、タグ和訳 |
| M6 ローカルLLM | **実機で生成確認済み**。約10秒/回、拒否なし。Gemma 4 E2B / LiteRT-LM 0.18.1 / CPU。詳細ベンチ・品質評価は継続 |
| debug署名 | 固定済み。`keystore/prompter-debug.jks` で上書き更新可能 |

### 現在のAPK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.6.0-m6-debug.apk`
- SHA-256: `10d7a21aec1be0a8c37c292672983354cc4419e3713e21c5cfd9efd7d9dbd77f`
- サイズ: 78,976,459 bytes（約79.0MB / 75.3MiB、arm64-v8aのみ）
- versionCode 8 / versionName `0.6.0-m6`
- CI: <https://github.com/shiokunn/prompter/actions/runs/40073512951>
- 署名証明書SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`
- M5のAPKも残っている:
  `/storage/emulated/0/Download/Prompter/Prompter-0.5.0-m5-debug.apk`
  （SHA-256 `43e013da255753fc81903f4d2813d576c5d715837c17742aea659d4bc4f3c0f5`）

### M6でできること

- 設定 → AIバックエンドで「OpenRouter（クラウド）」と「ローカルLLM（端末内）」を切り替え
- Gemma 4 E2B（約2.59GB）をWi-Fi限定で任意ダウンロード
- ダウンロード後にサイズとSHA-256を検証
- 端末内CPU推論で、オフライン結果を英語PixAIプロンプトに整形
- 実機（arrows Alpha）で **約10秒/回、拒否なし** を確認済み（ユーザー報告）
- 機内モードでも生成可能（モデルとML Kit翻訳モデルがダウンロード済みの場合）
- 失敗時はオフライン結果を保持し、AIカードにエラーを表示
- ローカル生成結果も履歴・キャッシュ・タグ検証の対象
- 設定画面からモデル削除、エンジン解放、再検証

モデルはAPKに同梱していない。保存先はアプリ専用外部領域:
`/storage/emulated/0/Android/data/com.prompter.app/files/Download/models/gemma-4-E2B-it.litertlm`

### M6の残る確認事項

- 約10秒が初回ロード込みか、2回目以降か（出力語数・プロンプト長も揃えて記録）
- 機内モードでの生成
- RAM、発熱、バッテリー消費
- 出力品質（意図・自然さ・構造の5段階評価）
- 成人向け入力のバリエーション別の拒否率（初回確認では拒否なし）
- GPUは未実装。今回はCPU / 4 threadsのみ。

## 3. 次のセッションでやること（推奨順）

### 最優先: M6ローカルLLMの詳細ベンチと品質評価

基本動作は実機で確認済み（**約10秒/回、拒否なし**、ユーザー報告）。`research/M6_REPORT.md` の6章に沿って、再現性のある数字を取る。

確認済み:

- モデルのダウンロードとSHA-256検証（生成に到達したため通過）
- CPU / 4 threadsでのローカル生成、約10秒/回
- 初回確認で生成拒否なし

残り:

1. 同じ例文で、初回（モデル未ロード）と2回目以降を分けて計測する。AIカードの「読み込み」「生成」表示を記録。
2. 出力語数が80〜150語に入っているか、`PROMPT_BEGIN`などのマーカーや日本語が残っていないか確認する。
3. 機内モードでアプリを再起動し、ローカル生成できることを確認する。
4. 成人向け、複数キャラ、風景、日常の10〜20件で品質と拒否率を確認する。
5. `adb shell dumpsys meminfo com.prompter.app`、logcat、体感の発熱・バッテリー消費を記録する。
6. 結果を `research/M6_REPORT.md` に追記し、品質のGo/No-Goを決める。

失敗時の切り分け:

- ダウンロード失敗 → DownloadManagerの理由コード、Hugging Faceの302/CDNリダイレクト。
- SHA不一致 → ファイル削除→再取得。必要なら独自レジュームDLへ。
- 初期化失敗 → `liblitertlm_jni.so` のロード、ABI、モデルパス、`adb logcat`。
- 生成拒否/日本語混入 → システムプロンプトとパーサーを調整。
- OOM/クラッシュ → CPUスレッド数、`maxNumTokens`、Engine解放、Foreground Service化。

### 次: PixAI DiT自然文の人手評価

M6の実機検証後、または並行して実施する。目標は **3.5/5以上**。

1. 日本語シーンを10〜20件用意する（日常、制服、風景、ポーズ、成人向けを含む）。
2. アプリでPixAI自然文を生成する。比較用に辞書+ML Kit、ローカルLLM、OpenRouterを記録。
3. 各文を5段階で評価する:
   - 意図が伝わるか
   - PixAI DiT向けの自然な英語か
   - 主語・動作・場所・雰囲気・スタイルが揃っているか
4. 平均点、失敗例、改善案を `research/PIXAI_EVAL.md` に残す。
5. 3.5未満なら、`PromptComposer` の語順・テンプレート・形容詞、またはローカルLLMのプロンプトを調整する。

### その後: M6.1とリリース準備

- GPU版Gemma 4 E2B（2.01GB）と`Backend.GPU()`を任意設定で追加。Mali-G615で失敗したらCPUへフォールバック。
- 12GB端末向けにE4B（3.65GB）を任意パックにするか判断。
- 生成が遅い場合、`sendMessageAsync`でストリーミング表示。
- モデル一覧の自動取得（OpenRouter `/models`）
- リリース署名（debug鍵は流用しない）
- M7のオンボーディング、設定整理、実機1週間テスト

## 4. 守る制約

- 主対象は **PixAI DiT**。既定出力は英語自然文80〜150語、`Subject + Action + Scene + Mood + Style`。
- タグ出力はSDXL / Pony / A1111 / ComfyUI / NovelAI向けに残す。
- AIは任意ON・BYOK。APIキーはAndroid Keystoreで暗号化し、ログ・履歴・APK・CIに残さない。
- 成人向けタグはアプリ側でフィルタしない。
- 画像生成API、独自IME、クラウド同期、アカウントはMVPに含めない。
- 辞書は同梱の上位5,003タグ。AIが返した辞書外タグは除去する。
- 語数80〜150は警告扱いで、生成自体は保持する。
- ML Kitモデルは必要時ダウンロード。Google Play Servicesが使えなくても辞書のみで動く。
- ローカルLLMは任意。モデル（約2.59GB）はAPKに同梱せず、Wi-Fi限定でダウンロードする。未ダウンロードでもM5の辞書＋ML Kitで動く。
- ローカルLLMはCPU / 4 threads。RAM 8GB以上、空き容量 約3.6GB以上を要求する。GPUはM6.1。
- ローカルLLMのモデルはアプリ専用外部領域に置き、アンインストール時に削除する。SHA-256で検証する。
- ローカル生成が失敗・拒否・日本語混入した場合も、オフライン結果を壊さない。
- 成人向けタグはクラウド/ローカルのどちらでもアプリ側でフィルタしない。モデル自体の拒否はエラーとして扱う。
- APKはarm64-v8a専用。x86エミュレータでは動かない。
- TermuxにAndroid SDKはない。APKビルドはGitHub Actionsで行う。
- debug keystoreはdebug専用。公開リリースには使わない。

## 5. ビルド・テスト・APK更新

```bash
# Pythonテスト
python3 -m unittest tools.test_m1 -v

# Kotlinテスト（Android SDKが必要なので通常はCI）
./gradlew testDebugUnitTest --stacktrace

# CIビルド
git push origin main
gh run list --limit 1
gh run watch <run-id> --exit-status

# Artifact取得
rm -rf dist/latest && mkdir -p dist/latest
gh run download <run-id> -n prompter-debug-apk -D dist/latest

# 署名確認
apksigner verify --print-certs dist/latest/app-debug.apk

# 共有ストレージへコピー
cp dist/latest/app-debug.apk \
  /storage/emulated/0/Download/Prompter/Prompter-<version>-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-<version>-debug.apk
```

次回APKでは `app/build.gradle.kts` の `versionCode` と `versionName` を上げる。
M7なら例: `versionCode 9` / `0.7.0-m7`。

インストール:

```bash
# 0.5.0以降は上書き可能
adb install -r /storage/emulated/0/Download/Prompter/Prompter-<version>-debug.apk
```

0.4.2以前から移行する場合だけ、初回にアンインストールが必要（APIキー・履歴は消える）。

## 6. 主要ファイル

| ファイル | 役割 |
|---|---|
| `app/src/main/java/com/prompter/app/MainActivity.kt` | 画面、変換、翻訳、AI連携 |
| `PromptEngine.kt` | 辞書変換、翻訳マージ、タグ和訳 |
| `PromptComposer.kt` | PixAI自然文の合成（150語上限） |
| `EnglishTagNormalizer.kt` | 英語→Danbooruタグ正規化 |
| `MlKitTextTranslator.kt` | ML Kit翻訳 |
| `TextTranslator.kt` | 翻訳インターフェース |
| `SettingsScreen.kt` | AI設定、ローカルLLMモデルDL、ML KitモデルDL |
| `LocalLlm.kt` | Gemma 4 E2B / LiteRT-LM、プロンプト、出力パース |
| `LocalModelManager.kt` | モデルDL、SHA-256検証、状態管理 |
| `TagEditor.kt` | タグ編集、日本語表示 |
| `OpenRouterClient.kt` | OpenAI互換クライアント |
| `AiSettings.kt` / `AiCache.kt` | キー保存、AIキャッシュ |
| `keystore/prompter-debug.jks` | 固定debug署名鍵 |
| `research/M5_REPORT.md` | M5の詳細 |
| `research/M6_REPORT.md` | M6の実装・実機検証手順 |
| `README.md` | ビルド・インストール手順 |

## 7. 既知の未完了・リスク

- M6ローカルLLMは実機で約10秒/回・拒否なしを確認済み。ただし初回/2回目の内訳、機内モード、RAM、発熱、品質評価は未完了。
- M6のダウンロードはHugging Faceの302/CDNリダイレクトに依存する。実機では生成まで到達したが、失敗が再発する場合は独自DLへ切り替える。
- M6はCPUのみ。GPU（Mali-G615）とE4Bは未対応。
- 初回ロード時間、2回目以降の生成時間、RAM、発熱、バッテリーは未測定。
- ローカルLLMの成人向け入力は初回確認では拒否なし。バリエーション別の拒否率は未評価。
- PixAI自然文の人手評価（3.5/5）が未完了。
- PixAI DiTの最大プロンプト長、成人向けモデレーションは未検証。
- リリース署名は未実装。
- OpenRouterの実通信テストはユーザー環境依存。CIはMockWebServerのみ。
- `openrouter/free` は選択先モデルにより品質・速度・モデレーションが変わる。
- AIタグは上位5,003タグの範囲外だと除去される。
- ML Kit翻訳はGoogle Play ServicesとモデルDLに依存する。
- debug鍵はリポジトリに含まれるため、公開配布には使わない。

## 8. 次セッションの最初の一言（例）

> `HANDOFF.md` を読んで、M6ローカルLLMの詳細ベンチと品質評価から進めてください。
> 約10秒/回・拒否なしは確認済みなので、初回ロードと2回目以降の内訳、機内モード、
> RAM・発熱、10〜20件の5段階評価を記録し、結果を `research/M6_REPORT.md` に追記してください。
> 端末が見えない場合は、先にPixAI自然文の評価セットを準備してください。
