# Prompter 引き継ぎ

- 更新日: 2026-10-30
- リポジトリ: <https://github.com/shiokunn/prompter>（private）
- 作業ブランチ: `feature/character-mode-ui`
- 最新コミット: `a366d12`（詳細は `git log -1 --oneline` で確認）
- 現在の作業ツリー: clean（引き継ぎ記述を更新する前の状態）
- アプリ: versionName `0.9.1-m9`, versionCode `23`
- 実機: arrows Alpha。ADB端末が複数表示される場合、対象を明示して操作する。

## 現在の状態

M1〜M9相当の機能実装が進み、版権キャラクターの手動確認、登録キャラクターDB、外部候補検索、履歴、設定画面が揃っている。DuckDuckGoのHTML版チャレンジを調査しLite版を利用する。候補は確定名ではなく参照情報として表示し、ユーザーが明示的に選択・確認する。別形態は別候補として扱う。

直近の変更では、履歴・設定UI、作品名を考慮した候補ランキング、かな・全半角等の正規化、AniListからWikidataへのフォールバック、DuckDuckGo広告候補除外・エラー識別を実装した。

## 検証記録

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

- ユーザー指示により、キャラクター検索をローカルDB中心に変更中。AniList/Wikidataの外部候補検索とDuckDuckGo（アプリ内・ブラウザ起動）を削除し、DB候補の明示選択と手動英語名入力は維持。
- 実装差分は未コミット。GitHub Actions検証は未実施（変更をpushしていないため）。次は差分確認後、ユーザー指示に従ってCI検証する。

## 作業時の注意

- ユーザーの現在の明示指示を優先する。現在はローカルDB中心の検索への変更が許可されている。コミット・pushは明示指示なしに行わない。
- 履歴・AIキー・設定を保持すること。ADBインストールは必要時 `adb install -r` のみ。`pm clear`、アンインストール、DB削除は避ける。
- ADB端末は `adb devices -l` で確認し、必ず `-s <serial>` を指定する。複数端末の片方が不安定だった履歴がある。
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
