# M2 — アプリ骨格とオフライン変換

- 日付: 2026-10-23
- 状態: **コア完了**（履歴/Room は M3）
- 対象: `app/` / `tools/m1_*` / GitHub Actions

## 目的

M1 で作った日本語→Danbooru 辞書を Android アプリに載せ、日本語入力から

1. PixAI DiT 向けの英語自然文
2. Danbooru タグ列

をオフラインで生成できるようにする。M2 の受け入れ条件は「日本語→PixAI 自然文がオフラインで動く」こと。

## 実装

### UI（Jetpack Compose）

- 日本語の複数行入力
- 出力モード: `両方` / `PixAI自然文` / `Danbooruタグ`
- 例文チップ
- コピー / 共有
- 変換中インジケータ、エラー表示
- 辞書外テキストの表示
- ダーク/ライトテーマ
- オフライン辞書のタグ数・別名数をフッターに表示

### 辞書の同梱

- `app/src/main/assets/promptdb.sqlite` を `PromptDb` がアプリ内部ストレージへコピー
- `androidResources.noCompress += "sqlite"` により `AssetManager.openFd()` でサイズを確認可能
- 読み込みは `SQLiteDatabase.OPEN_READONLY`

### Kotlin マッチャ

`tools/m1_match.py` の判定を Kotlin に移植した。

- NFKC / 小文字化 / カタカナ→ひらがな / 句読点除去
- 日本語別名トライの最長一致
- かな境界・単漢字境界のガード
- 色 + 名詞の合成ルール（例: 青いドレス → `blue_dress`）
- サイズ系ルール（長い髪、大きな胸など）
- 親タグ展開（例: `blue_dress` → `dress`）
- 推論（`1girl` / `1boy` / `2girls` / `multiple_girls` / `solo` / `hetero` / `yuri`、成人向けの保守的な推論）
- 同一タグの重複除去

### PixAI プロンプト合成

`PromptComposer` がタグを以下の層に分けて英文を組み立てる。

`Subject + Action + Scene + Mood + Style`

- 主語: `1girl` / `1boy` / `2girls` / `2boys` / `multiple_girls` / `hetero` / `yuri` / `solo`
- 髪・目・服装・ポーズ・場所・表情・画風
- 複数主語では `They are ...` / `They have ...`
- 冠詞（`a blue dress`, `an orange dress`）と複数名詞（`thighhighs`, `boots`）の調整
- 80語未満のときは構図・光・背景の補足文で 80〜150 語に調整
- 成人向けタグは `Explicit adult elements are present: ...` として**フィルタせず原文のタグ名で出力**
- タグモードは canonical tag をそのままカンマ区切りで出力

## テスト

CI で以下を実行する。

```bash
python3 -m unittest tools.test_m1 -v
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

追加した Kotlin テスト:

- `PromptComposerTest`（5件）
  - 80〜150語の範囲
  - 複数主語の文法
  - 成人向けタグの非フィルタ
  - 親タグの重複表示防止
  - 空入力
- `M1MatcherTest`（3件、Robolectric SDK 35）
  - 実 asset DB の読み込み
  - 日本語文からの主要タグ
  - 二人の少女の推論
  - 裸の少女の成人向けタグ

## 検証結果

- GitHub Actions run: <https://github.com/shiokunn/prompter/actions/runs/39962415633>
- `Run M1 matcher tests`: 成功
- `Run Kotlin unit tests`: 成功
- `Build debug APK`: 成功
- APK: 約31MB、`assets/promptdb.sqlite`（2,289,664 bytes）を同梱
- 共有ストレージ: `/storage/emulated/0/Download/Prompter/Prompter-0.2.0-m1-debug.apk`
- SHA-256: `90f73f361ff62fdf8ec62dfc910df7338b9691a8a37487f158e42391843f68b7`

## 未完了・既知の制約

- Room による履歴・お気に入り・修正学習は未実装（M3）
- ML Kit Translation は未接続。現在の自然文はタグ名ベースのテンプレート合成
- Gemma 4 E2B / OpenRouter の AI 補完は未接続
- 実機（arrows Alpha）へのインストールは adb 未接続のため未実施
- PixAI DiT の最大プロンプト長・成人向けモデレーションは未検証
- リリース署名は未設定（debug 署名のみ）
- タグの重み付け、ネガティブプロンプト、モデル別プリセットは未実装

## 次の作業

1. M3: Room 履歴、お気に入り、コピー履歴、入力の再利用
2. ML Kit Translation のフォールバック（辞書外の日本語を英訳）
3. タグ重み・ネガティブ・モデル別プリセット
4. 実機で DB 読み込み時間、変換時間、メモリを計測
5. OpenRouter キーが用意できたら AI 補完と逆翻訳検証
