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
- **M5 実装レポート**: [`research/M5_REPORT.md`](research/M5_REPORT.md)
- **M6 実装レポート**: [`research/M6_REPORT.md`](research/M6_REPORT.md)
- **M6 実機評価（10ケース）**: [`research/M6_LOCAL_EVAL.md`](research/M6_LOCAL_EVAL.md)
- **M6 プロンプトv7 再評価**: [`research/M6_PROMPT_V7.md`](research/M6_PROMPT_V7.md)
- **M7 UI刷新・OpenRouter診断・ローカルv9**: [`research/M7_REPORT.md`](research/M7_REPORT.md)
- **M7 クラウド生成検証（有効キー・上限なし）**: [`research/M7_CLOUD_VERIFY.md`](research/M7_CLOUD_VERIFY.md)
- **M7 ローカルv9 10ケース評価**: [`research/M7_LOCAL_V9_EVAL.md`](research/M7_LOCAL_V9_EVAL.md)
- **M7 ローカルv13 忠実さ・タグ整合性の再評価**: [`research/M7_LOCAL_V13_EVAL.md`](research/M7_LOCAL_V13_EVAL.md)
- **PixAI自然文 人手評価結果（8ケース）**: [`research/PIXAI_EVAL.md`](research/PIXAI_EVAL.md)
- **v13 人手再採点キット（基準・プロンプト・8ケース全文）**: [`research/M7_V13_RERATE.md`](research/M7_V13_RERATE.md)
- **M7 ローカルv9 20回連続生成の安定性**: [`research/M7_STABILITY.md`](research/M7_STABILITY.md)
- **引き継ぎ指示書**: [`HANDOFF.md`](HANDOFF.md)
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
- 2026-10-24: **M4 実装完了（実通信確認済み、既定 `openrouter/free`）**。OpenRouter AI補完、設定画面、
  Keystore暗号化、JSON Schema、タグ検証、キャッシュを実装。Gemma 4 31Bの無料プリセットは
  レート制限が厳しいため、無料モデル自動ルーター `openrouter/free` に置き換えた。
  `content: null` や不正JSONが返った場合は `json_object` で1回再試行する。
- 2026-10-24: **M5 実装・実機確認完了**。ML Kit翻訳フォールバック、英語→タグ正規化、
  タグの日本語別名表示を追加。CIのdebug署名鍵を固定し、今後のAPKを上書き可能にした。
  ユーザー実機（arrows Alpha）でモデルDL、機内モード翻訳、タグ和訳、上書き更新を確認済み。
- 2026-10-25: **M6 ローカルLLM 実機評価完了（0.6.1-m6）**。LiteRT-LM 0.18.1 +
  Gemma 4 E2B（約2.59GB）を任意ダウンロード。CPU / 4 threadsで端末内生成し、
  クラウド/ローカルを設定で切り替えられる。プロンプトを「展開型・6文構成・90〜130語」に
  改善し、10ケースで **平均89語、生成平均15.6秒、拒否0/10、暫定総合4.37/5**。
  アプリPSSは最大1.95GB、終了後219MBまで解放。10回連続で発熱・バッテリー変化なし。
- 2026-10-26: **M6 プロンプトv7 再調整（0.6.6-m6）**。短文3件を77・79・71語から
  87・99・92語に改善し、10ケースで **平均101語、80〜150語10/10、拒否0/10**。
  `in-color hair`と`smile mixed with ...`を修正。生成時間は平均30.3秒に増加。
  詳細は [`research/M6_PROMPT_V7.md`](research/M6_PROMPT_V7.md)。
- 2026-10-26: **M7 UI刷新・ローカルv9（0.7.1-m7）**。紙面調のライトUIに刷新し、
  タグ編集をタグモードだけに限定。OpenRouterに「接続テスト」とキー再発行リンクを追加し、
  401の説明を改善した。ローカルLLMは **180〜260語・8〜12文** に拡張し、
  実機1ケースで **198語・生成40.5秒**、服装色の追加なしを確認。
  詳細は [`research/M7_REPORT.md`](research/M7_REPORT.md)。
