# M6 ローカルLLM 実機評価（10ケース）

- 日付: 2026-10-25
- 端末: arrows Alpha（M08 / Android 16 / RAM 12GB / arm64-v8a）
- APK: `Prompter-0.6.1-m6-debug.apk`（versionCode 9）
- モデル: Gemma 4 E2B IT / LiteRT-LM 0.18.1 / CPU 4 threads
- 設定: `maxNumTokens=4096`, `maxOutputToken=768`, topK 10, topP 0.95, temperature 0.3, seed 42, thinking off
- 評価方法: アプリの履歴にある日本語シーンを開き、オフライン変換→ローカルLLM補完を実行。出力と語数は履歴DBから取得。
- 注意: スコアはAIによる**暫定の一次評価**。最終的な人手評価ではない。

## 1. 総合結果

| 指標 | 結果 |
|---|---|
| ケース数 | 10 |
| 平均語数 | 89.0語 |
| 80〜150語に入ったケース | 7/10 |
| 80語未満 | 3/10（77語、79語、71語） |
| 生成拒否 | 0/10（成人向け2件を含む） |
| 平均生成時間 | 15.6秒（12.8〜18.8秒） |
| 初回ロード（コールド） | 6.4秒 |
| 再ロード（ページキャッシュ後） | 0.5秒 |
| アプリPSS（2回生成後） | 1.73GB |
| アプリPSS（10回生成後） | 1.95GB |
| 終了後PSS | 193〜219MB |
| 発熱・バッテリー | 10回連続で大きな変化なし。バッテリー温度36.9°C、SKIN 38.5°C、CPU 49.7°C |

### 暫定スコア（5点満点）

| 観点 | 平均 |
|---|---|
| 意図の保持 | 4.15 |
| 英語の自然さ | 4.25 |
| 構造（主題・動作・情景・光・画風） | 4.70 |
| **総合平均** | **4.37** |

目標3.5/5に対して暫定Go。ただし、最終評価はユーザーが10〜20件で確認する。

## 2. ケース別結果

| # | ID | 入力の要点 | 語数 | 生成時間 | 意図 | 自然さ | 構造 | 総合 | 主な所見 |
|---|---|---|---|---:|---:|---:|---:|---:|---|
| 1 | 20 | 長い黒髪、青いドレス、椅子に座る | 86 | 15.0s | 4.5 | 4.5 | 5.0 | 4.67 | 木の椅子・視線・室内を補完。自然 |
| 2 | 21 | 黒ボブ、水色インカラー、黒フリル＋白レース、上半身、左側、上目遣い | 110 | 18.8s | 4.5 | 3.5 | 5.0 | 4.33 | `in-color hair`、`smile mixed with a neutral expression in the center of her face`が不自然 |
| 3 | 22 | 明るい黄色の髪、紫の目、ロングヘアー | 94 | 15.2s | 4.0 | 4.5 | 5.0 | 4.50 | 服装・表情を汎用的に補完。入力外の具体物は少ない |
| 4 | 24 | 二人の少女、ベッドに座る、軽く微笑む | 77 | 13.6s | 4.0 | 4.5 | 4.0 | 4.17 | 髪・服装を補完。77語で目標下限に3語届かず |
| 5 | 25 | 白髪赤目、メイド服、オムライス、3分割、料理工程、中央で差し出す、ロングヘア | 85 | 17.6s | 4.5 | 4.5 | 5.0 | 4.67 | 3分割・料理・中央提示・控えめな胸元を保持。キッチンは補完 |
| 6 | 26 | 白いドレス、野原に立つ、こちらを見る | 79 | 13.7s | 4.0 | 4.5 | 4.0 | 4.17 | 髪・風・空を補完。79語で下限に1語届かず |
| 7 | 27 | 縦半分、壁線、右=学生服で寄りかかる、左=メイド服でトレーとジュース、白ボブ | 108 | 17.0s | 3.5 | 4.5 | 5.0 | 4.33 | 構図・服装・左手・トレーは保持。**白ボブの「白」が両者から抜けた** |
| 8 | 28 | 成人向け、乳首を擦る | 71 | 12.8s | 4.0 | 3.5 | 4.0 | 3.83 | 拒否なし・内容保持。`intensely focused on her own chest`がやや不自然。71語 |
| 9 | 29 | 白髪赤目、メイド服、オムライス、3分割、料理工程 | 87 | 14.7s | 4.5 | 4.5 | 5.0 | 4.67 | 3分割・料理工程を保持。キッチンは補完 |
| 10 | 30 | 成人向け短文「エッチなお嬢さん」 | 93 | 17.8s | 4.0 | 4.0 | 5.0 | 4.33 | 拒否なし。ランジェリー・部屋・髪を補完。性的表現を保持 |

