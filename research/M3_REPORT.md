# M3 — タグ編集と履歴

- 日付: 2026-10-24
- 状態: **実装完了（実機確認待ち）**
- 対象: `app/src/main/java/com/prompter/app/` / `app/src/test/java/com/prompter/app/`
- APK: `Prompter-0.3.0-m3-debug.apk`（versionCode 3）

## 目的

M2 の「日本語 → PixAI 自然文 / Danbooru タグ」変換に、変換後の手直しと再利用を追加する。

- 変換結果のタグをチップとして編集する
- タグの重み・並び順・削除を反映する
- 変換履歴をお気に入り付きで端末内に保存する
- 出力の共有ボタンを廃止し、コピー中心の操作にする

## 実装

### 出力UI

- 変換後の「共有」ボタンを削除。コピーとテキスト選択は維持。
- `PixAI自然文` / `Danbooruタグ` / `両方` の切替は M2 から継続。
- タグを編集すると、タグ出力と PixAI 自然文をその場で再生成する。

### タグ編集（`TagEditor.kt` / `TagEditing.kt`）

- 一致タグを `FilterChip` として表示。
- チップをタップすると重みを循環:
  - `1.0` → `1.2` → `1.4` → `1.0`
- `↑` / `↓` でタグの並び順を変更。
- `×` でタグを削除。
- 重み付きタグはタグ出力で `(tag:1.2)` 形式になる。
- 自然文はタグ名だけを使うため、重みは自然文には反映しない（UI に明記）。
- タグをすべて削除した場合は専用の空状態を表示。

### 履歴（`HistoryStore.kt` / `HistoryScreen.kt`）

- 端末内 SQLite（`prompter_history.db`）に保存。
- 変換時に自動保存し、タグ編集時は同じ履歴を 300ms デバウンスで更新。
- 保存項目:
  - 入力文
  - PixAI 自然文
  - 重み付きタグ列
  - 辞書外テキスト
  - 作成日時
  - お気に入り
- 履歴画面:
  - 新しい順に最大100件
  - 「すべて」/「お気に入り」フィルタ
  - お気に入り切替（★/☆）
  - 履歴を開いて入力・出力・タグ編集状態を復元
  - 1件削除 / 全削除（確認ダイアログ付き）
- 履歴 DB は辞書 DB と分離し、辞書は読み取り専用のまま。

### 依存関係

Room は使わず、`SQLiteOpenHelper` による小さな履歴ストアにした。
M3 の目的である履歴・お気に入り・復元には十分で、KSP/KAPT の追加なしに CI を安定させるため。

## テスト

CI で以下を実行し、すべて成功した。

```bash
python3 -m unittest tools.test_m1 -v
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

Kotlin テストは合計13件:

- `PromptComposerTest`（5件）
- `M1MatcherTest`（3件、Robolectric SDK 35）
- `TagEditingTest`（3件）
  - 重み付きタグの整形・パース
  - 通常タグのパース
  - 重みの循環
- `HistoryStoreTest`（2件、Robolectric SDK 35）
  - 追加・お気に入り・更新・削除
  - 全削除

## 検証結果

- GitHub Actions run: <https://github.com/shiokunn/prompter/actions/runs/40048728287>
- `Run M1 matcher tests`: 成功
- `Run Kotlin unit tests`: 成功
- `Build debug APK`: 成功
- APK: 約31MB
- 共有ストレージ: `/storage/emulated/0/Download/Prompter/Prompter-0.3.0-m3-debug.apk`
- SHA-256: `43616094c475215f33b6f21a70ca3f26b2076189ec4f797a8b0e3e327eecc3ed`
- debug 署名のため、同じ keystore cache が残っている限り上書きインストール可能。

## 未完了・既知の制約

- 実機（arrows Alpha）へのインストールと操作確認は未実施（adb 未接続）。
- 履歴の検索・エクスポート・クラウド同期は未実装。
- タグの並び替えはドラッグ&ドロップではなく `↑` / `↓` ボタン。
- 自然文への重み反映、ネガティブプロンプト、モデル別プリセットは未実装。
- Room は未使用。履歴は `SQLiteOpenHelper`。
- ML Kit Translation、OpenRouter AI、Gemma 4 ローカル LLM は未接続。
- PixAI DiT の最大プロンプト長・成人向けモデレーションは未検証。
- リリース署名は未設定。

## 次の作業

1. 実機で起動・変換・タグ編集・コピー・履歴復元を確認する。
2. OpenRouter キーが用意できたら M4 の AI 補完に進む。
3. キー待ちの間は M5 の ML Kit 翻訳フォールバックを進める。
4. タグのドラッグ並び替え、ネガティブプロンプト、プリセットは M7 前に追加する。