- 2026-10-27: **M7 クラウド上限撤廃（0.7.3-m7）**。有効なOpenRouterキーで接続OK
  （無料枠 / 使用量$0.00）。`openrouter/free` で **224語・44.3秒** と **183語・191.2秒** を生成し、
  150語上限がないことを確認。`max_tokens` とクラウドの語数上限を撤廃した。
  3回目の連続生成は無料枠のレート制限に当たり、0.7.3の忠実さ改善は未検証。
  詳細は [`research/M7_CLOUD_VERIFY.md`](research/M7_CLOUD_VERIFY.md)。
- 2026-10-27: **M7 ローカルv9 10ケース評価（0.7.3-m7）**。平均 **195.5語**、
  **180〜260語 9/10**、260語超 0/10、拒否 **0/10**、日本語混入 0/10、
  平均生成 **35.6秒**（30.5〜39.1秒）。複雑な3分割・縦分割・成人向けも保持。
  課題は157語の成人向け短文1件、服色の捏造2件、オフラインタグの誤り。
  詳細は [`research/M7_LOCAL_V9_EVAL.md`](research/M7_LOCAL_V9_EVAL.md)。
- 2026-10-27: **M7 ローカルv9 20回連続生成の安定性（0.7.3-m7）**。
  PSSはモデルロード後 2103.6MB → 1972.5MB（範囲1937.5〜2143.5MB）で単調増加なし。
  RSS/ SWAPも減少。平均生成 **43.1秒**（32.9〜63.2秒）、
  180〜260語 **15/20**、クラッシュ・OOM・拒否 0/20。
  最高温度 BATTERY 36.9°C / SKIN 39.7°C / CPU 51.5°C。
  詳細は [`research/M7_STABILITY.md`](research/M7_STABILITY.md)。
- 2026-10-27: **PixAI自然文の人手評価（8ケース）**。総合 **3.63/5** で暫定合格、
  意図4.38、自然さ3.63、構造4.50、**忠実さ2.88（未達）**。8/8件が180〜260語。
  最優先課題は自然文とタグの矛盾（25の `cleavage`、27の `solo,1girl`）と、
  未指定の身体的特徴・服色の追加。詳細は [`research/PIXAI_EVAL.md`](research/PIXAI_EVAL.md)。
- 2026-10-27: **M7 ローカルv13 忠実さ・タグ整合性の再評価（0.7.4-m7）**。
  人数・未指定服装・服色・メタ説明を決定論的に補正し、辞書アセットの更新を
  SHA-256比較に修正。同じ8ケースで **平均230.4語、180〜260語 8/8、
  拒否0/8、日本語混入0/8、平均38.8秒**。27は `2girls, multiple_girls,
  white_hair, bob_cut`、25は `cleavage` なし、29は `multiple_girls` なし。
  詳細は [`research/M7_LOCAL_V13_EVAL.md`](research/M7_LOCAL_V13_EVAL.md)。
- 2026-10-28: **v13の人手再採点**。総合 **4.125/5**、意図4.75、自然さ4.25、構造5.00、
  **忠実さ3.625（v9の2.88から+0.745）**、タグ3.875。ただし未指定ヘアの重大2件
  （ID24・ID26）とタグ平均3.875で、正式基準は **不合格**。
- 2026-10-28: **v13.1（0.7.5-m7）**。未指定ヘアの除去、未指定カメラタグの除去、
  `long_hair` / インカラー / `tray` / `juice` / `omurice` / `split_screen` のタグ補完。
  8ケースは **平均219.4語、180〜260語 8/8**、未指定ヘアの重大0件の見込み。
- APK: `/storage/emulated/0/Download/Prompter/Prompter-0.7.5-m7-debug.apk`
  （SHA-256 `e1ba871511b8ac6f4363b3cdbd3d73a8f10361e36ab0190c22dd9332fd20d0fd`、
  約75MB、versionCode 20）。