## 3. 出力全文

### 1. ID 20（86語）
入力: 長い黒髪の少女が青いドレスを着て、椅子に座っている

> A beautiful young girl with long black hair is elegantly sitting on a wooden chair. She is wearing a flowing, vibrant blue dress that contrasts beautifully with her dark hair. Her posture is relaxed yet poised as she gazes softly towards the viewer with a gentle expression. The setting is a simple, softly lit indoor space with muted background details. Warm, diffused lighting casts a gentle glow over the scene, creating a serene and contemplative atmosphere. This image is rendered in a high-quality, detailed anime style.

### 2. ID 21（110語）
入力: 黒い髪でボブカットで水色のインカラーをした女の子。服装は黒のフリルつきレースと上から白いレースを重ねてきている。上半身をアップで描く。笑顔と普通の表情の真ん中。黒い目。左側から見た構図。少し振り向いている。上目遣い

> A beautiful girl with black bob-cut hair and light blue in-color hair is featured in this upper-body portrait. She wears a black frilly lace outfit layered with white lace over it. The composition is a side view, showing her slightly turning her head towards the viewer. She has a gentle smile mixed with a neutral expression in the center of her face. Her dark eyes are captivating and she looks up with a sweet, innocent gaze. The background is softly blurred to keep the focus entirely on the subject. Soft, diffused lighting illuminates her features beautifully in this intimate scene. The art style is high-quality anime rendering with detailed textures.

### 3. ID 22（94語）
入力: 明るい黄色の髪の毛、紫の目、ロングヘアー、

> A beautiful anime girl with long, bright blonde hair and striking purple eyes is featured prominently. She has flowing, long hair that cascades down her shoulders in soft waves. She is wearing simple, elegant clothing that complements her vibrant appearance. Her expression is serene and slightly mysterious, gazing directly at the viewer with intense purple eyes. The scene is set in a softly lit, ethereal environment with gentle ambient light. Warm, bright lighting illuminates her features, creating a dreamy and inviting atmosphere. The art style should be high-quality, detailed anime rendering with smooth shading.

### 4. ID 24（77語）
入力: 二人の少女がベッドに座って、軽く微笑んでいる

> Two beautiful young girls are sitting closely together on a soft bed. They share a gentle, warm smile directed towards the viewer. Long, flowing hair frames their faces beautifully in the soft light. They are wearing simple, comfortable clothing suitable for a relaxed atmosphere. The scene is set in a cozy, dimly lit bedroom environment. Soft, warm ambient lighting creates a very intimate and peaceful mood. The image should be rendered in a high-quality, detailed anime style.

### 5. ID 25（85語）
入力: 白い髪の毛で赤い目をした勝利の女神:ニケのシンデレラが、全身をしっかりメイド服を着てオムライスを作っている。（露出が非常に少ない、胸元を隠す）背景は真ん中を中心とした3分割。それぞれの画面で、シンデレラが、一生懸命料理を作っているシーンを一場面ごとに違う料理工程で見せる。画面の中央にできたオムライスを視聴者に笑顔で差し出しているようにする。髪型はロングヘアーにする

> A beautiful goddess figure with long white hair and striking red eyes is diligently preparing an omurice. She is wearing a detailed maid uniform that modestly conceals her figure well. The scene is divided into three distinct panels showing different cooking stages of the meal. In the center, she presents the finished omurice to the viewer with a warm, inviting smile. Soft, bright lighting illuminates the kitchen setting, highlighting the detailed food preparation. The art style should be high-quality, detailed anime rendering with vibrant colors.

### 6. ID 26（79語）
入力: 白いドレスを着た少女が野原に立って、こちらを見ている

> A beautiful young girl wearing an elegant white dress stands alone in a wide, open field. She is looking directly towards the viewer with a gentle and serene expression on her face. Her long, flowing hair catches the soft breeze across the grassy expanse. The setting is a vast, sunlit meadow under a bright, clear sky. Soft, warm sunlight illuminates the scene, creating a peaceful and ethereal atmosphere. The art style should be high-quality anime rendering with detailed textures.

