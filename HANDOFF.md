# Prompter 引き継ぎ指示書

- 最終更新: 2026-10-24
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 状態: **M1〜M5 完了。M5はCI・実機確認済み。次はPixAI自然文の評価とM6。**
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
ls -lh /storage/emulated/0/Download/Prompter/Prompter-0.5.0-m5-debug.apk
sha256sum /storage/emulated/0/Download/Prompter/Prompter-0.5.0-m5-debug.apk
```

## 2. 現在の到達点

| 項目 | 状態 |
|---|---|
| M1 辞書 | 完了。Recall 0.5194 / Precision 0.7617 / Invalid 0% |
| M2 オフライン変換 | 完了。Compose UI、Kotlinトライ、PixAI自然文 |
| M3 タグ編集・履歴 | 完了。重み、並び替え、削除、SQLite履歴・お気に入り |
| M4 OpenRouter AI | 完了。BYOK、Keystore暗号化、既定 `openrouter/free` |
| M5 ML Kit翻訳 | **完了・実機確認済み**。辞書外の英訳、英語→タグ、タグ和訳 |
| debug署名 | 固定済み。`keystore/prompter-debug.jks` で上書き更新可能 |

### 現在のAPK

- パス: `/storage/emulated/0/Download/Prompter/Prompter-0.5.0-m5-debug.apk`
- SHA-256: `43e013da255753fc81903f4d2813d576c5d715837c17742aea659d4bc4f3c0f5`
- サイズ: 約51MB（arm64-v8aのみ）
- versionCode 7 / versionName `0.5.0-m5`
- CI: <https://github.com/shiokunn/prompter/actions/runs/40057958074>
- 署名証明書SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`

### M5でできること

- 辞書にない日本語だけをML Kitで英訳
- 英訳からDanbooruタグを補完（`long black hair` → `long_hair` + `black_hair` など）
- 英訳をPixAI自然文の追加コンテキストに反映
- 設定画面から翻訳モデル（約60MB）をダウンロード
- ダウンロード後は機内モードでも翻訳可能
- タグチップ下に日本語の意味を表示（辞書別名優先、なければML Kit）
- 0.5.0以降は `adb install -r` で上書き可能

## 3. 次のセッションでやること（推奨順）

### 最優先: PixAI DiT自然文の人手評価

M1のGo/No-Goで唯一残っている項目。目標は **3.5/5以上**。

1. 日本語シーンを10〜20件用意する（日常、制服、風景、ポーズ、成人向けを含む）。
2. アプリでPixAI自然文を生成する。
3. 各文を5段階で評価する:
   - 意図が伝わるか
   - PixAI DiT向けの自然な英語か
   - 主語・動作・場所・雰囲気・スタイルが揃っているか
4. 平均点、失敗例、改善案を `research/PIXAI_EVAL.md` に残す。
5. 3.5未満なら、`PromptComposer` の語順・テンプレート・形容詞を調整する。

### 次: M6 ローカルLLM

- 対象: **Gemma 4 E2B（2.58GB）**、4B以下を厳守。
- 実行基盤: LiteRT-LM（実装前に最新APIを確認）。
- 任意ダウンロード方式。未ダウンロードでもM5の辞書＋ML Kitで動くこと。
- 機内モードで動くこと。
- ベンチ項目: 初回ロード時間、1回の生成時間、RAM、発熱、バッテリー。
- E4Bは12GB端末限定の任意扱い。MVPでは必須にしない。
- AI補完と同じく、失敗時はオフライン結果を壊さない。

### その後

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
M6なら例: `versionCode 8` / `0.6.0-m6`。

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
| `SettingsScreen.kt` | AI設定、ML KitモデルDL |
| `TagEditor.kt` | タグ編集、日本語表示 |
| `OpenRouterClient.kt` | OpenAI互換クライアント |
| `AiSettings.kt` / `AiCache.kt` | キー保存、AIキャッシュ |
| `keystore/prompter-debug.jks` | 固定debug署名鍵 |
| `research/M5_REPORT.md` | M5の詳細 |
| `README.md` | ビルド・インストール手順 |

## 7. 既知の未完了・リスク

- PixAI自然文の人手評価（3.5/5）が未完了。
- PixAI DiTの最大プロンプト長、成人向けモデレーションは未検証。
- M6ローカルLLMは未実装。
- リリース署名は未実装。
- OpenRouterの実通信テストはユーザー環境依存。CIはMockWebServerのみ。
- `openrouter/free` は選択先モデルにより品質・速度・モデレーションが変わる。
- AIタグは上位5,003タグの範囲外だと除去される。
- ML Kit翻訳はGoogle Play ServicesとモデルDLに依存する。
- debug鍵はリポジトリに含まれるため、公開配布には使わない。

## 8. 次セッションの最初の一言（例）

> `HANDOFF.md` を読んで、まずPixAI自然文の人手評価から進めてください。
> 評価セットは10〜20件、結果は `research/PIXAI_EVAL.md` に残してください。
