# Prompter 引き継ぎ指示書

- 最終更新: 2026-10-26
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 状態: **M1〜M6 完了・実機確認済み。M7 UI刷新・ローカルプロンプトv9実装完了（実機1ケース198語）。OpenRouterは有効キー待ち。最終人手評価・v9の10ケース再評価・長時間メモリ確認を残す。**
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
ls -lh /storage/emulated/0/Download/Prompter/Prompter-0.7.1-m7-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-0.7.1-m7-debug.apk
```

## 2. 現在の到達点

| 項目 | 状態 |
|---|---|
| M1 辞書 | 完了。Recall 0.5194 / Precision 0.7617 / Invalid 0% |
| M2 オフライン変換 | 完了。Compose UI、Kotlinトライ、PixAI自然文 |
| M3 タグ編集・履歴 | 完了。重み、並び替え、削除、SQLite履歴・お気に入り |
| M4 OpenRouter AI | 完了。BYOK、Keystore暗号化、既定 `openrouter/free` |
| M5 ML Kit翻訳 | **完了・実機確認済み**。辞書外の英訳、英語→タグ、タグ和訳 |
| M6 ローカルLLM | **実機検証完了**。v7で10ケース平均101語、80〜150語10/10、拒否0/10、生成平均30.3秒。v9で198語（1ケース、40.5秒）。アプリPSS最大1.95GB |
| M7 UI刷新・OpenRouter診断 | **実装・実機確認中**。紙面調UI、タグ編集のモード限定、接続テスト、ローカルv9。APK `0.7.1-m7` |
| debug署名 | 固定済み。`keystore/prompter-debug.jks` で上書き更新可能 |

### 現在のAPK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.7.1-m7-debug.apk`
- SHA-256: `594273793abbde4db52df82d21f107856b9af18b6b9448538789700378727828`
- サイズ: 約76MB（arm64-v8aのみ）
- versionCode 16 / versionName `0.7.1-m7`
- コード: コミット `4768434`（この後のコミットはドキュメントのみ）
- CI: <https://github.com/shiokunn/prompter/actions/runs/40318320648>
- 署名証明書SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`
- 旧APKも残っている:
  `Prompter-0.6.6-m6-debug.apk`（SHA-256 `bb3774f64cfb025bd3a96ce42354b253480f6018ae2bc5e77a29b71ff5e5c4eb`）

### M6/M7でできること

- 設定 → AIバックエンドで「OpenRouter（クラウド）」と「ローカルLLM（端末内）」を切り替え
- Gemma 4 E2B（約2.59GB）をWi-Fi限定で任意ダウンロード
- ダウンロード後にサイズとSHA-256を検証
- 端末内CPU推論で、オフライン結果を英語PixAIプロンプトに整形
- M7のローカルプロンプト **v9** は **180〜260語・8〜12文**。実機1ケースで **198語・生成40.5秒**、服装色の追加なし
- M6のv7実績: 10ケース平均101語、80〜150語10/10、拒否0/10、生成平均30.3秒（`research/M6_PROMPT_V7.md`）
- v2の初回評価は平均89語・生成平均15.6秒・暫定4.37/5（`research/M6_LOCAL_EVAL.md`）
- 初回ロード6.4秒（コールド）/0.5秒（ページキャッシュ後）。機内モード生成成功
- アプリPSSは最大1.95GB、終了後219MBまで解放。バッテリー・発熱は10回連続で大きな変化なし
- 機内モードでも生成可能（モデルとML Kit翻訳モデルがダウンロード済みの場合）
- 失敗時はオフライン結果を保持し、AIカードにエラーを表示
- ローカル生成結果も履歴・キャッシュ・タグ検証の対象
- 設定画面からモデル削除、エンジン解放、再検証
- M7の紙面調UI、タグモード限定のタグ編集、OpenRouter接続テスト、キー再発行リンク
- ステータスバー・ナビゲーションバーを紙色に統一し、暗いアイコンを強制

モデルはAPKに同梱していない。保存先はアプリ専用外部領域:
`/storage/emulated/0/Android/data/com.prompter.app/files/Download/models/gemma-4-E2B-it.litertlm`

### M7の残る確認事項

- **有効なOpenRouterキー**でのクラウド生成。保存済みキーは `401 User not found.` で、再発行が必要
- v9で10ケースを再評価し、平均語数・180〜260語の達成率・拒否数・生成時間を測る
- v9の服装色ルールを複数ケースで確認する（1ケースのみ）
- 20〜30回連続生成でPSSの増加が止まるか（v9は40.5秒/198語で、v7より長い）
- 0.7.1-m7で機内モードを再確認する
- 10〜20件の人手による最終評価（目標3.5/5以上）
- GPUは未実装。今回はCPU / 4 threadsのみ。

## 3. 次のセッションでやること（推奨順）

### 最優先: OpenRouterの有効キーとv9評価

現状:

- 保存済みOpenRouterキーは実通信で `401 User not found.`。モデルID・Base URLは有効。
- 設定の「接続テスト」は401を検出し、「キーを再発行」から <https://openrouter.ai/keys> を開ける。
- ローカルはv9で198語・40.5秒を1ケース確認。白髪・赤目・メイド服・オムライス・三分割を保持し、服装色の追加はなくなった。
- 詳細は [`research/M7_REPORT.md`](research/M7_REPORT.md)。

手順:

1. 有効なOpenRouterキーを発行し、設定 → クラウド → キー入力 → **接続テスト** → **保存**。
2. 同じ入力でクラウド生成を1回行い、80〜150語・JSON・タグ検証を確認。
3. ローカルv9でM6の10ケースを再実行する。語数・生成時間・拒否・色の保持を記録。
4. 20〜30回連続生成でPSSと発熱を確認。
5. 機内モードでv9を再確認。
6. 人手評価を10〜20件行い、`research/PIXAI_EVAL.md` にまとめる。

失敗時の切り分け:

- クラウド401 → キー再発行。モデルIDやBase URLの問題ではない。
- ダウンロード失敗 → DownloadManagerの理由コード、Hugging Faceの302/CDNリダイレクト。
- SHA不一致 → ファイル削除→再取得。必要なら独自レジュームDLへ。
- 初期化失敗 → `liblitertlm_jni.so` のロード、ABI、モデルパス、`adb logcat`。
- 生成拒否/日本語混入 → システムプロンプトとパーサーを調整。
- OOM/クラッシュ → CPUスレッド数、`maxNumTokens`、Engine解放、Foreground Service化。

### 次: PixAI DiT自然文の人手評価

M7の実機検証後、または並行して実施する。目標は **3.5/5以上**。

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
- オンボーディング、共有、実機1週間テスト

## 4. 守る制約

- 主対象は **PixAI DiT**。既定出力は英語自然文（クラウド/オフライン80〜150語、ローカルLLM180〜260語）、`Subject + Action + Scene + Mood + Style`。
- タグ出力はSDXL / Pony / A1111 / ComfyUI / NovelAI向けに残す。
- AIは任意ON・BYOK。APIキーはAndroid Keystoreで暗号化し、ログ・履歴・APK・CIに残さない。
- 成人向けタグはアプリ側でフィルタしない。
- 画像生成API、独自IME、クラウド同期、アカウントはMVPに含めない。
- 辞書は同梱の上位5,003タグ。AIが返した辞書外タグは除去する。
- 語数はクラウド/オフライン80〜150、ローカル180〜260を外れると警告を出すが、生成自体は保持する。
- UIは強制ライトテーマ。ステータスバー・ナビゲーションバーも紙色に統一する。
- タグ編集はタグモードのときだけ表示する。自然文モード・両方モードでは結果のタグだけを見せる。
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
現在は `versionCode 16` / `0.7.1-m7`。次回は `17` / `0.7.2-m7`。

インストール:

```bash
# 0.5.0以降は上書き可能
adb install -r /storage/emulated/0/Download/Prompter/Prompter-<version>-debug.apk
```

0.4.2以前から移行する場合だけ、初回にアンインストールが必要（APIキー・履歴は消える）。

## 6. 主要ファイル

| ファイル | 役割 |
|---|---|
| `app/src/main/java/com/prompter/app/MainActivity.kt` | 画面、変換、翻訳、AI連携、M7レイアウト |
| `PromptEngine.kt` | 辞書変換、翻訳マージ、タグ和訳 |
| `PromptComposer.kt` | オフラインPixAI自然文の合成（150語上限） |
| `EnglishTagNormalizer.kt` | 英語→Danbooruタグ正規化 |
| `MlKitTextTranslator.kt` | ML Kit翻訳 |
| `TextTranslator.kt` | 翻訳インターフェース |
| `Theme.kt` | M7の紙面調テーマ、共通ヘッダー、ボタン、カード |
| `SettingsScreen.kt` | AI設定、接続テスト、ローカルLLMモデルDL、ML KitモデルDL |
| `HistoryScreen.kt` | M7の履歴一覧、お気に入り、削除 |
| `LocalLlm.kt` | Gemma 4 E2B / LiteRT-LM、プロンプトv9、出力パース |
| `LocalModelManager.kt` | モデルDL、SHA-256検証、状態管理 |
| `TagEditor.kt` | タグ編集、重み、並び替え、削除、日本語表示 |
| `OpenRouterClient.kt` | OpenAI互換クライアント、`/key` 接続テスト、401判定 |
| `AiPromptValidator.kt` | クラウド80〜150語 / ローカル180〜260語の検証 |
| `AiSettings.kt` / `AiCache.kt` | キー保存、AIキャッシュ |
| `app/src/test/java/com/prompter/app/OpenRouterClientTest.kt` | `/key` 成功・401のMockWebServerテスト |
| `keystore/prompter-debug.jks` | 固定debug署名鍵 |
| `research/M5_REPORT.md` | M5の詳細 |
| `research/M6_REPORT.md` | M6の実装・実機検証手順 |
| `research/M6_LOCAL_EVAL.md` | M6の10ケース実機評価・出力全文・暫定スコア（v2） |
| `research/M6_PROMPT_V7.md` | プロンプトv7の変更点と10ケース再評価 |
| `research/M7_REPORT.md` | M7 UI刷新・OpenRouter診断・ローカルv9 |
| `README.md` | ビルド・インストール手順 |

## 7. 既知の未完了・リスク

- **OpenRouterの保存キーは無効**。実通信で `401 User not found.`。モデルID・Base URLは有効で、<https://openrouter.ai/keys> で再発行が必要。クラウド生成は有効キー待ち。
- M7のローカルv9は1ケースのみ（198語・40.5秒）。10ケース再評価、服装色ルールの複数確認、人手評価が未完了。
- v9の生成時間は40.5秒で、v7の平均30.3秒より長い。詳細化とのバランス、発熱・メモリは再測定が必要。
- M6のv7実績は10ケース平均101語・80〜150語10/10・拒否0/10。v7のID24（87語）、ID27（125語）、ID30（黒髪補完）はv9で解消したか未確認。
- アプリPSSは2回後1.73GB、10回後1.95GB。終了後219MBまで解放。v9で20〜30回の安定性は未確認。
- M6のダウンロードはHugging Faceの302/CDNリダイレクトに依存する。実機では生成まで到達したが、失敗が再発する場合は独自DLへ切り替える。
- M6はCPUのみ。GPU（Mali-G615）とE4Bは未対応。
- 入力にない服装・髪・キッチンなどを補完する場合がある。v9は1ケースで服装色の追加を抑えたが、一般化は未確認。
- PixAI自然文の人手評価（3.5/5）が未完了。
- PixAI DiTの最大プロンプト長、成人向けモデレーションは未検証。
- リリース署名は未実装。debug鍵はリポジトリに含まれるため公開配布に使わない。
- OpenRouterの実通信テストはユーザー環境依存。CIはMockWebServerのみ。
- `openrouter/free` は選択先モデルにより品質・速度・モデレーションが変わる。
- AIタグは上位5,003タグの範囲外だと除去される。
- ML Kit翻訳はGoogle Play ServicesとモデルDLに依存する。
- UIは強制ライトテーマ。端末のダークモード設定でも紙色で表示する。

## 8. 次セッションの最初の一言（例）

> `HANDOFF.md` を読んで、M7の仕上げから進めてください。
> まず有効なOpenRouterキーを設定し、「接続テスト」→「保存」→クラウド生成を確認してください。
> 次にローカルv9で10ケースを再評価し、語数・生成時間・拒否・服装色の保持を記録してください。
> 20〜30回連続生成のメモリ安定性と機内モードも再確認します。
> 0.7.1-m7は実機にインストール済み、AIバックエンドはLOCALです。詳細は `research/M7_REPORT.md`。