### 7. ID 27（108語）
入力: 画面を真ん中で縦半分にした構図で、割った部分の線は壁として描く。右半分には壁に寄りかかっているような姿勢で、学生服を着ている白いボブカットの女の子を描く。左半分にはメイド服を着て、左手でトレーを持って、ジュースを乗せて持っている白いボブカットの女の子を描く。

> A vertical split composition divides the scene down the center with a wall line. On the right side, a girl with a bob cut leans against the wall in a relaxed posture. She is wearing a neat school uniform and has a gentle expression on her face. On the left side, another girl in a maid outfit holds a tray with a juice drink. She is holding the tray with her left hand in a charming manner. The background is a simple, neutral interior setting for this split scene. Soft, diffused lighting illuminates the entire composition evenly. The art style should be high-quality anime rendering with detailed shading.

### 8. ID 28（71語、成人向け）
入力: んぅ、シコシコシコシコ♡乳首をスリスリ♡スリスリ♡

> A beautiful anime girl is intensely focused on her own chest. She has long, flowing hair and delicate features. Her expression is one of deep, sensual pleasure and desire. She is gently caressing her nipples with soft, deliberate movements. The scene is dimly lit with warm, intimate ambient lighting. A soft, luxurious bedroom setting provides a private and secluded atmosphere. The rendering style should be highly detailed and vibrant anime illustration.

### 9. ID 29（87語）
入力: 白い髪の毛で赤い目をしたニケのシンデレラが、メイド服を着てオムライスを作っている。背景は真ん中を中心とした3分割。それぞれの画面で、シンデレラが、一生懸命料理を作っているシーンを一場面ごとに違う料理工程で見せる

> A beautiful maiden with pure white hair and striking red eyes is diligently preparing an omurice. She is wearing a crisp, detailed maid uniform with delicate accents. The scene is divided into three distinct panels focusing on different stages of the cooking process. In each panel, the maiden shows her earnest effort while making various steps of the omurice preparation. Soft, warm lighting illuminates the kitchen setting, creating a cozy and inviting atmosphere. The art style should be high-quality anime rendering with intricate details and vibrant colors.

### 10. ID 30（93語、成人向け）
入力: エッチなお嬢さん

> A beautiful young woman is posing sensually in a dimly lit, luxurious room. She has long, flowing hair and captivating, alluring eyes that hold a seductive gaze. She is wearing revealing, elegant lingerie that accentuates her figure beautifully. Her pose is provocative, with a confident and overtly sexual expression on her face. The background is softly blurred, suggesting an intimate, private setting with rich textures. Soft, dramatic lighting highlights her curves and creates deep shadows for a moody atmosphere. The art style is highly detailed anime rendering with smooth shading and vibrant colors.

## 4. 考察

### 良かった点

- プロンプト改善（6文構成、90〜130語、設定・光・画風の明示）で、単純入力でも86語まで伸びた。
- 複雑な3分割・二人構図・成人向け入力でも、主要要素と拒否なしを両立。
- 背景・光・画風を補完することで、PixAI向けの自然文としての完成度が上がった。
- 成人向け2件で拒否・検閲・英語以外の混入なし。
- 10回連続生成後もアプリPSSは1.95GBに収まり、終了後219MBまで解放された。

### 残る課題

- 3/10が80語未満（77、79、71語）。短文入力ではまだ短い。
- `light blue in-color hair`、`smile mixed with a neutral expression in the center of her face`、`intensely focused on her own chest`など、英語として不自然な箇所。
- 入力にない服装・髪・キッチンなどを補完する場合がある。汎用的な補完は許容範囲だが、意図を変える具体物は避けたい。
- 縦分割ケースで「白いボブカット」の「白」が抜けた。色の保持を強化する余地。
- 生成時間は平均15.6秒。以前の10秒より長くなった。品質とのトレードオフ。
- 10回でPSSが1.73GB→1.95GBに増加。長時間で安定するか、20〜30回で再確認したい。

## 5. 次の判断

暫定評価は総合4.37/5で、M6の品質目標3.5/5を上回る。**M6は条件付きGo**とし、次を実施してから最終確定する。

1. ユーザーによる10〜20件の人手評価（本レポートのスコアを叩き台にする）。
2. 20〜30回連続生成でのメモリ増加・発熱・バッテリーの再確認。
3. 短文ケースの語数対策（プロンプトの最終調整または77〜79語を許容するか決定）。
4. 必要ならGPU版（M6.1）で速度改善。
