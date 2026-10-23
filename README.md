# Prompter

日本語の自然言語を、**PixAI DiT 向けの英語プロンプト**と **Danbooru タグ**に変換する Android アプリの企画・調査リポジトリ。

- **企画書**: [`PLAN.md`](PLAN.md)
- **確定要件**: [`research/DECISIONS.md`](research/DECISIONS.md)
- **PixAI DiT 調査**: [`research/PIXAI_DIT.md`](research/PIXAI_DIT.md)
- **オンデバイスモデル選定**: [`research/MODEL_SELECTION.md`](research/MODEL_SELECTION.md)
- **辞書スパイク**: [`research/SPIKE.md`](research/SPIKE.md)
- **プロトタイプ**: [`tools/spike_match.py`](tools/spike_match.py)

## 状況

- 2026-10-23: 要件確定。開発環境と GitHub Actions の骨格を準備。
- 次の作業: 上位5,000タグの別名生成パイプライン（M1）→ アプリ骨格の実装（M2）。
- `app/` は CI の疎通確認用の最小 Compose 画面。APK は GitHub Actions でビルドする。

## 要約

- 主対象は **PixAI DiT（Tsubaki.3 / Tsubaki.2）**。DiT はタグ列より **英語の自然文** が向く。
- 既定出力は `Subject + Action + Scene + Mood + Style` の英文。タグモードも併設。
- オフライン辞書（14万タグ、SQLite 17MB）+ ML Kit 翻訳で下書きできる。
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
```

## APK のビルド（GitHub Actions）

`main` ブランチへの push、または Actions 画面からの手動実行（`workflow_dispatch`）でビルドする。

1. GitHub の **Actions → build-apk → Run workflow** を実行
2. 完了後、Artifact `prompter-debug-apk` をダウンロード
3. Termux で実機にインストール

```bash
unzip prompter-debug-apk.zip
adb install -r app-debug.apk
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
