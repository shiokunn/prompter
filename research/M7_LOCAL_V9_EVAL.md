# M7 ローカルLLM v9 10ケース評価

> 追記: 忠実さ・タグ整合性を修正したv13の再評価は
> [`research/M7_LOCAL_V13_EVAL.md`](M7_LOCAL_V13_EVAL.md) を参照。

- 日付: 2026-10-27
- APK: `0.7.3-m7`（versionCode 18）
- モデル: Gemma 4 E2B IT / LiteRT-LM 0.18.1 / CPU 4 threads
- 生成設定: `maxNumTokens=4096`, `maxOutputToken=1024`, topK 10, topP 0.95, temperature 0.3, seed 42, thinking off
- 評価方法: M6の10ケースを履歴から読み込み、オフライン変換→ローカルLLMで整える。出力とタグは履歴DBから取得。
- 注意: 文章の良し悪しはAIによる暫定確認。最終評価は人手で行う。

## 1. 結果サマリ

| 指標 | 結果 |
|---|---|
| ケース数 | 10 |
| 平均語数 | **195.5語** |
| 180〜260語に入ったケース | **9/10** |
| 180語未満 | 1/10（157語） |
| 260語超 | 0/10 |
| 生成拒否 | **0/10**（成人向け2件を含む） |
| 日本語混入 | 0/10 |
| 平均生成時間 | **35.6秒**（30.5〜39.1秒） |

v7（101.0語、平均30.3秒）と比べ、語数は約1.9倍、生成時間は約5.3秒増。

## 2. ケース別結果

| # | ID | 入力の要点 | 語数 | 生成 | 180〜260 | 主な所見 |
|---|---|---|---:|---:|---|---|
| 20 | 20 | 長い黒髪、青いドレス、椅子 | 185 | 36.9s | OK | 黒髪・青ドレス・椅子を保持。部屋・窓・レースは汎用補完。185語。 |
| 21 | 21 | 黒ボブ、水色インカラー、黒フリル＋白レース、上半身、左側、上目遣い | 192 | 30.5s | OK | 黒髪・水色インナー・黒目・黒/白レースを保持。`normal expression mixed with a bright smile` が不自然。タグ `aiming` は上目遣いの誤変換。 |
| 22 | 22 | 明るい黄色の髪、紫の目、ロングヘアー | 220 | 35.7s | OK | 黄色髪・紫目・ロングを保持。服装は色名なし。220語。 |
| 24 | 24 | 二人の少女、ベッド、軽い微笑み | 189 | 33.0s | OK | 二人・ベッド・微笑みを保持。`eyes are closed or cast in ...` が曖昧。白いシーツは背景補完。 |
| 25 | 25 | 白髪赤目、メイド、オムライス、3分割、胸元を隠す、中央提示 | 207 | 36.0s | OK | 白髪・赤目・メイド・オムライス・3分割・胸元を隠す・中央提示を保持。服色の捏造なし。タグは `cleavage` と `hiding` が矛盾。 |
| 26 | 26 | 白いドレス、野原、立ち、こちらを見る | 194 | 39.1s | OK | 白ドレス・野原・立ち・視線を保持。髪色は指定なしで追加なし。 |
| 27 | 27 | 縦半分、壁、右=学生服、左=メイド、白ボブ、左手トレー | 204 | 36.6s | OK | 縦分割・壁・両者の白ボブ・学生服/メイド・左手トレーを保持。**学生服の白シャツ/黒ズボンは入力にない服色**。タグは二人なのに `solo, 1girl`、`half_updo` も誤り。 |
| 28 | 28 | 成人向け、乳首を擦る | 157 | 32.9s | NG | 成人向け。拒否なし、内容保持。**157語で180語未満**。`focusing intensely on her chest` はやや説明的。 |
| 29 | 29 | 白髪赤目、メイド、オムライス、3分割、料理工程 | 217 | 39.1s | OK | 白髪・赤目・メイド・オムライス・3分割・料理工程を保持。**白い布・黒いエプロンは入力にない服色**。タグ `multiple_girls` は単数入力に矛盾。 |
| 30 | 30 | 成人向け短文「エッチなお嬢さん」 | 190 | 36.0s | OK | 成人向け短文。拒否なし、性的表現を保持。髪/服の色名なし。190語。 |

## 3. 出力全文

### 20. ID 20（185語 / 36.9秒）

入力: 長い黒髪、青いドレス、椅子

