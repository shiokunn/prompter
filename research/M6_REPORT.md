# M6 実装レポート: オンデバイス LLM（Gemma 4 E2B / LiteRT-LM）

- 日付: 2026-10-24
- 状態: **実装・CIビルド完了。実機検証は未実施（adb接続端末なし）。**
- 対象: arrows Alpha（arm64-v8a / RAM 12GB / Android 15または16）
- ブランチ: `main`
- 最新コミット: `84bf4e2 fix: make local model verification and engine teardown safe`
- CI: <https://github.com/shiokunn/prompter/actions/runs/40073512951>

---

## 1. 結論

M6の最初の縦切りを実装した。設定画面から約2.59GBのGemma 4 E2Bモデルを任意ダウンロードし、SHA-256検証後に端末内で日本語→英語PixAIプロンプトを生成できる。バックエンドは設定で「OpenRouter（クラウド）」と「ローカルLLM（端末内）」を切り替えられる。

クラウドを使わない場合でも、M5までの辞書＋ML Kit翻訳はそのまま動く。ローカルLLMのダウンロード失敗、読み込み失敗、生成拒否、日本語混入、空応答のいずれでも、オフライン結果を壊さずエラーを表示する設計にした。

実機でのダウンロード・初回ロード・生成時間・RAM・発熱・機内モード動作は未確認。次セッションの最優先事項。

---

## 2. 確定した技術選定

| 項目 | 決定 |
|---|---|
| 推論基盤 | LiteRT-LM Android `0.18.1`（2026-10-20公開の最新） |
| モデル | Gemma 4 E2B IT（LiteRT-LM版、CPU用 `.litertlm`） |
| モデルサイズ | 2,588,147,712 bytes（約2.59GB） |
| SHA-256 | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` |
| Hugging Face revision | `1caa527c1e88b55f046fcd8d2b4265606680663d`（固定） |
| ライセンス | Apache-2.0 |
| バックエンド | CPU / 4 threads（GPUは次段階） |
| コンテキスト | 4,096 tokens |
| 出力上限 | 768 tokens（80〜150語＋タグに十分な上限。暴走時の待ち時間を抑える） |
| サンプリング | topK 10 / topP 0.95 / temperature 0.3 / seed 42 |
| 思考トークン | 無効（`enableThinking = false`） |

LiteRT-LMは`latest.release`ではなく`0.18.1`に固定した。AARはarm64-v8aのJNIを約22MB含むため、APKはM5の約51MBから約79MB（decimal）に増えた。

### GPUを今回入れなかった理由

- arrows AlphaのMali-G615でLiteRT-LM GPUが動くか未確認。
- GPU版モデルは別ファイル（2,008,432,640 bytes）で、CPU版との差分検証が必要。
- まずCPUで「動く・落ちない・オフラインで使える」を確認する。
- GPU対応はM6.1で、GPU版モデルの追加ダウンロードまたはCPU失敗時のフォールバックとして実装する。

---

## 3. 実装したもの

### 3.1 モデル管理

`LocalModelManager.kt`

- `DownloadManager`でHugging FaceからWi-Fi限定ダウンロード。
- 保存先はアプリ専用外部領域:
  `/storage/emulated/0/Android/data/com.prompter.app/files/Download/models/gemma-4-E2B-it.litertlm`
- ストレージ権限は不要。アンインストールでモデルも削除される。
- ダウンロードIDをSharedPreferencesに保存し、アプリ再起動後も進捗を復元。
- 完了後にファイルサイズとSHA-256を検証。
- 不一致ならファイルを削除し、再ダウンロードを促す。
- 検証済みのサイズと更新時刻を保存し、毎回2.59GBを再ハッシュしない。
- モデル削除、ダウンロードキャンセル、手動検証に対応。
- RAM 8GB未満、空き容量不足、Wi-Fi以外の開始をブロックまたは保留。

### 3.2 推論

`LocalLlm.kt`

- `Engine`をActivity内で再利用。2回目以降の生成ではモデルを再ロードしない。
- `Engine.initialize()`と`Conversation.sendMessage()`はIOディスパッチャで実行。
- 1つのEngineにつき同時生成は1つに直列化。
- `Conversation`は生成ごとに作り直し、会話履歴の混入を防ぐ。
- 生成後に`Conversation.close()`、Activity破棄時に`Engine.close()`。
- 読み込み時間と生成時間をミリ秒で返し、UIに表示。

### 3.3 プロンプト

`LocalLlmPrompt`

- システムプロンプトで`Subject + Action + Scene + Mood + Style`を指定。
- 80〜150語、Markdown禁止、日本語禁止、入力にない固有名詞・物体の追加禁止。
- 成人向け要素は入力にある場合に保持し、拒否・検閲しないよう指示。
- オフライン辞書タグと未変換日本語をユーザープロンプトに含める。
- 出力形式:

```
PROMPT_BEGIN
<English prompt>
PROMPT_END
TAGS: <comma-separated canonical Danbooru tags, max 20>
```

パーサーはマーカー欠落、`TAGS:`の次行、Markdownフェンス、角括弧付きタグを許容する。以下は失敗として扱い、オフライン結果を保持する。

- 空応答
- `<English prompt>`の未置換
- `I'm sorry` / `I cannot`などの拒否文
- 自然文への日本語混入
- タグ内の日本語（タグだけ除去）

