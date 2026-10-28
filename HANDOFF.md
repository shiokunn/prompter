# Prompter 引き継ぎ指示書

- 最終更新: 2026-10-28
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 状態: **M1〜M6 完了・実機確認済み。M7は `0.7.5-m7`（versionCode 20）。v13の人手再採点は総合4.125/5・忠実さ3.625/5まで改善したが、未指定ヘアの重大2件（ID24・ID26）とタグ平均3.875で正式基準は不合格。v13.1で未指定ヘアの除去、未指定カメラタグの除去、`long_hair` / インカラー / `tray` / `juice` / `omurice` / `split_screen` のタグ補完を実装。8ケースは平均219.4語・180〜260語8/8・全部キャッシュ時約3.1秒。次の最優先はv13.1の8ケース人手再採点。機内モードはユーザー指定により実施しない。**
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
ls -lh /storage/emulated/0/Download/Prompter/Prompter-0.7.5-m7-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-0.7.5-m7-debug.apk
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
| M7 UI刷新・クラウド上限撤廃 | **実装・実機確認済み（一部）**。紙面調UI、タグ編集のモード限定、接続テスト、ローカルv13、クラウド語数上限なし。APK `0.7.5-m7`。v13の人手再採点は総合4.125/5・忠実さ3.625/5だが、重大2件とタグ3.875で不合格。v13.1で未指定ヘア・カメラタグ・タグ欠落を修正し、8ケース平均219.4語・8/8。次はv13.1の人手再採点 |
| debug署名 | 固定済み。`keystore/prompter-debug.jks` で上書き更新可能 |