> A solitary young girl with long black hair is seated gracefully on a wooden chair. Her features are delicate, framed by the cascade of her dark, flowing locks that reach down past her shoulders. She possesses large, expressive eyes that gaze softly toward the viewer with a serene and contemplative expression. She is wearing an elegant, flowing blue dress that drapes softly around her body, emphasizing the fabric's smooth texture. The dress features delicate lace trim along the neckline and sleeves, catching the ambient light with a subtle sheen. Her posture is relaxed yet poised as she sits upright in the chair, her hands resting gently in her lap. The setting is a dimly lit, richly paneled room, suggesting an antique or quiet study environment. Soft, warm light streams in from a nearby window, casting gentle highlights on the blue fabric and creating soft shadows across the scene. The composition is a medium shot, focusing on the girl and her attire against the darker background elements. The overall art style should be high-quality anime rendering, featuring intricate details in the hair strands and fabric folds.

タグ: `1girl, solo, long_hair, black_hair, blue_dress, sitting, on_chair, chair, dress, lace_trim`

### 21. ID 21（192語 / 30.5秒）

入力: 黒ボブ、水色インカラー、黒フリル＋白レース、上半身、左側、上目遣い

> A beautiful young woman with black hair styled in a bob cut is featured in this upper-body portrait. Her hair has a striking light blue inner-color highlight that catches the light beautifully. She possesses black eyes and wears a gentle, normal expression mixed with a bright smile on her face. Her skin appears smooth and porcelain-like under the soft illumination. She is dressed in an intricate outfit consisting of black lace with prominent ruffles layered over a delicate white lace overlay. The composition is a close-up shot focusing entirely on her upper body details. Her posture involves a slight turn of the head towards the viewer, looking up with a captivating, innocent gaze. The lighting is soft and diffused, creating gentle highlights on the lace textures and subtle shadows that add depth to the scene. The background is intentionally simple, perhaps a softly blurred, muted tone to keep the focus entirely on the subject. The camera angle is a side view, emphasizing the intricate layering of the fabrics and the delicate features of the girl. The overall art style should be high-detail anime rendering with meticulous attention to fabric texture and shading.

タグ: `1girl, solo, upper_body, smile, black_hair, bob_cut, black_eyes, aiming, lace, from_above, from_side, looking_up`

### 22. ID 22（220語 / 35.7秒）

入力: 明るい黄色の髪、紫の目、ロングヘアー

> A beautiful young woman with long, flowing blonde hair and striking purple eyes is the central focus of this portrait. Her hair cascades down her shoulders in thick, glossy waves, catching the light with a warm, golden sheen. Her eyes possess a deep, captivating purple hue, reflecting a thoughtful and serene expression on her face. She has smooth, porcelain-like skin that glows softly under the ambient illumination of the scene. She is wearing an outfit that suggests a casual yet elegant style, perhaps a simple, flowing garment that drapes softly around her form. Her long hair is meticulously styled, with subtle highlights that emphasize the texture and volume of the strands. Her pose is relaxed yet engaging, with one hand gently touching the curve of her long hair while her gaze is directed softly toward the viewer. The setting is a softly lit, airy space, perhaps near a window where gentle daylight streams in, creating soft highlights and subtle shadows across her features. The background is intentionally blurred, consisting of muted, warm tones that keep the focus entirely on the subject. Cinematic lighting emphasizes the texture of her hair and the depth of her purple eyes, creating a warm and inviting atmosphere. The overall rendering style is high-quality anime, featuring intricate details in the hair strands and soft, painterly shading.

タグ: `blonde_hair, purple_eyes, long_hair, portrait`

### 24. ID 24（189語 / 33.0秒）

入力: 二人の少女、ベッド、軽い微笑み

> Two young girls are seated comfortably on a large, plush bed, sharing a moment of gentle happiness. Both girls possess soft, delicate features, with their hair styled in a way that catches the soft light beautifully. Their eyes are closed or cast in a soft, contented gaze, revealing a faint smile on their faces. They are dressed in simple, soft clothing that emphasizes a relaxed and intimate atmosphere. One girl has her hair cascading over the white sheets, while the other has her hair neatly arranged around her shoulders. Their posture is relaxed yet engaging, suggesting a close and comfortable relationship between them. One girl gently rests her head near the other, conveying a sense of quiet affection in the scene. The setting is a cozy bedroom, with soft shadows playing across the rumpled bedding textures. Warm, diffused lighting streams in from a nearby window, casting a warm glow and highlighting the subtle sheen on their skin. The composition is a medium close-up shot, focusing intimately on their faces and the shared, light smile. The overall art style is high-quality anime rendering, featuring soft shading and detailed fabric textures.

タグ: `2girls, bed, multiple_girls, smile, on_bed, sitting, light_smile`

### 25. ID 25（207語 / 36.0秒）

入力: 白髪赤目、メイド、オムライス、3分割、胸元を隠す、中央提示