- 2026-10-28: **v13.1の再採点（独立AI）**。忠実さ **4.00**、総合 **4.25**、タグ **4.125**、
  重大 **0件** で **合格（7/7条件）**。v13.1をM7ローカル標準として採用。
- 次の作業: **M8 版権キャラクターモード**。キャラクター名の専用入力欄、`character_(series)`
  タグの固定、自然文での改名・弱化の防止、曖昧名の候補表示を設計する。
  設計案は [`research/M8_CHARACTER_MODE.md`](research/M8_CHARACTER_MODE.md)。
  方法は未定のため、まず方針を確認する。GPU版はその後。
  機内モードはユーザー指定により実施しない。クラウドは代替なので低優先。

## 要約

- 主対象は **PixAI DiT（Tsubaki.3 / Tsubaki.2）**。DiT はタグ列より **英語の自然文** が向く。
- 既定出力は `Subject + Action + Scene + Mood + Style` の英文。タグモードも併設。
- M1 のオフライン辞書は上位5,000タグ + 補完3タグ（5,006タグ）+ 別名14,618件（SQLite 2.29MB）。
  将来は14万タグ規模へ拡張し、ML Kit 翻訳と組み合わせる。
- M2 で **Compose UI + Kotlin トライ + オフライン自然文合成** を実装済み。
  日本語入力から PixAI 向け英文（オフライン80〜150語、クラウドは語数上限なし、ローカルLLM180〜260語）とタグ列を同時に生成できる。
- M3 で **タグ編集（重み・並び替え・削除）と履歴・お気に入り** を追加。
  履歴は端末内のSQLiteに保存し、変換結果をすぐ呼び出せる。
- M4 で **OpenRouter AI補完** を追加。設定画面でBYOKのAPIキーを登録し、
  オフライン結果を「整える」ことができる。キーはAndroid Keystoreで暗号化して保存する。
  既定モデルは無料モデル自動ルーター `openrouter/free`。Gemma 4 31Bはレート制限のためプリセットから外した。
- M5 で **ML Kit翻訳フォールバック** を追加。辞書外の日本語を端末内で英訳し、
  英語からタグを補完する。モデルは設定画面から任意ダウンロード（約60MB）。
- M6 で **オンデバイスLLM（Gemma 4 E2B / LiteRT-LM）** を追加。約2.59GBのモデルを
  任意ダウンロードし、CPUで英語PixAIプロンプトを生成する。クラウド/ローカルを設定で切り替え、
  失敗時はオフライン結果を保持する。モデルはAPKに同梱しない。
- M7 で **UIを紙面調に刷新**し、タグ編集をタグモードだけに限定。OpenRouterの
  「接続テスト」とキー再発行リンクを追加。ローカルプロンプトv9は **180〜260語** で、
  10ケース評価は **平均195.5語・9/10が範囲内・拒否0/10・平均35.6秒**。
  20回連続生成はPSSが約2.0GBで安定し、クラッシュ・OOMなし（平均43.1秒）。
  クラウドは **語数上限なし** に変更し、有効キーで224語・183語を確認。
  詳細は [`research/M7_REPORT.md`](research/M7_REPORT.md)、
  [`research/M7_CLOUD_VERIFY.md`](research/M7_CLOUD_VERIFY.md)、
  [`research/M7_LOCAL_V9_EVAL.md`](research/M7_LOCAL_V9_EVAL.md)、
  [`research/M7_STABILITY.md`](research/M7_STABILITY.md)。
- AI 補完は **OpenAI API 互換（既定: OpenRouter）** または **端末内Gemma 4 E2B** を任意 ON。
- クラウドAIを使いたくない人向けの **ML Kit翻訳**、およびネットなしで動く **ローカルLLM** を用意。
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

現在の APK は `0.7.5-m7`（versionCode 20、debug 署名、arm64-v8a）。M1 の辞書、M2 のオフライン変換、
M3 のタグ編集・履歴、M4 のAI補完、M5 のML Kit翻訳フォールバック、M6 のローカルLLM、
M7 のUI刷新・ローカルv13・忠実さ/タグ整合性補正・クラウド語数上限なしを含む。

