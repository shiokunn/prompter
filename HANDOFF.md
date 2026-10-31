# Prompter 引き継ぎ

- 更新日: 2026-10-31
- リポジトリ: <https://github.com/shiokunn/prompter>（public）
- 作業ブランチ: `main`
- 最新コードコミット: `1f53395` 以降（キャラクターモードの丸ボタン、公開配布構成、README刷新。詳細は `git log -5 --oneline`）
- 現在の作業ツリー: クリーン（この引き継ぎ記録を含むコミット後）
- アプリ: 公開版 `0.10.0`（versionCode 24、`com.prompter.app.release`）、開発版 `com.prompter.app`
- 実機: arrows Alpha。ADB端末が複数表示される場合、対象を明示して操作する。

## 現在の状態

M1〜M9相当の機能実装が進み、版権キャラクターの手動確認、登録キャラクターDB、外部候補検索、履歴、設定画面が揃っている。DuckDuckGoのHTML版チャレンジを調査しLite版を利用する。候補は確定名ではなく参照情報として表示し、ユーザーが明示的に選択・確認する。別形態は別候補として扱う。

直近の変更では、履歴・設定UI、作品名を考慮した候補ランキング、かな・全半角等の正規化、AniListからWikidataへのフォールバック、DuckDuckGo広告候補除外・エラー識別を実装した。

## 検証記録

- 公開配布準備（`0.10.0`）:
  - キャラクターモードのスイッチを、48dp のタップ領域を持つ丸い円（選択時に中心を塗りつぶし）へ変更。`ScreenInteractionTest` に ON/OFF とパネル表示のテストを追加し、実機 `127.0.0.1:5555` でも円のタップでパネルが開くことを確認。
  - 公開版アプリIDを `com.prompter.app.release` に分離し、開発版と共存可能にした。debug APK の表示名は「Prompter Dev」。
  - 依存ライブラリの同梱ABIを調査し、`arm64-v8a` と `x86_64` を含めた。32bit端末は対象外。
  - debug CI run [41154799190](https://github.com/shiokunn/prompter/actions/runs/41154799190) 成功（M1 17、Kotlin 128、APKビルド、パッケージ/ABI/16KB整合の検証）。
  - release CI run [41155081899](https://github.com/shiokunn/prompter/actions/runs/41155081899) 成功。`Prompter-0.10.0.apk`（約123MB）、`com.prompter.app.release`、versionCode 24、`arm64-v8a` + `x86_64`、v2署名、証明書 SHA-256 `7F:B2:...:D0:DB` を確認。
  - 互換性CI run [41156886414](https://github.com/shiokunn/prompter/actions/runs/41156886414) 成功。Android 10 / 13 / 14 / 15 / 16 の x86_64 エミュレータで、同梱DBの読み込みと `MainActivity` 起動を確認。
  - 新規のRSA 4096bit署名鍵を作成し、GitHub Secrets 4件に登録。keystoreと控えはリポジトリ外の `~/.prompter-release/` に保管し、リポジトリには含めない。
  - README を一般利用者向けに刷新し、MIT LICENSE を追加。リポジトリは public に変更。
  - 詳細は `research/public-release-and-device-compatibility.md`。

- GitHub Actions run [41065666386](https://github.com/shiokunn/prompter/actions/runs/41065666386) は `04aadcd` に対して成功。M1 15件、Kotlin 124件（失敗0・エラー0・スキップ0、19 XMLファイル）、debug APKビルド成功。設定・履歴のヘッダー「戻る」を削除し、Androidのシステム戻る操作のみで閉じる変更を実機 `127.0.0.1:5555` で確認。両画面ともヘッダーに「戻る」は表示されず、システム戻るでメイン画面へ戻った。設定の未保存変更がある状態でシステム戻るを押すと「変更を破棄しますか？」が表示され、キャンセルと破棄の両方を確認。クラッシュ・アプリ例外なし。
- プロンプト→画像確認: 初回PixAI画像では `木の椅子` が `chair` と `tree` に分解され、助詞だけの追加文も残った。その後、修正APK（CI `41072431550`）を `127.0.0.1:5555` に `adb install -r` で更新インストール。履歴の同じ情景を再変換し、`on a wooden chair` 相当の自然文が出て、`tree` と不自然な追加文が消えたことを確認。ユーザーがPixAIで作成した画像も端末上で確認し、銀髪、赤いドレス、木製椅子、夜の街が反映され、大きな木は見られず、追加生成不要と判断。詳細は `research/header-navigation-and-prompt-image-check.md`。
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

- `d168c7b` / `465e323`: 「木の椅子」等6表記を正式タグ `wooden_chair` に最長一致させ、自然文で `on a wooden chair` と出力。助詞のみの未一致断片を翻訳前に除去し、英訳後にも機能語のみの断片を除去。保存済み履歴の文章は変更しない。
- `promptdb.sqlite` をローカルの元CSVの投稿数順上位10,000件ちょうどに更新（別名25,205、日本語別名付きタグ6,986）。最新オンラインランキングではない。Python回帰17件成功。初回CI `41071768825` は新テストのローカル専用CSV参照で失敗したため、タグ集合のハッシュ照合を追加し `465e323` で修正。再CI [41072431550](https://github.com/shiokunn/prompter/actions/runs/41072431550) 成功: M1 17件、Kotlin 128件（失敗0・エラー0・スキップ0）、debug APKビルド成功。APKを実機 `127.0.0.1:5555` に更新インストールして正常起動を確認、履歴の同じ情景を再変換。ユーザーがPixAIで作成した結果画像は意図に合い、大きな木は見られなかった。追加生成なし。
- 設定・履歴のヘッダーから「戻る」ボタンを削除し、Androidのシステム戻る操作で閉じるよう変更（`04aadcd`）。CI `41065666386` 成功、実機確認済み。テストは設定画面に戻るボタンがないことを確認する形へ更新。
- 実プロンプトをPixAIで画像化し、入力との一致を確認。画像は再生成不要。元プロンプトで見つかった `木の椅子` の `tree` 分解と助詞だけの `Of / in / /.` 混入は `d168c7b` で修正済み。新APKの実機確認・再生成は未実施。
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
