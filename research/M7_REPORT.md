# M7 UI刷新・OpenRouter診断・ローカルプロンプトv9

- 日付: 2026-10-26
- APK: `0.7.1-m7`（versionCode 16）
- 追記: 0.7.3-m7 のクラウド上限撤廃と有効キー検証は §7 と [`research/M7_CLOUD_VERIFY.md`](M7_CLOUD_VERIFY.md) を参照。
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

- 有効なキーでのクラウド生成確認は [`research/M7_CLOUD_VERIFY.md`](M7_CLOUD_VERIFY.md) に記録。
- クラウドは語数上限を撤廃し、入力への忠実さを優先する詳細プロンプトに変更。ローカルのみ 180〜260 語の範囲を検証する。

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
- ローカル出力の語数検証は 180〜260 語。クラウドは語数上限なし・語数警告なし。

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

1. 0.7.3のクラウド忠実さは、レート制限の解除後に余力があれば軽く確認する（クラウドはローカルLLMの代替なので低優先）。
2. 人手評価8ケースの結果（総合3.63/5、忠実さ2.88/5）を受けて、忠実さとタグ整合性を修正する。
3. 180語未満・服色・タグ誤りを、忠実さ修正後に再確認する。
4. 機内モードはユーザー指定により実施しない。

## 7. 追記: クラウド上限撤廃（2026-10-27、`0.7.3-m7`）

- 有効なOpenRouterキーで接続テスト成功（無料枠 / 使用量$0.00 / 上限$0.00）。
- `OpenRouterClient` のリクエストから `max_tokens` を削除。
- クラウド用プロンプトの80〜150語制限を削除し、入力への忠実さを優先する詳細プロンプトに変更。
- クラウドは語数上限なし・語数警告なし。ローカルは180〜260語、オフラインは80〜150語のまま。
- `AiCache.CACHE_VERSION` を3に更新し、旧プロンプトのキャッシュを無効化。
- `openrouter/free` で **224語・44.3秒** と **183語・191.2秒** を生成。150語上限がないことを確認。
- 224語の白髪メイドcaseは白髪・赤目・メイド服・オムライス・3分割を保持し、服装色の捏造なし。ただしオフライン由来の `2girls` / `multiple_girls` 誤タグを引き継いだ。
- 183語の単純caseは入力にない白い部屋・本・ランプ・ラグを追加したため、0.7.3で「入力にない家具・小道具・設定要素を追加しない」ルールを追加。
- 3回目の連続生成は無料枠のレート制限（`レート制限に達しました。少し待って再試行してください。 Provider returned error`）に当たり、0.7.3の忠実さ改善は未検証。特定の無料モデル `google/gemma-4-31b-it:free` でも同じエラー。
- 詳細: [`research/M7_CLOUD_VERIFY.md`](M7_CLOUD_VERIFY.md)

## 8. 追記: ローカルv9 10ケース評価（2026-10-27、`0.7.3-m7`）

- 結果: 平均 **195.5語**、180〜260語 **9/10**、260語超 0/10、拒否 **0/10**、日本語混入 0/10。
- 平均生成時間 **35.6秒**（30.5〜39.1秒）。v7の平均30.3秒より約5.3秒長い。
- 複雑な3分割（25/29）、縦分割・二人（27）、成人向け（28/30）を保持。
- 髪色・目色の捏造は0/10。入力にない服色は2/10（27の学生服、29のメイド服）残った。
- 28は **157語** で180語未満。内容的には完結している。
- オフラインタグの誤り: 21の `aiming`、25の `cleavage` と `hiding` の矛盾、27の二人なのに `solo, 1girl` と `half_updo`、29の単数入力に `multiple_girls`。ローカルLLMではなく辞書・タグ抽出側の問題。
- 詳細: [`research/M7_LOCAL_V9_EVAL.md`](M7_LOCAL_V9_EVAL.md)

## 9. 追記: ローカルv9 20回連続生成の安定性（2026-10-27、`0.7.3-m7`）

