# M5 — ML Kit翻訳フォールバックと安定したdebug署名

- 日付: 2026-10-24
- 状態: **実装・CI完了（実機確認待ち）**
- 対象: `app/src/main/java/com/prompter/app/` / `app/src/test/java/com/prompter/app/`
- APK: `Prompter-0.5.0-m5-debug.apk`（versionCode 7、arm64-v8a）

## 目的

辞書に一致しない日本語を端末内で英訳し、自然文の補足とDanbooruタグの両方に反映する。
翻訳モデルが未ダウンロード、Google Play Servicesが使えない、通信できない場合でも、
従来の辞書のみの変換を壊さないことを優先する。

あわせて、CIのたびにdebug署名鍵が変わって上書きインストールできなくなる問題を解消する。

## 実装

### 翻訳の流れ

1. 従来どおりKotlinの辞書マッチャで日本語を変換する。
2. `ConversionResult.unmatched`（辞書外の残り）がある場合だけ、ML Kitで日本語→英語に翻訳する。
3. 英訳を `EnglishTagNormalizer` に通し、辞書の英語別名・正規タグと最長一致させる。
4. 見つかったタグを既存タグの先頭に追加する（重複は除去）。
5. 英訳全文を `PromptComposer` の `extraContext` として自然文に追加する。
6. 自然文は最大150語に収める。タグが0件でも英訳コンテキストだけで自然文を生成する。
7. 翻訳に失敗した場合はエラーを表示し、辞書だけの結果をそのまま使う。

### 英語→タグ正規化（`EnglishTagNormalizer.kt`）

- 同梱SQLiteの正規タグ5,003件と英語別名をトライに格納。
- 単語境界を確認するため、`chair` の中の `hair` のような誤一致を防ぐ。
- 最長一致を優先し、`sitting on a chair` のような文では `sitting` と `chair` を拾う。
- `sitting on` のような途中で切れた英語別名は、名詞を飲み込むため除外する。
- 正規タグ側の `shirt_tucked_in`、`k-on!`、`straight-on` は除外しない。
- 自然な英語の語順に対応するため、髪・目の属性を補完する:
  - `long black hair` → `long_hair` + `black_hair`
  - `very long hair` → `very_long_hair`（`long_hair` は重複除去）
  - `short hair` → `short_hair`
  - `blue eyes` → `blue_eyes`
- 色は black / brown / blonde / blue / pink / red / white / silver / grey / green /
  purple / orange / aqua / gold / yellow を対象にし、辞書に存在するタグだけ採用する。

### 翻訳の抽象化（`TextTranslator.kt` / `MlKitTextTranslator.kt`）

- `TextTranslator` インターフェース:
  - `isReady()`
  - `downloadModels()`
  - `translateJapaneseToEnglish()`
  - `translateEnglishToJapanese()`
  - `close()`
- `MlKitTextTranslator` は `com.google.mlkit:translate:17.0.3` を使用。
- 日本語→英語と英語→日本語の2モデルを必要時にダウンロードする。
- `RemoteModelManager` でダウンロード済みモデルを確認する。
- モデルは端末内に保存され、ダウンロード後はオフラインで翻訳できる。
- 初期化・モデル確認・翻訳の例外はUI側で捕捉し、辞書のみの動作へフォールバックする。

### UI

- 変換結果の「辞書外」カードに、ML Kitの英訳と追加タグ件数を表示する。
- 翻訳モデルが未準備の場合は、設定画面への導線カードを表示する。
- 設定画面に「オフライン翻訳（ML Kit）」カードを追加:
  - 状態（未ダウンロード / ダウンロード中 / 準備完了）
  - 「翻訳モデルをダウンロード（約60MB）」ボタン
  - エラー表示
  - Google Play Servicesが必要である旨の注意
- タグチップの下に、辞書の日本語別名を最大2件表示する（`japaneseMeanings`）。
- 辞書に日本語別名がないタグは、英語→日本語モデルで機械訳して補完する（8件ずつ）。
  訳文はキャッシュし、タグ編集のたびに同じ翻訳を繰り返さない。
- 翻訳コンテキストだけの結果も履歴に保存する。

### 安定したdebug署名

- `keystore/prompter-debug.jks` をリポジトリに追加（debug専用）。
- `app/build.gradle.kts` の `stableDebug` signingConfigで全debugビルドに使用。
- GitHub Actionsのdebug keystoreキャッシュ処理を削除。
- 証明書SHA-256:
  `6B:8A:27:78:33:4F:4F:49:A2:E1:5D:2E:40:1F:89:49:05:FF:0F:BB:2A:88:2F:1B:68:7C:02:B5:29:95:CC:33`