M5以降はdebug署名鍵をリポジトリ内の `keystore/prompter-debug.jks` に固定した。
そのため、**0.5.0以降**のAPKは `adb install -r` で上書きできる。
0.4.2以前はCIの一時鍵で署名されていたため、0.5.0の初回だけは一度アンインストールが必要。

```bash
# 0.4.2からの初回だけ
adb uninstall com.prompter.app
adb install Prompter-0.5.0-m5-debug.apk

# 0.5.0以降の更新
adb install -r Prompter-0.5.0-m5-debug.apk
```

APKはarm64-v8a専用のため、x86/x86_64のエミュレータにはインストールできない。

### AI補完の使い方

1. アプリ右上の **設定** を開く
2. AIバックエンドを **クラウド** または **ローカル** から選ぶ
3. クラウドの場合、OpenRouterのAPIキーを入力して **接続テスト** を押し、**保存**
4. 日本語を入力して **変換する**
5. 結果の下にある **整える** を押す

ローカルを選ぶと、設定画面からGemma 4 E2B（約2.59GB）を任意ダウンロードできる。
ローカル生成は **180〜260語・8〜12文**、クラウドは **語数上限なし**（目安180語以上、
入力が単純な場合は短くてもよい）、オフラインは **80〜150語** を目安にする。
クラウドの既定モデルは無料モデル自動ルーター `openrouter/free`。設定で
`google/gemini-3.1-flash-lite` などの有料モデルにも変更できる。
**クラウドはローカルLLMが使えないときの代替**で、品質の作り込みは最小限にしている。
無料枠はレート制限があり、連続実行すると
`レート制限に達しました。少し待って再試行してください。 Provider returned error`
が出ることがある。時間を置くか、設定の詳細で特定の無料モデル
（例: `google/gemma-4-31b-it:free`）を指定する。
クラウドは `max_tokens` を送らず、語数上限も設けない。入力にない家具・小道具・色を
足して語数を埋めないようプロンプトで指示する。
旧既定のGemini 3.1 Flash Lite、または旧無料プリセットのGemmaを保存している場合は、
初回起動時に一度だけ `openrouter/free` へ移行する（カスタムモデルは維持）。
APIキーはチャット・リポジトリ・APK・履歴には保存しない。

**401 `User not found.` が出る場合**: 保存済みキーが無効になっている。
設定の **キーを再発行** から <https://openrouter.ai/keys> を開き、新しいキーを入力して
**接続テスト** → **保存** を行う。モデルIDやエンドポイントの問題ではない。

### ML Kit翻訳の使い方

1. アプリ右上の **設定** を開く
2. **オフライン翻訳（ML Kit）** の「翻訳モデルをダウンロード（約60MB）」を押す
3. ダウンロード完了後、日本語を入力して **変換する**

辞書にない日本語はML Kitで英訳され、自然文の補足とDanbooruタグに反映される。
モデルは端末内に保存されるため、ダウンロード後は機内モードでも使える。
Google Play Servicesが使えない端末や、モデル未ダウンロード時は辞書のみで動作する。
タグチップの下には同梱辞書の日本語別名を表示し、別名がないタグはML Kitの英語→日本語モデルで補完する。

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
| 既定出力 | 英語自然文（オフライン80〜150語、クラウドは上限なし、ローカルLLM180〜260語） |
| タグ出力 | PixAI Haruka v2 / SDXL / Pony / A1111 / ComfyUI / NovelAI |
| AI | OpenRouter / OpenAI 互換、任意 ON、BYOK |
| オフライン翻訳 | ML Kit（M5、約60MB、任意ダウンロード） |
| ローカルLLM | Gemma 4 E2B（任意、M6実装済み）、E4B（12GB 端末限定・未実装） |
| 配布 | 自分用 APK（arm64-v8a、安定debug署名） |
| 成人向け | フィルタしない（個人用） |
