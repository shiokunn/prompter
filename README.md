# Prompter

日本語の自然言語を、**PixAI DiT 向けの英語プロンプト**と **Danbooru タグ**に変換する Android アプリの企画・調査リポジトリ。

- **GitHub**: <https://github.com/shiokunn/prompter>（private）
- **企画書**: [`PLAN.md`](PLAN.md)
- **確定要件**: [`research/DECISIONS.md`](research/DECISIONS.md)
- **PixAI DiT 調査**: [`research/PIXAI_DIT.md`](research/PIXAI_DIT.md)
- **オンデバイスモデル選定**: [`research/MODEL_SELECTION.md`](research/MODEL_SELECTION.md)
- **辞書スパイク**: [`research/SPIKE.md`](research/SPIKE.md)
- **M1 評価レポート**: [`research/M1_REPORT.md`](research/M1_REPORT.md)
- **M1 fresh holdout**: [`research/M1_FINAL_REPORT.md`](research/M1_FINAL_REPORT.md)
- **M2 実装レポート**: [`research/M2_REPORT.md`](research/M2_REPORT.md)
- **プロトタイプ**: [`tools/spike_match.py`](tools/spike_match.py)

## 状況

- 2026-10-23: 要件確定。開発環境と GitHub Actions の骨格を準備。
- 2026-10-23: 初回 CI 成功。`app-debug.apk`（約29.7MB）を Artifact から取得できることを確認。
- 2026-10-23: **M1 辞書パイプライン GO**。fresh holdout test 100件で
  Recall 0.5194 / Precision 0.7617 / Invalid 0.000%。
- 2026-10-23: **M2 コア完了**。Compose UI、Kotlin トライ、オフライン PixAI 自然文合成を実装。
  GitHub Actions で Python テスト、Kotlin 単体テスト（Robolectric 含む）、APK ビルドが成功。
- APK: `/storage/emulated/0/Download/Prompter/Prompter-0.2.0-m1-debug.apk`
  （SHA-256 `fe5fd796a68ee382f9d9199ffb2b0b57a6a365f713a188ad1e13f54a95bce503`）。
- 次の作業: M3 の履歴・お気に入り（Room）、ML Kit 翻訳フォールバック、タグ重み/ネガティブ。

## 要約

- 主対象は **PixAI DiT（Tsubaki.3 / Tsubaki.2）**。DiT はタグ列より **英語の自然文** が向く。
- 既定出力は `Subject + Action + Scene + Mood + Style` の英文。タグモードも併設。
- M1 のオフライン辞書は上位5,000タグ + 別名14,587件（SQLite 2.18MB）。
  将来は14万タグ規模へ拡張し、ML Kit 翻訳と組み合わせる。
- M2 で **Compose UI + Kotlin トライ + オフライン自然文合成** を実装済み。
  日本語入力から PixAI 向け英文（80〜150語）とタグ列を同時に生成できる。
- AI 補完は **OpenAI API 互換（既定: OpenRouter）** を任意 ON（BYOK）。
- AI を使わない人向けに **Gemma 4 E2B（2.58GB）** の任意ダウンロードを用意。
- APK は GitHub Actions でビルドし、Termux から `adb install` する。
- 実機は arrows Alpha（12GB RAM / 512GB）。成人向けタグはそのまま扱う。

## 開発環境（Termux）

APK のビルドは GitHub Actions 上で行うため、Termux 側に Android SDK は不要。以下を導入済み。

| ツール | 用途 |
|---|---|
| Git / GitHub CLI (`gh`) | リポジトリ操作、GitHub 認証 |
| OpenJDK 21 | Gradle / Kotlin |
| Gradle Wrapper 9.5.0 | ローカル設定確認・CI と同一バージョン |
| Kotlin 2.4.0 | 構文確認用（ビルドは Gradle 内の Kotlin を使用） |
| Android platform-tools (`adb`) | 実機への APK インストール |
| `jq` / `zip` / `wget` | 調査・成果物の操作 |

```bash
# 初回のみ（認証は対話式）
gh auth login

# ローカルの Gradle 設定確認
./gradlew --version

# M1 辞書マッチャの回帰テスト
python3 -m unittest tools.test_m1 -v

# Kotlin 単体テスト（Android SDK が必要。Termux では CI に任せる）
./gradlew testDebugUnitTest
```

## APK のビルド（GitHub Actions）

`main` ブランチへの push、または Actions 画面からの手動実行（`workflow_dispatch`）でビルドする。

1. GitHub の **Actions → build-apk → Run workflow** を実行
2. 完了後、Artifact `prompter-debug-apk` をダウンロード
3. Termux で実機にインストール

```bash
# GitHub CLI で最新の Artifact を取得する場合
gh run download -n prompter-debug-apk
adb install -r app-debug.apk
```

ブラウザからダウンロードした場合は ZIP を展開してからインストールする。

現在の APK は `0.2.0-m1`（versionCode 2、debug 署名）。M1 の辞書と M2 のオフライン変換 UI を含む。

旧ビルドの debug APK が既に入っている場合、GitHub Actions の debug 署名がビルドごとに変わり
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` になることがある。その場合は一度アンインストールしてから入れる。

```bash
adb uninstall com.prompter.app
adb install Prompter-0.2.0-m1-debug.apk
```

## 調査の再現

```bash
python3 tools/spike_match.py
python3 tools/build_db_spike.py
```

データは `research/data/` にダウンロードする（`.gitignore` 済み）。

## 主な結論

| 項目 | 決定 |
|---|---|
| 第一対象 | PixAI DiT（Tsubaki.3 / Tsubaki.2） |
| 既定出力 | 英語自然文 80〜150語 |
| タグ出力 | PixAI Haruka v2 / SDXL / Pony / A1111 / ComfyUI / NovelAI |
| AI | OpenRouter / OpenAI 互換、任意 ON、BYOK |
| オフライン AI | Gemma 4 E2B（任意）、E4B（12GB 端末限定） |
| 配布 | 自分用 APK |
| 成人向け | フィルタしない（個人用） |
