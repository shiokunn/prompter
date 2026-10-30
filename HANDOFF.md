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

## 次に進めるなら

現タスク（DuckDuckGo検索診断、キャラクター候補精度、履歴・設定UI）は実装とCI検証済み。ユーザーは現在、追加機能の「提案のみ」を求めており、コード変更を許可していない。まずは提案を提示し、実装に着手する前に対象と優先順位の承認を得る。

候補機能（まだ未着手・優先順位は要相談）:
1. 履歴のバックアップ／エクスポート・復元（既存データ保護を優先）。
2. キャラクター候補の根拠表示（作品名・ソース・同名候補の区別）。
3. 履歴の追加検索・フィルター・並べ替え。
4. 検索エラー後の再試行導線。

## 作業時の注意

- ユーザーの現在の指示を優先する。直近の明示制約は「コードなどの変更は絶対にしない」であり、提案だけを求めている。許可が出るまでファイル編集、コミット、push、設定保存、データ削除をしない。
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