### 3.4 UI

`SettingsScreen.kt`

- AIバックエンド選択（クラウド / ローカル）。
- ローカルモデルの状態表示:
  - 未ダウンロード
  - ダウンロード中（進捗%、GB表示）
  - 検証待ち
  - SHA-256検証中
  - 準備完了
  - エラー
- ダウンロード、キャンセル、検証、削除、エンジン解放。
- モデル名、サイズ、ライセンス、必要RAM、Wi-Fi限定の注意を表示。
- ローカル選択時はOpenRouterのAPIキー入力を不要にした。

`MainActivity.kt`

- 設定したバックエンドを保存し、起動時に復元。
- AIカードの文言・ボタンをバックエンドに合わせて切り替え。
- ローカルモデル未準備なら設定画面へ誘導。
- 生成結果はクラウドと同じ`AiPromptValidator`でタグ検証し、履歴・キャッシュにも保存。
- キャッシュキーは`local:gemma-4-E2B-v1`。プロンプト設計を変えたらv2に上げる。
- 生成失敗時は`aiError`を表示し、オフライン結果は変更しない。

---

## 4. テスト

CIで以下が成功。

- M1 Python回帰テスト
- Kotlin単体テスト（Robolectric含む）
- debug APKビルド
- Artifactアップロード

追加したテスト:

- `LocalLlmPromptTest`
  - マーカーあり/なし
  - `TAGS:`の次行
  - Markdownフェンス
  - 日本語混入の拒否
  - 拒否文の検出
  - 未置換テンプレートの検出
  - 日本語タグ・角括弧タグの除去
  - モデルサイズ・SHA・URLの固定確認
- `AiSettingsTest`
  - バックエンドの既定値がCLOUD
  - LOCALの保存・復元

実機テストは未実施。単体テストは「パーサーと設定」の範囲で、LiteRT-LMのネイティブ初期化・モデルダウンロード・生成品質は保証しない。

---

## 5. APK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.6.0-m6-debug.apk`
- サイズ: 78,976,459 bytes（約79.0MB / 75.3MiB）
- SHA-256: `10d7a21aec1be0a8c37c292672983354cc4419e3713e21c5cfd9efd7d9dbd77f`
- versionCode 8 / versionName `0.6.0-m6`
- ABI: arm64-v8aのみ
- 署名: M5と同じ固定debug鍵。`adb install -r`で上書き可能。

---

## 6. 実機検証チェックリスト

### 6.1 インストール

```bash
adb install -r /storage/emulated/0/Download/Prompter/Prompter-0.6.0-m6-debug.apk
adb shell am force-stop com.prompter.app
```

### 6.2 モデルダウンロード

1. 設定 → AIバックエンド → ローカルLLM（端末内）。
2. 「モデルをダウンロード（約2.59GB）」をタップ。
3. Wi-Fi接続を確認。
4. 通知と設定画面の進捗が更新されること。
5. 完了後に「SHA-256検証中」→「準備完了」になること。
6. ファイルサイズを確認:

```bash
adb shell run-as com.prompter.app ls -l \
  /storage/emulated/0/Android/data/com.prompter.app/files/Download/models/
```

期待値: `2588147712` bytes。

### 6.3 生成

1. 例: 「長い黒髪の少女が青いドレスを着て、椅子に座っている」を変換。
2. 「ローカルLLMで整える」をタップ。
3. 初回の読み込み時間、生成時間、語数がAIカードに表示されること。
4. 出力が80〜150語の英語で、`PROMPT_BEGIN`などのマーカーが残っていないこと。
5. タグが辞書内のものだけになっていること。
6. もう一度実行し、2回目が読み込みなしで速くなること。
7. アプリを再起動し、モデルが「準備完了」のままで再ダウンロード不要なこと。

### 6.4 オフライン

1. 機内モードをON。
2. アプリを強制停止して再起動。
3. モデルが「準備完了」のままであること。
4. ローカルLLMで生成できること。
5. ML Kit翻訳モデルもダウンロード済みなら、辞書外の翻訳が動くこと。

### 6.5 品質・負荷

- 日本語シーン10〜20件で、意図・自然さ・構造を5段階評価。
- 成人向けタグを含む入力で、拒否されずタグが保持されるか。
- 複数キャラ入力で属性が混ざらないか。
- 初回ロード時間、生成時間、RAM、発熱、バッテリー消費を記録。

```bash
adb shell dumpsys meminfo com.prompter.app
adb logcat -d | grep -iE "litert|gemma|prompter" | tail -100
```

### 6.6 失敗時

- ダウンロード失敗: DownloadManagerの通知と設定画面のエラー理由を確認。
- SHA不一致: ファイルが削除され、再ダウンロードできること。
- 読み込み失敗: アプリが落ちず、オフライン結果が残ること。
- 生成拒否/日本語混入: エラー表示され、オフライン結果が残ること。
- OOM/クラッシュ: `adb logcat`と`dumpsys meminfo`を保存。

---

## 7. 既知の制約・リスク

| 制約 | 内容 | 次アクション |
|---|---|---|
| 外部保存のmmap | app-specific external上の2.59GBをLiteRT-LMがmmapできるか未確認 | 失敗時は内部storageへコピーする方式に変更 |
| 実機未検証 | LiteRT-LMの初期化・生成・ダウンロードが実機で未確認 | 最優先で6章を実施 |
| CPUのみ | GPU版モデル/バックエンドは未実装 | M6.1でMali-G615を検証 |
| 2.59GB | Wi-Fi必須。ストレージを大きく使う | ダウンロード前に容量表示済み |
| 8GB RAM要件 | 8GB未満は開始不可 | 実機12GBなので問題なし |
| 初回ロード | 数秒〜数十秒かかる可能性 | UIに注記。実測後に必要ならプログレス表示 |
| バックグラウンド | 生成中にOSがkillする可能性 | 必要ならForeground Service化 |
| ストリーミングなし | 完了までスピナー | 実測が長ければ`sendMessageAsync`へ |
| 拒否検出は簡易 | 多様な拒否文を完全には検出できない | 実機の失敗例を収集して追加 |
| E4B未対応 | 12GB向け高品質パックは未実装 | E2Bの品質次第で追加 |
| モデル更新 | revision固定のため自動更新しない | 更新時はSHAとサイズを更新 |

---

## 8. 次の作業

1. **実機で6章を実施**し、結果をこのレポートに追記する。
2. ダウンロードが失敗する場合は、`DownloadManager`の理由コードとHugging Faceのリダイレクトを確認する。必要なら独自のレジューム付きHTTPダウンロードに切り替える。
3. 生成品質が3.5/5未満なら、システムプロンプトのfew-shot追加、タグの渡し方、語数指示を調整する。
4. 初回ロードが長すぎる場合、`sendMessageAsync`で最初のトークンまでの進捗を出す。
5. CPUで安定したら、GPU版モデル（2.01GB）と`Backend.GPU()`を任意設定で追加する。
6. M6のGo/No-Go後、PixAI自然文の人手評価（`research/PIXAI_EVAL.md`）へ進む。