> A beautiful goddess figure, resembling a victory goddess, with striking red eyes and long white hair, is meticulously preparing an omurice while wearing a detailed maid uniform. Her expression is a bright, genuine smile directed towards the viewer, showcasing her dedication to the cooking process. She is dressed in a maid outfit that is intentionally modest, with the top carefully structured to fully conceal her chest area. Her long white hair flows gracefully around her shoulders, catching the soft ambient light of the kitchen setting. The scene is dramatically divided into three distinct panels, centered around the middle section, with dividing lines extending from the bottom left and the top center to the center. In each panel, the goddess is shown engaged in a different stage of the omurice preparation, illustrating the entire cooking procedure sequentially. Finally, in the central view, she presents the perfectly formed omurice directly towards the viewer with an inviting and cheerful smile. The lighting is bright and warm, casting gentle highlights on the textures of the food and the fabric of her uniform. The composition uses a dynamic three-part split, emphasizing the progression of the meal preparation. Rendered in a high-quality anime style with intricate details on the food and clothing textures.

タグ: `1girl, maid, white_hair, red_eyes, full_body, food, smile, cleavage, hiding, split, from_side, day, can, breasts, from_below`

### 26. ID 26（194語 / 39.1秒）

入力: 白いドレス、野原、立ち、こちらを見る

> A solitary young girl stands in a wide, open field, gazing directly out at the viewer with an intense expression. She is wearing a flowing white dress that catches the soft sunlight, its fabric appearing delicate and ethereal against the green landscape. Her long, straight hair cascades down her back, shimmering slightly as it moves in the gentle breeze across the open expanse. Her eyes are wide and focused, conveying a sense of quiet contemplation mixed with a subtle yearning. The girl's skin appears porcelain-like, illuminated beautifully by the bright daylight filtering through the atmosphere. She stands with her hands loosely clasped in front of her, her posture relaxed yet poised, inviting the viewer into her silent world. The setting is a vast, verdant field, dotted with tall, slightly swaying grasses that create soft texture in the foreground. Warm, golden sunlight bathes the scene, casting long, soft shadows that emphasize the depth of the open space. The composition uses a medium shot, placing the girl centrally against the expansive, slightly hazy background of the meadow. The art style should be high-detail anime rendering, featuring soft shading and intricate fabric folds for a realistic texture.

タグ: `1girl, solo, standing, looking_at_viewer, white_dress, dress, field`

### 27. ID 27（204語 / 36.6秒）

入力: 縦半分、壁、右=学生服、左=メイド、白ボブ、左手トレー

> A striking composition divides the frame vertically down the center, with the dividing line itself rendered as a textured wall structure. On the right side, a young girl with a white bob cut leans against this wall in a relaxed, casual posture. She is wearing a crisp school uniform, featuring a white blouse and dark trousers, with subtle folds visible in the fabric. Her expression is neutral, and her gaze is directed slightly off-camera, conveying a sense of quiet contemplation. Conversely, the left half features another girl dressed in a detailed maid uniform. This girl also has a white bob cut and is holding a tray with a glass of juice balanced carefully on it with her left hand. Her posture is attentive, suggesting a formal yet gentle demeanor. Soft, diffused lighting illuminates the scene, casting gentle shadows that emphasize the textures of the clothing and the wall. The background is simple, perhaps a muted interior space, allowing the focus to remain entirely on the two figures and their contrasting outfits. The camera angle is a medium shot, perfectly bisecting the composition to highlight the visual division. The overall art style should be high-quality anime rendering, emphasizing clean line work and detailed fabric textures.

タグ: `solo, 1girl, school_uniform, bob_cut, maid, holding, half_updo`

### 28. ID 28（157語 / 32.9秒）

入力: 成人向け、乳首を擦る

> A single anime character is depicted in an intimate and suggestive pose, focusing intensely on her chest. Her expression is one of intense, blissful pleasure, characterized by a soft, yielding smile. Her skin appears smooth and flushed, catching the warm, soft light of the scene. She is wearing minimal clothing, emphasizing the focus on her delicate features and curves. Her long, flowing hair cascades around her shoulders, with individual strands catching the light subtly. Her hands are positioned delicately, gently tracing the contours of her nipples with slow, deliberate movements. The setting is dimly lit, creating deep, sensual shadows that accentuate the curves of her body. Soft, warm ambient lighting highlights the texture of her skin and the sheen of her exposed areas. The camera angle is a close-up shot, framing her upper body to capture the detailed interaction. The art style should be highly detailed anime rendering, emphasizing soft shading and realistic fabric folds where applicable.

タグ: `close-up, long_hair`

### 29. ID 29（217語 / 39.1秒）

入力: 白髪赤目、メイド、オムライス、3分割、料理工程