- 同じシーンを20回連続生成（AIキャッシュ回避のため入力末尾に `。` を追加）。
- 20/20完走。クラッシュ・OOM・生成拒否なし。
- PSSはモデルロード後 **2103.6MB → 1972.5MB**（範囲1937.5〜2143.5MB）。単調増加なし。
- RSSは 1959.3 → 1845.4MB、SWAPは 268.0 → 247.9MB。
- 平均生成 **43.1秒**（32.9〜63.2秒）。最初の5回平均38.3秒、最後の5回平均44.3秒。
- 語数は165〜236語、180〜260語 **15/20**、180語未満5/20、260語超0/20。
- 最高温度 BATTERY **36.9°C** / SKIN **39.7°C** / CPU **51.5°C**。危険域ではない。
- バッテリーは52%→48%。
- 詳細: [`research/M7_STABILITY.md`](M7_STABILITY.md)

## 10. 追記: ローカルv13 忠実さ・タグ整合性（2026-10-27、`0.7.4-m7`）

- v9の人手評価で未達だった忠実さ2.88/5を受け、人数・未指定服装・服色・メタ説明を決定論的に補正。
- 人数: 人物特徴だけの入力は1人、分割構図は2人、3分割の同一人物は1人。`solo/1girl` と `2girls/multiple_girls` の矛盾を除去。
- 服装: 入力に服装がない場合は服装タグと服装だけの文を削除。色は入力に明示された場合だけ保持。
- メタ説明: "not specified" の説明文を自然な文に置換。
- タグ復元: 自然文が保持している `white_hair` などの属性をタグに戻す。`ボブカット` から `bob_cut` を補完。
- 辞書アセット: サイズが同じでも内容が変われば端末内DBを更新するようSHA-256比較に変更。
- v13プロンプト: 11〜12文、210〜240語、各文18語以上。単純入力でも11文まで既存要素を深掘りする。
- 同じ8ケース: 平均 **230.4語**、**180〜260語 8/8**、拒否0/8、日本語混入0/8、平均 **38.8秒**。
- 27は `2girls,multiple_girls,white_hair,bob_cut`、`solo,1girl,half_updo` なし。25は `cleavage` なし。29は `multiple_girls` なし。
- 残課題: 25の未指定カメラタグ、29の具体的な料理工程、24の単数代名詞、27の251語。v13の人手再採点は未実施。
- 8ケース終了後PSS 2,317MB。OOM・クラッシュなし。一括最後の自動操作タイムアウト1回は再起動後の再実行で成功。
- 詳細: [`research/M7_LOCAL_V13_EVAL.md`](M7_LOCAL_V13_EVAL.md)

## 11. 追記: v13人手再採点とv13.1（2026-10-28、`0.7.5-m7`）

- v13の人手再採点: 総合 **4.125/5**、意図4.75、自然さ4.25、構造5.00、忠実さ **3.625**、タグ **3.875**。
- 忠実さは v9 の 2.88 から **+0.745**。数値目標（3.5）は達成したが、正式基準は **不合格**。
- 不合格の理由: ID24・ID26 の未指定ヘア（重大2件）、タグ平均3.875（4.0未満）。
- v13.1で修正: 未指定ヘアの除去、未指定カメラタグの除去、`greyscale` の除去、
  `long_hair` / インカラー / `frills` / `looking_back` / `tray` / `juice` / `omurice` / `split_screen` の補完。
- 辞書は5,006タグ / 別名14,618件。`omurice` / `juice` / `split_screen` を補完タグとして追加。
- v13.1の8ケース: 平均 **219.4語**、180〜260語 **8/8**（195〜235語）。未指定ヘアの重大0件。
- v13.1の再採点（独立AI）: 忠実さ **4.00**、総合 **4.25**、タグ **4.125**、重大 **0件** で **合格**。
  v13.1をM7ローカル標準として採用。人手による最終判定は未実施。
- 採点キット: [`research/M7_V13_RERATE.md`](M7_V13_RERATE.md) §11・付録A。