### 現在のAPK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.7.5-m7-debug.apk`
- SHA-256: `e1ba871511b8ac6f4363b3cdbd3d73a8f10361e36ab0190c22dd9332fd20d0fd`
- サイズ: 約75MB（arm64-v8aのみ）
- versionCode 20 / versionName `0.7.5-m7`
- コード: コミット `b0135b6`（v13.1のガード・辞書）
- CI: <https://github.com/shiokunn/prompter/actions/runs/40662259014>
- 署名証明書SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`
- 旧APKも残っている:
  `Prompter-0.7.1-m7-debug.apk`（SHA-256 `594273793abbde4db52df82d21f107856b9af18b6b9448538789700378727828`）
  `Prompter-0.6.6-m6-debug.apk`（SHA-256 `bb3774f64cfb025bd3a96ce42354b253480f6018ae2bc5e77a29b71ff5e5c4eb`）

### M6/M7でできること

- 設定 → AIバックエンドで「OpenRouter（クラウド）」と「ローカルLLM（端末内）」を切り替え
- Gemma 4 E2B（約2.59GB）をWi-Fi限定で任意ダウンロード
- ダウンロード後にサイズとSHA-256を検証
- 端末内CPU推論で、オフライン結果を英語PixAIプロンプトに整形
- M7のローカルプロンプト **v13** は **180〜260語・11〜12文・各文18語以上**。同じ8ケースで **平均230.4語、180〜260語 8/8、拒否0/8、日本語混入0/8、平均38.8秒**。v9の180〜260語・8〜12文から更新
- **v13.1（0.7.5-m7）** はプロンプトを変えず、ガードと辞書だけを修正。8ケースは **平均219.4語、180〜260語 8/8**（195〜235語）。AIキャッシュ利用時は約3.1秒。未指定ヘアの重大0件、`long_hair` / インカラー / `tray` / `juice` / `omurice` / `split_screen` を補完
- v13の8ケース終了後PSSは **2,317MB**（v9の20回連続時は1937.5〜2143.5MB）。OOM・クラッシュなし。8ケース一括の最後に自動操作タイムアウトが1回あったが、再起動後の再実行は231語・39.2秒で成功
- v9で見つかりv13で修正した課題: 成人向け短文157語、入力にない服色の捏造、`aiming`、二人なのに `solo,1girl`、単数に `multiple_girls`、`half_updo`、`cleavage`、未指定の服装・メタ説明
- v13.1で修正: 24/26の未指定ヘア、25の未指定カメラタグ、`long_hair` / インカラー / `tray` / `juice` / `omurice` / `split_screen` のタグ欠落
- v13.1の残課題: 29の具体的な料理工程（野菜を切る/卵を焼く/盛り付け）、25の `split` と `split_screen` の重複、24の単数代名詞と「deep, shared affection」の解釈
- 人手評価8ケース（20/21/22/24/25/26/27/29）: v9は総合3.63/5・忠実さ2.88（未達）。v13は総合 **4.125/5**・意図4.75・自然さ4.25・構造5.00・**忠実さ3.625**・タグ3.875。重大2件（24/26の未指定ヘア）で**不合格**。v13.1の再採点は未実施
- 機内モードはユーザー指定により実施しない（評価対象から外す）
- M7のクラウドは **`max_tokens` と語数上限を撤廃**。有効キーで **224語・44.3秒**、**183語・191.2秒** を確認。3回目の連続生成は無料枠のレート制限に当たった
- M6のv7実績: 10ケース平均101語、80〜150語10/10、拒否0/10、生成平均30.3秒（`research/M6_PROMPT_V7.md`）
- v2の初回評価は平均89語・生成平均15.6秒・暫定4.37/5（`research/M6_LOCAL_EVAL.md`）
- 初回ロード6.4秒（コールド）/0.5秒（ページキャッシュ後）。機内モード生成成功
- アプリPSSはv7最大1.95GB、v9 20回連続で1937.5〜2143.5MB。終了後219MBまで解放。v9の20回連続でクラッシュ・OOM・危険な発熱なし
- 機内モードでも生成可能（モデルとML Kit翻訳モデルがダウンロード済みの場合）
- 失敗時はオフライン結果を保持し、AIカードにエラーを表示
- ローカル生成結果も履歴・キャッシュ・タグ検証の対象
- 設定画面からモデル削除、エンジン解放、再検証
- M7の紙面調UI、タグモード限定のタグ編集、OpenRouter接続テスト、キー再発行リンク
- ステータスバー・ナビゲーションバーを紙色に統一し、暗いアイコンを強制

モデルはAPKに同梱していない。保存先はアプリ専用外部領域:
`/storage/emulated/0/Android/data/com.prompter.app/files/Download/models/gemma-4-E2B-it.litertlm`

### M7の残る確認事項

- 人手評価の結果を受けて、**忠実さ（2.88）とタグ整合性**を修正する
- 10ケースで157語、20回連続で180語未満5/20の語数ばらつきも、忠実さ修正後に再確認する
- クラウドは代替なので低優先。余力があればレート制限の解除後に0.7.3の忠実さを軽く確認する
- GPUは未実装。今回はCPU / 4 threadsのみ
- 機内モードはユーザー指定により実施しない

## 3. 次のセッションでやること（推奨順）

### 最優先: 忠実さとタグ整合性の修正

現状:

- ローカルv9の10ケース評価は **平均195.5語、180〜260語 9/10、拒否0/10、日本語混入0/10、平均35.6秒**。
- 20回連続生成は **PSS約2.0GBで安定**、クラッシュ・OOM・拒否0/20。平均43.1秒、180〜260語15/20、最高温度BATTERY 36.9°C / SKIN 39.7°C / CPU 51.5°C。
- 人手評価8ケースは **総合3.63/5（合格）**、意図4.38、自然さ3.63、構造4.50、**忠実さ2.88（未達）**。
- 最優先課題は **自然文とタグの矛盾**（25の `cleavage`、27の `solo,1girl`）と、**未指定の身体的特徴・服色の追加**（26の長い髪、27/29の服色）。
- クラウドは有効キーで224語・44.3秒、183語・191.2秒を生成し、語数上限なしを確認済み。3回目は無料枠のレート制限。クラウドは代替なので深追いしない。
- 詳細は [`research/PIXAI_EVAL.md`](research/PIXAI_EVAL.md)、[`research/M7_LOCAL_V9_EVAL.md`](research/M7_LOCAL_V9_EVAL.md)、[`research/M7_STABILITY.md`](research/M7_STABILITY.md)。

手順:

1. **タグ整合性チェック**を実装する。
   - 胸元を隠す/露出が少ない入力で `cleavage` / `breasts` を除去。
   - 二人以上の明示がある入力で `solo` / `1girl` を除去し、`2girls` / `multiple_girls` を補う。
   - 単数の明示がある入力で `2girls` / `multiple_girls` を除去。
   - `半分` 由来の `half_updo`、`上目遣い` 由来の `aiming` を修正。
2. **v10プロンプト**で忠実さを強化する。入力の人数・髪色/長さ/型・目色・服の種類/色・ポーズ・背景・小物をチェックリスト化し、リスト外の身体的特徴・服色・具体的な家具/小道具を追加しない。制服/メイド服の色、胸元を隠す指定での露出表現、定型句を禁止例にする。
3. 同じ8ケース（可能なら10ケース）を再評価し、忠実さ3.5以上を目指す。
4. 余力があれば、レート制限の解除後に0.7.3のクラウド忠実さを軽く確認する（低優先）。
5. 機内モードはユーザー指定により実施しない。

失敗時の切り分け:

- クラウド401 → キー再発行。モデルIDやBase URLの問題ではない。
- `レート制限 ... Provider returned error` → 無料枠の制限。時間を置くか、別の無料モデルを指定する。クラウドは代替なので深追いしない。
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

- クラウドAIは **ローカルLLMが使えないときの代替**。品質の作り込みは最小限にし、接続・生成・語数上限なしが確認できれば十分。0.7.3の忠実さ再検証は低優先。
- 主対象は **PixAI DiT**。既定出力は英語自然文（オフライン80〜150語、クラウドは語数上限なし、ローカルLLM180〜260語）、`Subject + Action + Scene + Mood + Style`。
- タグ出力はSDXL / Pony / A1111 / ComfyUI / NovelAI向けに残す。
- AIは任意ON・BYOK。APIキーはAndroid Keystoreで暗号化し、ログ・履歴・APK・CIに残さない。
- 成人向けタグはアプリ側でフィルタしない。
- 画像生成API、独自IME、クラウド同期、アカウントはMVPに含めない。
- 辞書は同梱の上位5,000タグ + 補完3タグ（`omurice` / `juice` / `split_screen`）。AIが返した辞書外タグは除去する。
- 語数はオフライン80〜150、ローカル180〜260を外れると警告を出すが、生成自体は保持する。クラウドは語数上限なしで、語数警告を出さない。
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
現在は `versionCode 20` / `0.7.5-m7`。次回は `21` / `0.7.6-m7`。

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
| `AiPromptValidator.kt` | オフライン/ローカルの語数検証。クラウドは上限なし |
| `AiSettings.kt` / `AiCache.kt` | キー保存、AIキャッシュ |
| `app/src/test/java/com/prompter/app/OpenRouterClientTest.kt` | `/key` 成功・401のMockWebServerテスト |
| `keystore/prompter-debug.jks` | 固定debug署名鍵 |
| `research/M5_REPORT.md` | M5の詳細 |
| `research/M6_REPORT.md` | M6の実装・実機検証手順 |
| `research/M6_LOCAL_EVAL.md` | M6の10ケース実機評価・出力全文・暫定スコア（v2） |
| `research/M6_PROMPT_V7.md` | プロンプトv7の変更点と10ケース再評価 |
| `research/M7_REPORT.md` | M7 UI刷新・OpenRouter診断・ローカルv9 |
| `research/M7_CLOUD_VERIFY.md` | 有効キーでのクラウド生成・語数上限撤廃・レート制限の記録 |
| `research/M7_LOCAL_V9_EVAL.md` | ローカルv9の10ケース評価・出力全文・残課題 |
| `research/M7_STABILITY.md` | ローカルv9の20回連続生成・PSS/温度/時間の推移 |
| `research/PIXAI_EVAL.md` | PixAI自然文の人手評価結果（8ケース、総合3.63/5、忠実さ2.88/5） |
| `README.md` | ビルド・インストール手順 |

## 7. 既知の未完了・リスク

- **OpenRouterの有効キーは確認済み**（無料枠 / 使用量$0.00）。ただし無料モデルはレート制限が厳しく、3回目の連続生成で `レート制限に達しました。少し待って再試行してください。 Provider returned error` が出た。クラウドはローカルLLMの代替なので、0.7.3の忠実さ再検証は低優先。
- M7のローカルv9は10ケース評価済み（平均195.5語、180〜260語 9/10、拒否0/10、平均35.6秒）と20回連続生成済み（PSS約2.0GBで安定、クラッシュ・OOM・拒否0/20、平均43.1秒）。人手評価8ケースも完了（総合3.63/5、忠実さ2.88/5）。残るのは忠実さ・タグ整合性の修正。
- v9の生成時間は10ケース平均35.6秒、20回連続平均43.1秒。v7の平均30.3秒より長い。20回連続の後半はやや遅いが、出力語数とばらつきの範囲で、危険な発熱はない。
- v9の残課題: 10ケースで成人向け短文1件が157語、20回連続で180語未満5/20（165〜177語）、入力にない服色の捏造2/10（学生服の白シャツ/黒ズボン、メイド服の白布/黒エプロン）、オフラインタグの誤り（`aiming`、二人なのに `solo,1girl`、単数に `multiple_girls`）。
- アプリPSSはv7の2回後1.73GB、10回後1.95GB。v9の20回連続は1937.5〜2143.5MB（モデルロード後）。終了後219MBまで解放。
- M6のダウンロードはHugging Faceの302/CDNリダイレクトに依存する。実機では生成まで到達したが、失敗が再発する場合は独自DLへ切り替える。
- M6はCPUのみ。GPU（Mali-G615）とE4Bは未対応。
- 入力にない服装・髪・キッチンなどを補完する場合がある。v9の10ケースでは服色の捏造が2/10（27の学生服、29のメイド服）残った。髪色・目色の捏造は0/10。
- PixAI自然文の人手評価は8ケース完了。総合3.63/5で合格、意図4.38、自然さ3.63、構造4.50、忠実さ2.88（未達）。ID28/30は未評価。画像生成での検証は未実施。
- PixAI DiTの最大プロンプト長、成人向けモデレーションは未検証。
- リリース署名は未実装。debug鍵はリポジトリに含まれるため公開配布に使わない。
- OpenRouterの実通信テストはユーザー環境依存。CIはMockWebServerのみ。
- `openrouter/free` は選択先モデルにより品質・速度・モデレーションが変わる。
- AIタグは同梱辞書（5,006タグ）の範囲外だと除去される。
- ML Kit翻訳はGoogle Play ServicesとモデルDLに依存する。
- UIは強制ライトテーマ。端末のダークモード設定でも紙色で表示する。

## 8. 次セッションの最初の一言（例）

> `HANDOFF.md` を読んで、M7ローカルv13.1の人手再採点から進めてください。
> クラウドはローカルLLMが使えないときの代替なので、品質の作り込みはしないでください。
> v13の再採点は総合4.125/5・忠実さ3.625/5でしたが、24/26の未指定ヘア2件とタグ3.875で不合格でした。
> v13.1で未指定ヘア・カメラタグ・タグ欠落を直し、8ケースは平均219.4語・180〜260語8/8です。
> `research/M7_V13_RERATE.md` の付録A（v13.1）を同じ基準で再採点し、重大0件・タグ4.0以上を確認してください。
> 残課題は29の具体的な料理工程、25の `split` 重複、24の単数代名詞です。
> 機内モードは実施しないでください。0.7.5-m7は実機にインストール済み、AIバックエンドはLOCALです。
> 採点キット（基準・評価用プロンプト・8ケース全文）は `research/M7_V13_RERATE.md` を使ってください。
> 詳細は `research/M7_LOCAL_V13_EVAL.md`、`research/PIXAI_EVAL.md`、`research/M7_LOCAL_V9_EVAL.md`。