> A beautiful figure with pure white hair and striking red eyes, resembling a Cinderella-like maiden, is dressed in a detailed maid uniform while diligently preparing an omurice. Her expression is one of intense concentration as she focuses on the cooking process. The scene is meticulously divided into three distinct panels, centered around the middle, showcasing different stages of the omurice preparation in each section. In the first panel, she is carefully slicing vegetables for the omelet, showing her earnest effort in the culinary task. Next, the second panel captures her skillfully arranging the rice and topping the dish with savory ingredients. The third panel illustrates the final assembly, where she delicately garnishes the finished omurice with precise movements. Her maid outfit features crisp white fabric with delicate lace trim and a neatly tied black apron, emphasizing the contrast with her pale hair. Her hands are actively engaged in various cooking tasks, demonstrating a sense of dedication and meticulous care for the food. Soft, warm lighting illuminates the kitchen setting, casting gentle shadows that highlight the textures of the ingredients and the fabric folds. The composition uses a central focus, with the three distinct cooking scenes arranged symmetrically across the frame. The overall art style should be high-quality anime rendering, featuring sharp lines and rich, appetizing food details.

タグ: `white_hair, red_eyes, maid, food, cooking, multiple_girls`

### 30. ID 30（190語 / 36.0秒）

入力: 成人向け短文「エッチなお嬢さん」

> A single, alluring young woman is the central focus of this intimate scene. Her expression is overtly suggestive, conveying a bold and provocative demeanor to the viewer. She possesses long, flowing hair that cascades down her shoulders, with delicate strands catching the soft light. Her eyes hold a knowing, sultry gaze that draws the viewer into her intense presence. She is dressed in revealing attire that emphasizes her figure, featuring sheer fabrics and delicate lace details. Intricate accessories adorn her body, adding to the overall sensual and decadent aesthetic of the image. Her pose is deliberately provocative, leaning slightly forward with a confident and alluring body language. Her hands are positioned gracefully, perhaps lightly touching her own décolletage or resting near her hips. The setting is dimly lit, perhaps a luxurious, private room with rich, dark textures dominating the background. Soft, warm lighting casts deep shadows, highlighting the curves of her body and creating a highly atmospheric mood. The camera angle is a medium close-up, focusing sharply on her face and upper body. The art style should be highly detailed anime rendering, emphasizing smooth skin textures and intricate fabric folds.

タグ: `solo, 1girl`

## 4. 所見

### 良かった点

- 10/10で入力の主要属性（髪色・目色・服の種類・人数・構図・ポーズ）を保持した。
- 複雑な3分割（25/29）と縦分割・二人構図（27）を破綻なく保持した。
- 成人向け2件（28/30）で拒否・検閲・日本語混入なし。
- 平均195.5語、9/10が180〜260語。v9の詳細化は明確に効いている。
- 平均35.6秒で、v9の198語・40.5秒と同水準。

### 課題

- **157語**: 28（成人向け短文）だけ180語未満。内容的には完結しており、無理に水増しするより許容する判断もある。
- **服色の捏造**: 27の「白いブラウス・黒いズボン」、29の「白い布・黒いエプロン」は入力にない服色。v9の「服の色を名付けない」ルールに反する。
- **不自然な英語**: 21の `normal expression mixed with a bright smile`、24の `eyes are closed or cast in a soft, contented gaze`。
- **タグの誤り（オフライン由来）**: 21の `aiming`、25の `cleavage` と `hiding` の矛盾、27の二人なのに `solo, 1girl` と `half_updo`、29の単数入力に `multiple_girls`。同じ入力のオフライン変換行の時点でこれらのタグが付いており、ローカルLLMのTAGS出力がそれを引き継いでいる。原因は辞書・マッチャ側（例: `半分` → `half_updo`、`それぞれ` → `2girls`/`multiple_girls` の推論）。
- 20は部屋・窓・レースを追加。汎用補完の範囲だが、入力が単純なときの補完量はやや多い。

## 5. 判定

- v9は詳細化・複雑構図・成人向けの保持で v7 を上回る。**10ケース評価では採用継続**。
- 180〜260語は9/10。28の157語は許容範囲とするか、短文向けの下限を再検討する。
- 服色の捏造（2/10）とタグ誤りは残課題。ただしタグはオフライン辞書側の問題で、v9の生成品質とは分けて扱う。
- 次: 人手評価8ケースの結果（総合3.63/5、忠実さ2.88/5）を受けて、忠実さとタグ整合性を修正する。機内モードはユーザー指定により実施しない。

## 6. 関連: 20回連続生成の安定性

同じシーンを20回連続生成した結果は [`research/M7_STABILITY.md`](M7_STABILITY.md) にある。

- PSSはモデルロード後 2103.6MB → 1972.5MB（範囲1937.5〜2143.5MB）で単調増加なし。
- クラッシュ・OOM・拒否 0/20。平均43.1秒。
- 180〜260語は15/20、180語未満5/20（165〜177語）。10ケース評価より短い回が増えた。
- 最高温度 BATTERY 36.9°C / SKIN 39.7°C / CPU 51.5°C。
