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
- **M3 実装レポート**: [`research/M3_REPORT.md`](research/M3_REPORT.md)
- **M4 実装レポート**: [`research/M4_REPORT.md`](research/M4_REPORT.md)
- **プロトタイプ**: [`tools/spike_match.py`](tools/spike_match.py)

## 状況

- 2026-10-23: 要件確定。開発環境と GitHub Actions の骨格を準備。
- 2026-10-23: 初回 CI 成功。`app-debug.apk`（約29.7MB）を Artifact から取得できることを確認。
- 2026-10-23: **M1 辞書パイプライン GO**。fresh holdout test 100件で
  Recall 0.5194 / Precision 0.7617 / Invalid 0.000%。
- 2026-10-23: **M2 コア完了**。Compose UI、Kotlin トライ、オフライン PixAI 自然文合成を実装。
  GitHub Actions で Python テスト、Kotlin 単体テスト（Robolectric 含む）、APK ビルドが成功。
- 2026-10-24: **M3 実装完了（実機確認待ち）**。タグチップの削除・並び替え・重み付け、履歴・お気に入りを実装。
  共有ボタンは削除し、コピー中心のUIに変更。
- 2026-10-24: **M4 実装完了（実キー・実機確認待ち）**。OpenRouter AI補完、設定画面、Keystore暗号化、
  JSON Schema、タグ検証、キャッシュを実装。
- APK: `/storage/emulated/0/Download/Prompter/Prompter-0.4.0-m4-debug.apk`
  （SHA-256 `d75dce08a2ab12349bfcc64ccf3a4c4480599e3007733dbc61f69dab6ccbe5d3`）。
- 次の作業: 実機でAPIキーを設定してAI補完を確認、またはM5 ML Kit翻訳フォールバック。

## 要約

- 主対象は **PixAI DiT（Tsubaki.3 / Tsubaki.2）**。DiT はタグ列より **英語の自然文** が向く。
- 既定出力は `Subject + Action + Scene + Mood + Style` の英文。タグモードも併設。
- M1 のオフライン辞書は上位5,000タグ + 別名14,587件（SQLite 2.18MB）。
  将来は14万タグ規模へ拡張し、ML Kit 翻訳と組み合わせる。
- M2 で **Compose UI + Kotlin トライ + オフライン自然文合成** を実装済み。
  日本語入力から PixAI 向け英文（80〜150語）とタグ列を同時に生成できる。
- M3 で **タグ編集（重み・並び替え・削除）と履歴・お気に入り** を追加。
  履歴は端末内のSQLiteに保存し、変換結果をすぐ呼び出せる。
- M4 で **OpenRouter AI補完** を追加。設定画面でBYOKのAPIキーを登録し、
  オフライン結果を「AIで整える」ことができる。キーはAndroid Keystoreで暗号化して保存する。
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

現在の APK は `0.4.0-m4`（versionCode 4、debug 署名）。M1 の辞書、M2 のオフライン変換、M3 のタグ編集・履歴、M4 のAI補完を含む。

旧ビルドの debug APK が既に入っている場合、CIは debug keystore をキャッシュして再利用するため
通常は上書きインストールできる。キャッシュが消えた場合だけ
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` になるので、その場合は一度アンインストールしてから入れる。

```bash
adb uninstall com.prompter.app
adb install Prompter-0.4.0-m4-debug.apk
```

### AI補完の使い方

1. アプリ右上の **設定** を開く
2. OpenRouterのAPIキーを入力して保存（キーは端末内で暗号化）
3. 日本語を入力して **変換する**
4. 結果の下にある **AIで整える** を押す

APIキーはチャット・リポジトリ・APK・履歴には保存しない。実キーでの動作確認は未実施。

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
