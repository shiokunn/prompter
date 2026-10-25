# Prompter 引き継ぎ指示書

- 最終更新: 2026-10-25
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 状態: **M1〜M5 完了・実機確認済み。M6は実機検証完了（10ケース、平均89語、暫定4.37/5、拒否0/10）。最終人手評価と長時間メモリ確認を残す。**
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
ls -lh /storage/emulated/0/Download/Prompter/Prompter-0.6.1-m6-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-0.6.1-m6-debug.apk
```

## 2. 現在の到達点

| 項目 | 状態 |
|---|---|
| M1 辞書 | 完了。Recall 0.5194 / Precision 0.7617 / Invalid 0% |
| M2 オフライン変換 | 完了。Compose UI、Kotlinトライ、PixAI自然文 |
| M3 タグ編集・履歴 | 完了。重み、並び替え、削除、SQLite履歴・お気に入り |
| M4 OpenRouter AI | 完了。BYOK、Keystore暗号化、既定 `openrouter/free` |
| M5 ML Kit翻訳 | **完了・実機確認済み**。辞書外の英訳、英語→タグ、タグ和訳 |
| M6 ローカルLLM | **実機検証完了**。10ケース平均89語、暫定4.37/5、拒否0/10、生成平均15.6秒。アプリPSS最大1.95GB |
| debug署名 | 固定済み。`keystore/prompter-debug.jks` で上書き更新可能 |

### 現在のAPK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.6.1-m6-debug.apk`
- SHA-256: `a2ab65e17df0992e3d32999e0d4c36b5c4d5813e569370c345d29ef8dc02c53e`
- サイズ: 78,976,459 bytes（約79.0MB / 75.3MiB、arm64-v8aのみ）
- versionCode 9 / versionName `0.6.1-m6`
- CI: <https://github.com/shiokunn/prompter/actions/runs/40160559150>
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
- 実機（arrows Alpha）で10ケースを評価。**平均89語、生成平均15.6秒、拒否0/10、暫定4.37/5**
- 初回ロード6.4秒（コールド）/0.5秒（ページキャッシュ後）。機内モード生成成功
- アプリPSSは最大1.95GB、終了後219MBまで解放。バッテリー・発熱は10回連続で大きな変化なし
- 機内モードでも生成可能（モデルとML Kit翻訳モデルがダウンロード済みの場合）
- 失敗時はオフライン結果を保持し、AIカードにエラーを表示
- ローカル生成結果も履歴・キャッシュ・タグ検証の対象
- 設定画面からモデル削除、エンジン解放、再検証

モデルはAPKに同梱していない。保存先はアプリ専用外部領域:
`/storage/emulated/0/Android/data/com.prompter.app/files/Download/models/gemma-4-E2B-it.litertlm`

### M6の残る確認事項

- 10〜20件の人手による最終評価（暫定スコアは `research/M6_LOCAL_EVAL.md`）
- 20〜30回連続生成でPSSの増加が止まるか（10回時点1.95GB）
- 80語未満の3ケース（77・79・71語）を許容するか、追指示で伸ばすか
- `in-color hair`、`center of her face`などの不自然な英語の改善
- 0.6.1-m6での機内モード再確認
- GPUは未実装。今回はCPU / 4 threadsのみ。

## 3. 次のセッションでやること（推奨順）

### 最優先: M6ローカルLLMの最終評価

10ケースの実機評価は完了。`research/M6_LOCAL_EVAL.md` と `research/M6_REPORT.md` の6章に結果がある。

確認済み:

- モデルのダウンロードとSHA-256検証
- 平均89語、80〜150語は7/10、生成平均15.6秒
- 機内モードで生成成功
- 成人向け2件を含め拒否0/10
- アプリPSS最大1.95GB、終了後219MBまで解放
- 10回連続で発熱・バッテリーの大きな変化なし
- 暫定スコア: 意図4.15 / 自然さ4.25 / 構造4.70 / 総合4.37

残り:

1. ユーザーが10〜20件で人手評価し、暫定スコアを確定する。
2. 20〜30回連続生成でPSSが安定するか確認する。
3. 80語未満のケースを許容するか、プロンプトの語数指示をさらに強めるか決める。
4. 不自然な英語をfew-shotで改善する。
5. 0.6.1-m6で機内モードを再確認する。

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
| `research/M6_LOCAL_EVAL.md` | M6の10ケース実機評価・出力全文・暫定スコア |
| `README.md` | ビルド・インストール手順 |

## 7. 既知の未完了・リスク

- M6ローカルLLMは10ケースで平均89語・暫定4.37/5・拒否0/10。人手による最終評価と20〜30回連続のメモリ安定性が未完了。
- M6のダウンロードはHugging Faceの302/CDNリダイレクトに依存する。実機では生成まで到達したが、失敗が再発する場合は独自DLへ切り替える。
- M6はCPUのみ。GPU（Mali-G615）とE4Bは未対応。
- アプリPSSは2回後1.73GB、10回後1.95GB。終了後219MBまで解放。20〜30回での安定性は未確認。
- 3/10が80語未満（77・79・71語）。短文入力の語数対策は未決定。
- 入力にない服装・髪・キッチンなどを補完する場合がある。汎用的な補完は許容範囲だが、意図を変える具体物は減らしたい。
- ローカルLLMの成人向け入力は2/2で拒否なし・内容保持。バリエーションはさらに確認したい。
- PixAI自然文の人手評価（3.5/5）が未完了。
- PixAI DiTの最大プロンプト長、成人向けモデレーションは未検証。
- リリース署名は未実装。
- OpenRouterの実通信テストはユーザー環境依存。CIはMockWebServerのみ。
- `openrouter/free` は選択先モデルにより品質・速度・モデレーションが変わる。
- AIタグは上位5,003タグの範囲外だと除去される。
- ML Kit翻訳はGoogle Play ServicesとモデルDLに依存する。
- debug鍵はリポジトリに含まれるため、公開配布には使わない。

## 8. 次セッションの最初の一言（例）

> `HANDOFF.md` を読んで、M6ローカルLLMの最終評価から進めてください。
> 10ケースの暫定評価は `research/M6_LOCAL_EVAL.md` にあります。人手で10〜20件を確認し、
> 20〜30回連続生成のメモリ安定性、80語未満ケースの扱い、不自然な英語の改善を決めてください。
> 0.6.1-m6は実機にインストール済みです。最終Go/No-Go後にM6.1のGPU/E4Bへ進みます。