- これにより、今後CIでビルドするAPKは同じ署名になり、`adb install -r` で上書きできる。
- 0.4.2以前のAPKはCIごとに異なる一時鍵で署名されていたため、0.5.0の初回だけは
  0.4.2のアンインストールが必要。

### APKサイズ

- ML KitのJNIライブラリが全ABI分入ると約95MBになるため、`ndk.abiFilters` で
  `arm64-v8a` のみに限定した。
- 実機（arrows Alpha）はarm64-v8a。APKは約51MB。
- x86/x86_64のエミュレータにはインストールできない。

## テスト

CIで以下を実行し、すべて成功した。

```bash
python3 -m unittest tools.test_m1 -v   # 15 tests
./gradlew testDebugUnitTest --stacktrace  # 34 tests
./gradlew assembleDebug --stacktrace
```

M5で追加したKotlinテスト:

- `EnglishTagNormalizerTest`（5件）
  - 自然な英語から髪色・髪型・服装・ポーズ・場所をタグ化
  - `very long hair` と `long hair` の重複除去
  - `shirt_tucked_in` / `k-on!` など正規タグの保持
  - 翻訳テキストの自然文・タグへのマージ
  - 日本語別名の取得
- `PromptComposerTest`（3件追加、合計8件）
  - 翻訳コンテキストの自然文への追加
  - コンテキストのみでの自然文生成
  - 長すぎるコンテキストの150語への切り詰め

既存のM1〜M4テスト（`M1MatcherTest`、`TagEditingTest`、`HistoryStoreTest`、
`AiPromptValidatorTest`、`OpenRouterClientTest`、`AiCacheTest`、`AiSettingsTest`）も継続して成功。

## 検証結果

- GitHub Actions run: <https://github.com/shiokunn/prompter/actions/runs/40057958074>
- `Run M1 matcher tests`: 成功（15件）
- `Run Kotlin unit tests`: 成功（34件）
- `Build debug APK`: 成功
- APK: 約51MB（arm64-v8a）
- 共有ストレージ:
  `/storage/emulated/0/Download/Prompter/Prompter-0.5.0-m5-debug.apk`
- SHA-256: `43e013da255753fc81903f4d2813d576c5d715837c17742aea659d4bc4f3c0f5`
- 署名: `CN=Prompter Debug`、証明書SHA-256は上記の安定debug鍵と一致。

## 未完了・既知の制約

- 当環境にadb接続端末がないため、実機でのML Kitモデルのダウンロード、機内モードでの翻訳、
  タグ日本語表示の確認は未実施。
- ML Kit翻訳はGoogle Play Servicesとモデルダウンロードに依存する。利用できない端末では
  辞書のみで動作する。
- 翻訳品質はML Kitに依存する。短い辞書外断片では文脈が失われることがある。
- タグの日本語表示は同梱辞書の別名を優先し、別名がない場合だけML Kitの英語→日本語モデルを使う。
  モデル未ダウンロード時や翻訳失敗時は日本語が表示されないタグがある。
- ML Kitの機械訳はタグ名だけを訳すため、文脈によっては不自然な訳になる。
- 英語→タグ正規化は上位5,003タグの範囲。辞書外の正しいDanbooruタグは追加されない。
- 自然文は150語で打ち切るため、非常に長い翻訳コンテキストでは末尾が省略される。
- APKはarm64-v8a専用。x86エミュレータでは動かない。
- debug署名鍵はリポジトリに含まれる。公開リリースには使用しない。
- PixAI DiTの自然文の人手評価（目標3.5/5）は未実施。

## 次の作業

1. 0.4.2を一度アンインストールし、`Prompter-0.5.0-m5-debug.apk` をインストールする。
2. 設定→「翻訳モデルをダウンロード（約60MB）」を実行する。
3. 辞書外の日本語を含む入力で変換し、英訳・追加タグ・自然文を確認する。
4. 機内モードにして同じ変換を実行し、オフライン翻訳が動くことを確認する。
5. タグチップ下の日本語別名を確認する。
6. 次回CIビルド後、`adb install -r` で上書きできることを確認する。
7. M6のローカルLLM（Gemma 4 E2B）、PixAI自然文の人手評価、モデル自動取得へ進む。
