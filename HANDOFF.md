# Prompter 引き継ぎ

- 更新日: 2026-10-30
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 作業ブランチ: `main`
- 最新コードコミット: `b44789e`（情景候補検出。詳細は `git log -1 --oneline` で確認）
- 現在の作業ツリー: クリーン（この引き継ぎ記録を含むコミット後）
- アプリ: versionName `0.9.1-m9`, versionCode `23`
- 実機: arrows Alpha。ADB端末が複数表示される場合、対象を明示して操作する。

## 現在の状態

M1〜M9相当の機能実装が進み、版権キャラクターの手動確認、登録キャラクターDB、外部候補検索、履歴、設定画面が揃っている。DuckDuckGoのHTML版チャレンジを調査しLite版を利用する。候補は確定名ではなく参照情報として表示し、ユーザーが明示的に選択・確認する。別形態は別候補として扱う。

直近の変更では、履歴・設定UI、作品名を考慮した候補ランキング、かな・全半角等の正規化、AniListからWikidataへのフォールバック、DuckDuckGo広告候補除外・エラー識別を実装した。

## 検証記録

- GitHub Actions run [41062349076](https://github.com/shiokunn/prompter/actions/runs/41062349076) は `b44789e` に対して成功。M1 15件、Kotlin 124件（失敗0・エラー0・スキップ0、19 XMLファイル）、debug APKビルド成功。情景候補の実機表示・タップ操作は未検証。
- ADB UI確認: CI run `41062349076` のAPKを `adb install -r` で `127.0.0.1:5555` にインストール。キャラクターモードをONにし、情景 `Cinderella is dancing` から「シンデレラ / 勝利の女神：NIKKE · Cinderella」の候補ブロックが表示されることを確認。候補表示前は未選択、候補をタップすると「確認済み」カードに切り替わった。プロンプト生成は実行していない。Prompterのクラッシュやアプリ例外は観測されず、MainActivityが前面に残った。アプリデータは消去せず、`emulator-5554` は操作していない。
- GitHub Actions run [41028250624](https://github.com/shiokunn/prompter/actions/runs/41028250624) は `a366d12` に対して成功。
  - Kotlin/JUnit: 115件、失敗0・エラー0・スキップ0（JUnit XML 20ファイル）。
  - `python3 -m unittest tools.test_m1 -q`: 15件成功。
  - `testDebugUnitTest` と `assembleDebug` 成功。
- 成果物APKを取得し `adb install -r` で更新インストール、MainActivity起動を確認。アプリデータは消去していない。
- ADBでホーム、履歴、設定画面を目視確認。履歴レコードが残っていること、設定のモデル・辞書・翻訳状況と保存操作の表示を確認。
- ユーザー自身が操作テストを実行し成功したと報告。直近のADB操作は一部不安定で、こちらでは全操作を再現できていない。ユーザーの成功報告を尊重し、未確認の個別ケースを「こちらで検証済み」とは扱わない。
- 作品名のKotlin単体確認: Re:ゼロ正式日本語表記、半角カナ、鳴潮/Wuthering Waves、原神/Genshin Impactが対応する英語名と一致。
- 本タスクのコミットは既にリモートへpush済み。追加コミットやpushはユーザーの指示なしに行わない。

## 現在の作業

- キャラクター検索はローカルDB中心。AniList/WikidataとDuckDuckGoの検索導線を削除済み。コミット `a5f66b8`・`a617bc3`、CI run [41042089838](https://github.com/shiokunn/prompter/actions/runs/41042089838) 成功。
- `46bddb9` までpush済み。作品・ゲーム・サービス名入力、キラキラアイコン付き「AIで整える」操作、モデル名を表に出さない表示、自然文出力一本化を実装。
- GitHub Actions run [41048459142](https://github.com/shiokunn/prompter/actions/runs/41048459142) はM1テスト、Kotlinユニットテスト、debug APKビルド成功。
- 今回のAPKは `adb install -r` で `127.0.0.1:5555` にインストールし、キャラクター `Hatsune miku` とサービス `VOCALOID` を使った自然文生成・履歴保存を確認。生成文に `Wear / / in /.` が混入する不自然な訳文を確認したが、ユーザー指示により修正していない。AIボタンの起動は未テスト（課金・モデル実行を避けた）。タグ形式UIとモデル名がメイン画面にないことを確認。テスト用履歴1件を追加。`emulator-5554` は未変更。
- GPU機能のADBテスト: `127.0.0.1:5555`（arrows Alpha）で設定のGPU選択・保存後、既存サンプルから「AIで整える」を実行。GPU選択がチェック状態で保存されていること、生成文が約2分で表示され、GPU初期化エラーやPrompterクラッシュがないことを確認。結果履歴が1件追加された。アプリデータは消去せず、`emulator-5554` は操作していない。
- キャラクター名の自動検出を調査（`research/character-autodetection.md`）。現状のDB部分一致は2文字以下の一般語名で誤検出が多く、完全自動確定は不適切。3文字以上の正式名＋英語単語境界＋2文字名は作品名文脈がある場合のみ、という条件で候補表示する案を推奨。DBは10作品のみのため初音ミク等は対象外で、未知キャラクターを扱うにはDB拡張かAI抽出＋手動確認が必要。
- 情景からのキャラクター候補検出を実装（`4b951a0`、境界修正 `e26dbf7`、重複解決 `b44789e`）。
  - `tools/build_character_db.py`: 「は別名」前の最後の文だけを採用して `二次資料。うまる` の混入を除去。別名に `detect_tier` を追加（0=検出しない、1=作品名がある場合のみ、2=候補可）。`D`/`K` は検索用に残し検出対象外。`characterdb.sqlite` は621人・1,546別名で再生成。
  - `CharacterMentionDetector`: 日本語はカタカナ→ひらがな変換をせず、空白・区切りを保った位置情報で境界を判定。1文字除外、2文字は作品名必須、英語は単語境界必須、最長一致、正式名優先、作品名による同名絞り込み。最大8件を候補として返し、自動確定はしない。
  - `CharacterPanel`: 名前欄の下に「情景の候補」を既存の候補ブロックで表示。タップするまで名前・作品・英語名へ反映しない。
  - `MainActivity`: キャラクターモードON・名前欄が空・未確定のときだけ180msデバウンスで検出。
  - テスト: `CharacterMentionDetectorTest`（一般語、1/2文字、複合語、英語境界、カタカナ/ひらがな境界、作品絞り込み、同名複数候補、最長一致、別形態優先、空白表記によるbase/variant解決）、`ScreenInteractionTest`（候補タップ必須）、`CharacterDbTest`（注記混入なし）。Python M1 15件成功。GitHub Actions run `41062349076` は M1 15件、Kotlin 124件（失敗0・エラー0・スキップ0）、debug APKビルドが成功。実機では `127.0.0.1:5555` で候補表示とタップ後の確認状態を確認。プロンプト生成・出力確認は未実施。
  - 未解決: `銀狼LV.999` と `銀狼 LV.999` は台帳の「別名統合禁止」に従い別レコードのまま保持する。検出時は入力の空白表記に合わせて片方だけ候補表示し、データ統合はしない。DB外キャラクター（初音ミク等）は検出できない。

## 作業時の注意

- ユーザーの現在の明示指示を優先する。APKビルド目的のコミット・pushはユーザーの事前許可済み。実機テストは許可がある場合のみ行う。
- 履歴・AIキー・設定を保持すること。ADBインストールは必要時 `adb install -r` のみ。`pm clear`、アンインストール、DB削除は避ける。
- ADB端末は `adb devices -l` で確認し、必ず `-s <serial>` を指定する。APKの更新インストールはユーザーが許可。ADBを使ったテスト・操作は事前にユーザー許可を得る。複数端末の片方が不安定だった履歴がある。
- Android SDKはTermuxにない。AndroidのKotlinテストとAPKビルドはGitHub Actionsを利用する。
- CI手順は `.github/workflows/build-apk.yml` を確認。`workflow_dispatch` が利用できる。
- DuckDuckGo LiteのHTTP結果は時点・ネットワーク依存。チャレンジ回避を試みない。ソースリンクは候補の根拠であり、本人確認済み情報ではない。
- `research/duckduckgo/diagnosis.md` にエンドポイント調査を記録。

## セッション開始時の確認

```bash
git status --short
git branch --show-current
git log -5 --oneline
adb devices -l
```

必要に応じて `.opencode/DEV_PROGRESS.md` と実際のGit状態を照合する。進捗ファイルは旧情報を含む可能性があるため、このHANDOFFとコード・CI記録を合わせて判断する。
