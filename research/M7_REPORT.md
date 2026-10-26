# M7 UI刷新・OpenRouter診断・ローカルプロンプトv9

- 日付: 2026-10-26
- APK: `0.7.1-m7`（versionCode 16）
- 対象: `app/src/main/java/com/prompter/app/`
- CI: <https://github.com/shiokunn/prompter/actions/runs/40318320648>

## 1. 目的

M6 の機能を保ったまま、次の4点をまとめて改善した。

1. OpenRouter の保存キーで発生する `401 User not found.` を診断し、設定画面からキーを検証できるようにする。
2. Gemma 4 E2B の出力を約100語で止めず、180〜260語の具体的な描写にする。
3. タグ編集をタグ出力モードだけに表示し、通常画面の説明文を減らす。
4. 画面を明るい紙面調のデザインに作り直す。

## 2. OpenRouter

### 診断結果

- 保存済みキーで実際に `POST /chat/completions` を呼ぶと、`401` と `User not found.` が返る。
- モデルID（`openrouter/free`、`google/gemini-3.1-flash-lite` など）とエンドポイントは有効。
- したがって、失敗の原因は保存キーそのもの。ユーザーが <https://openrouter.ai/keys> で新しいキーを発行する必要がある。

### 実装

- `OpenRouterClient.testKey()` を追加。`GET /key` を呼び、ラベル・使用量・上限・無料枠を表示する。トークンは生成しない。
- 設定画面に「接続テスト」と「キーを再発行」を追加。
- `401` のメッセージを「APIキーが無効です。OpenRouterで新しいキーを発行してください。」に変更。
- `OpenRouterException.isUnauthorized` を追加。
- 実機で保存済みの無効キーを使い、接続テストが 401 メッセージを表示することを確認。

### 残り

- 有効なキーでのクラウド生成確認。キーがないため未実施。
- クラウドの語数目標は従来どおり 80〜150 語。ローカルのみ 180〜260 語。

## 3. ローカルLLMプロンプト

### v8（2026-10-26）

- 目標を 180〜260 語、8〜12文、目安220語に変更。
- 文の長さは「大半15〜25語」の目安にし、v7の「16〜19語固定」を廃止。
- 描写順を「人物と動作 → 顔・髪・目・肌 → 服・小物 → ポーズ・手・視線 → 背景 → 光・影・雰囲気 → カメラ・構図 → 画風」に拡張。
- 出力トークン上限を 768 から 1,024 に増加。
- キャッシュキーを `local:gemma-4-E2B-v8` に更新。

実機1ケース（白髪・赤目・メイド服・オムライス・三分割背景）:

- 202語、生成34.2秒、読み込み0.9秒。
- 白髪・赤目・メイド服・オムライス・三分割を保持。
- 具体的な料理工程、光、構図、画風まで描写。
- ただし入力にない `black frilled collar` と `white apron` を追加した。

### v9（最終）

- 「髪・目・服の色は入力にない色を名付けない。布のひだ・トリム・背景小物は追加してよい」と明確化。
- キャッシュキーを `local:gemma-4-E2B-v9` に更新。
- 実機1ケース（同じ入力、開始時のタグは白髪・赤目・foodの3件。AIが `cooking` / `kitchen` を追加し、最終5件）:
  - **198語、生成40.5秒**。
  - 白髪・赤目・メイド服・オムライス・三分割を保持。
  - 服装の色（黒い襟、白いエプロン）を追加しなくなった。`her apron` のように色を付けない表現になった。
  - `golden brown` は食品の色、`rich color saturation` は画風の表現で、服の色ではない。
  - 汎用的な「野菜を切る」「オムレツを返す」「キッチン」「ミディアムクローズアップ」を追加。入力にない固有名詞・版権・固有物体は追加していない。
- 1ケースのみの確認であり、10ケースの再評価は未実施。

出力全文:

> A beautiful maiden with pure white hair and striking red eyes is diligently preparing an omurice while wearing a crisp maid uniform. Her expression is one of intense focus and earnest effort as she meticulously handles the ingredients for the meal. The scene is divided into three distinct panels, centered around the middle, showcasing different stages of the omurice preparation in each section. In one panel, she is carefully slicing vegetables with precise, focused movements, demonstrating the first step of the cooking process. Another view captures her skillfully flipping the omelet, ensuring the texture is perfectly smooth and golden brown. The third segment illustrates her delicately arranging the rice and topping the dish with vibrant ingredients, showing her concentration on the final presentation. Soft, warm lighting illuminates the kitchen, casting gentle shadows that emphasize the textures of the food and her apron. The background is subtly blurred, focusing all attention on the detailed culinary actions taking place in each frame. The camera employs a medium close-up shot, capturing the intricate details of her hands and the food preparation. The art style should be high-quality anime rendering, featuring sharp lines and rich color saturation for a detailed, appetizing finish.
>
> タグ: `white_hair, red_eyes, food, cooking, kitchen`（辞書外タグは除去）

### 注意

- 生成時間は約40秒。v7の平均30.3秒より長い。詳細さを優先した結果で、端末の発熱・メモリは未再測定。
- 180語未満の場合はアプリが「目標180〜260語」と通知するが、生成結果は破棄しない。
- ローカル出力の語数検証は 180〜260 語、クラウドは 80〜150 語。

## 4. UI

- 明るい紙面調（`#F4F0E8`）、カードは生成り、線は細いヘアライン、アクセントはテラコッタ。
- 見出しはセリフ体、本文はサンセリフ体。強制的にライトテーマ。
- ステータスバーとナビゲーションバーを紙色に合わせ、暗いアイコンを指定。
- メイン画面:
  - 「情景を、ことばに。」のヒーロー。
  - 入力、形式（両方／自然文／タグ）、変換を縦に整理。
  - 結果はカードではなく紙面に直接表示。自然文はコピーと語数、タグはチップとコピー。
  - タグ編集は **タグモードのときだけ** 表示。
  - AIカードはモデル名・状態・「整える」だけに圧縮。長い説明文を削除。
  - 辞書外は最大3行に省略。
- 設定画面:
  - クラウド／ローカルのセグメント。
  - APIキー、接続テスト、キー再発行、削除。
  - モデルプリセット、モデルID・Base URLは「詳細」に折りたたみ。
  - ローカルモデルの状態・進捗・検証・削除。
  - 翻訳モデルの状態。
- 履歴画面:
  - カードをやめ、ヘアラインで区切った行。
  - 行タップで読み込み、☆でお気に入り、×で削除。
  - すべて／お気に入りのセグメント。
- タグ編集:
  - タグ名・意味・重み・↑↓×の1行表示。
  - 重みタップで 1.0 → 1.2 → 1.4。
  - 長い説明文を削除。

## 5. テストとビルド

- Python M1テスト: 15件成功。
- Kotlin単体テスト: 成功（CI）。
- `OpenRouterClientTest` に `/key` の成功・401の2件を追加。
- `AiPromptValidatorTest` にローカル180〜260語の検証を追加。
- `LocalLlmPromptTest` をv9の文言・キャッシュキーに更新。
- CI run `40318320648` 成功。
- APK SHA-256: `594273793abbde4db52df82d21f107856b9af18b6b9448538789700378727828`
- APK: `/storage/emulated/0/Download/Prompter/Prompter-0.7.1-m7-debug.apk`
- 実機 `127.0.0.1:5555` にインストール済み。モデルは既存の2,588,147,712 bytesを再利用。

## 6. 残る確認

1. 有効なOpenRouterキーでクラウド生成を確認する。
2. v9で10ケースを再評価し、平均語数・180〜260語の達成率・拒否数・生成時間を測る。
3. v9の服装色ルールを複数ケースで確認する。
4. 20〜30回連続生成でPSSと発熱を再確認する。
5. 機内モードでv9を再確認する。
6. 人手で10〜20件の品質評価を行う。
