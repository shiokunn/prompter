# M8 版権キャラクターモード 設計案

- 作成: 2026-10-28
- 前提: M7ローカル標準 `0.7.5-m7`（v13.1、再採点合格）
- 目的: 版権キャラクター名を専用欄で受け取り、**キャラクター識別を上げる**。
- 制約: GPU版より先。ローカルLLMが主、クラウドは代替。入力への忠実さを優先する。

> **2026-10-28 決定事項・実装状況:** 名前と作品名の2欄＋候補、1人、既定外見の自動補完なし。日本語名はニケ＋主要10作品を目標とする。辞書外はDuckDuckGoの検索結果上位2件を英語名の候補として取り出す。検索は外部への名前・作品名の送信を伴うため、実装時には明示し、失敗時も入力を保持する。以下は設計案であり、実装済み仕様ではない。
>
> 初期辞書ビルダー `tools/build_characters.py` は全40,931件を取り込めることを確認済み。試作DBは約15 MiBで、後述の1〜2MBという予測は実測と異なる。日本語別名は現時点では2件の試験用データのみで、ニケや主要10作品の網羅は未完了。英語の `Cinderella` は6候補に一致する。日本語別名が1件しか登録されていなくても網羅的な検索結果とは限らないため、自動確定してはならない。作品タグの関連付けは元CSVからは確定できず、括弧だけを根拠に推測しない。

---

## 1. 現状のデータで分かったこと

同梱辞書（5,006タグ）と `research/data/danbooru.csv` を照合した結果:

| 項目 | 実測 |
|---|---:|
| Danbooru全キャラクタータグ（category 4） | 40,931 |
| 同・投稿500件以上 | 4,375 |
| 同・投稿1,000件以上 | 2,298 |
| 同梱辞書に入っているキャラクタータグ | **509** |
| Danbooru全作品タグ（category 3） | 9,478 |
| 同梱辞書に入っている作品タグ | 339 |
| 辞書の投稿数下限 | 1,781 |

- 辞書は「汎用タグ上位5,000」なので、キャラクターはほとんど入っていない。
- 例: `cinderella_(nikke)` は **585件** で辞書外。作品タグ `goddess_of_victory:_nikke` は16,634件で辞書内。
- **日本語のキャラクター名データは現状ない**。`danbooru-jp.csv` は汎用タグ427件のみ。
- 名前は曖昧: 「シンデレラ」だけで `cinderella_(nikke)` / `cinderella_(sinoalice)` / `cinderella_(grimm)` / `cinderella_(disney)` などが存在する。
- 現在の `isKnownTag` は同梱5,006タグのみを見るため、キャラクタータグをAIが返しても**検証で除去される**。

→ 専用欄だけでは足りない。**キャラクター辞書・名前解決・タグ固定・検証の例外**が必要。

---

## 2. 提案する全体像

```
[キャラクター名] [作品名(任意)]
        │
        ▼
  名前解決（characters.sqlite + 別名 + 候補）
        │  ResolvedCharacter(tag, name, seriesTag, confidence)
        ▼
  入力文 + 解決済みキャラクター
        │
        ├─ オフラインタグの先頭に character_tag, series_tag を固定
        ├─ ローカルLLMへ「この名前を改変するな」と明示
        ▼
  自然文 + タグ
        │
        ▼
  キャラクターガード（タグ復元・改名/比較表現の除去・名前の存在確認）
```

### 2.1 キャラクター辞書（新アセット）

`app/src/main/assets/characters.sqlite` を追加する（汎用辞書とは分離）。

```sql
characters(
  id INTEGER PRIMARY KEY,
  tag TEXT UNIQUE,          -- cinderella_(nikke)
  name TEXT,                -- cinderella
  series_tag TEXT,          -- goddess_of_victory:_nikke
  series_name TEXT,         -- nikke
  post_count INTEGER
)
character_aliases(
  id INTEGER PRIMARY KEY,
  character_id INTEGER,
  surface TEXT,             -- シンデレラ / Cinderella / シンデレラ(ニケ)
  norm TEXT,
  lang TEXT,                -- ja / en
  source TEXT,              -- curated / danbooru / mlkit
  confidence REAL
)
```

- `characters` は Danbooru の category 4 全40,931件から生成（英語名はこれで解決）。
- `series` は category 3 から生成。`cinderella_(nikke)` → `goddess_of_victory:_nikke`。
- 日本語別名は**手書きキュレーション**から始める。初期はニケ（73人、投稿100件以上）＋ユーザーが使う作品。
- DBサイズ見込み: キャラ＋作品＋英語別名で 1〜2MB（別ファイル、圧縮前）。

### 2.2 名前解決の順序

1. **作品名が入力されている**
   - `name + "_(" + series + ")"` の正規化で完全一致。
   - 一致しなければ作品内の候補を表示。
2. **作品名なし・英語名**
   - 正式タグ / 英語別名の完全一致。
   - 複数一致なら候補表示（例: Cinderella → 6件）。
3. **作品名なし・日本語名**
   - キュレーション別名の完全一致。
   - 複数一致なら候補表示（作品名つき）。
   - 一致なし → ML Kitで英訳 → 英語名として再解決（例: シンデレラ → Cinderella）。
4. **未解決**
   - 入力文字列をそのまま保持し、「未登録。PixAIが認識しない可能性」と表示。
   - 変換は続行（ブロックしない）。英語正式名の入力を促す。

### 2.3 タグ固定と検証

- 解決済みなら最終タグの**先頭**に `character_tag`、次に `series_tag` を置く。
- `isKnownTag` に「今回解決したキャラクタータグ」を許可リストとして渡す。
  （同梱辞書にない `cinderella_(nikke)` を消さないため）
- タグ重複を除去し、順序は `character, series, 入力由来タグ` を維持。
- タグ欄にはキャラクター名をUI上で別チップ表示し、ユーザーが削除できるようにする。

### 2.4 自然文の「気を配る」具体策

プロンプトにキャラクターブロックを追加する:

```text
Character (must preserve exactly):
- Name: Cinderella
- Series: Nikke
- Canonical tag: cinderella_(nikke)
- Series tag: goddess_of_victory:_nikke

Rules:
- Use the exact name "Cinderella" and the series "Nikke" in the first sentence.
- Never write "resembling", "like", "a version of", or a different character's name.
- Do not change the character's identity or series.
- Do not invent appearance details beyond the Japanese input.
```

決定論的ガード:

| 検査 | 動作 |
|---|---|
| タグに `character_tag` がない | 先頭に追加 |
| タグに `series_tag` がない | 2番目に追加 |
| 自然文に名前がない | 先頭に `The character is Cinderella from Nikke.` を挿入し、語数調整のため末尾の装飾文を1文削る |
| `resembling Cinderella` / `Cinderella-like` | `Cinderella` に置換（ID25の弱化表現） |
| 別シリーズ名が混入 | 警告を出し、自動では直さない（誤修正を避ける） |
| 未解決キャラ | 何もしない（通常変換） |

- 名前は翻訳パイプラインに通さない。**固有名として固定**する。
- メイン入力に同じ名前が書かれている場合は、重複を避けるため「キャラクター欄へ移動」を提案する。

### 2.5 外見の扱い（重要）

- M8aでは**キャラクターの既定外見を自動追加しない**。
  - 現行の忠実さ基準では、入力にない髪色・目の色の追加は重大違反になる。
  - キャラクタータグ自体が識別の主信号。PixAI側の知識に任せる。
- 将来オプション「既定の外見も補完」を追加する場合:
  - キュレーションした `hair/eyes/outfit` のみ。
  - **ユーザー入力が常に優先**。衝突時は入力を使い、警告を出す。
  - 評価時は「キャラクター選択由来の属性」として `statedTags` に含める。

---

## 3. UI案

```text
情景
[ 長い黒髪の少女が、青いドレスで椅子に座っている        ]

キャラクター（任意）
[ シンデレラ            ]  [ ニケ            ]
  → Cinderella / Nikke  [変更]

形式   [両方] [自然文] [タグ]
```

- 名前欄はオートコンプリート付き。候補は「名前 — 作品名」で表示。
- 候補選択で作品欄を自動補完。
- 未登録時は入力欄の下に小さく警告。
- 解決済みチップをタグ欄の上に表示（削除可能）。
- 初期実装は**1人**。複数キャラはM8b以降（`2girls` の分割構図と干渉するため）。

---

## 4. 履歴・保存

`HistoryStore` は現在 v1 で、`onUpgrade` がテーブルをDROPする実装。ここは**必ず移行**に直す。

```sql
ALTER TABLE history ADD COLUMN character_name TEXT NOT NULL DEFAULT '';
ALTER TABLE history ADD COLUMN character_tag TEXT NOT NULL DEFAULT '';
ALTER TABLE history ADD COLUMN series_tag TEXT NOT NULL DEFAULT '';
```

- 既存履歴を消さない。
- 履歴詳細にキャラクターを表示し、再変換時はキャラクター欄へ復元。
- `HistoryItem` に3フィールド追加。

---

## 5. 変更箇所（実装時）

| ファイル | 変更 |
|---|---|
| `MainActivity.kt` | キャラクター欄、解決状態、変換への受け渡し、警告表示 |
| `LocalLlm.kt` | `buildUserPrompt` にキャラクターブロック、システムプロンプトに改名禁止 |
| `FidelityGuard.kt` | キャラクタータグ復元、`resembling` 置換、名前の存在確認 |
| `PromptDb.kt` / 新 `CharacterDb.kt` | characters.sqlite の読み込み、名前解決 |
| `M1Matcher.kt` | `isKnownTag` の許可リスト対応（または呼び出し側で合成） |
| `HistoryStore.kt` | v2マイグレーション、キャラクター列 |
| `tools/build_characters.py` | Danbooru CSVから characters.sqlite を生成 |
| `research/aliases/character_aliases.tsv` | 日本語名のキュレーション（初期はニケ） |

---

## 6. 失敗しやすいケースと対策

| ケース | リスク | 対策 |
|---|---|---|
| シンデレラ（作品なし） | 別作品に誤解決 | 候補表示。自動選択しない |
| 日本語名が未登録 | 英訳が揺れる | ML Kit英訳→英語別名で再解決。失敗時は警告 |
| キャラクタータグが辞書外 | 検証で消える | 解決済みタグを許可リストに追加 |
| モデルが「resembling Cinderella」と書く | 識別弱化 | ガードで置換 |
| モデルが別シリーズを書く | 誤識別 | プロンプトで禁止＋警告。自動修正はしない |
| 著作権的理由で拒否 | 生成失敗 | 「固有名は事実識別子。拒否しない」と明示。拒否時はタグのみ提示 |
| キャラクター欄と本文の重複 | 冗長 | 重複検出して移動を提案 |
| シリーズ名の `:`（`goddess_of_victory:_nikke`） | UI/コピーで扱いにくい | タグはそのまま、表示は `Nikke` にする |

---

## 7. 評価計画

### 7.1 ユニット

- 名前解決: 作品あり / なし / 曖昧 / 未登録 / 英語 / 日本語。
- タグ順序: `character, series, ...`、重複なし。
- ガード: タグ欠落の復元、`resembling` 置換、名前なし時の挿入。
- 履歴: v1→v2で既存行が残る。

### 7.2 実機（8ケース＋キャラクター）

| ケース | 期待 |
|---|---|
| シンデレラ + ニケ + 既存ID25入力 | `cinderella_(nikke)` / `goddess_of_victory:_nikke` が先頭、本文に Cinderella、`resembling` なし |
| 初音ミク（英語名） | `hatsune_miku`（辞書内）が先頭 |
| シンデレラ（作品なし） | 候補が複数出て、勝手に決めない |
| 未登録キャラ | 警告のみ、通常変換は継続 |
| キャラクター欄なし × 8ケース | v13.1の出力・語数・タグが回帰しない |

### 7.3 成功条件

- 解決済みキャラのタグ保持 **100%**。
- 本文に正確な名前 **100%**、`resembling` **0%**。
- 未指定の外見追加 **0件**。
- キャラ欄なしの既存8ケースで回帰なし（忠実さ4.00、タグ4.125を維持）。

---

## 8. フェーズ分割

| フェーズ | 内容 | 完了条件 |
|---|---|---|
| M8a | キャラ欄1人＋作品欄、ニケの日本語別名、英語全キャラ、タグ固定、ガード、履歴v2 | シンデレラ/ニケで識別タグと名前が固定される |
| M8b | 候補UIの強化、ML Kit英訳フォールバック、複数キャラ、外見補完（任意） | 曖昧名・未登録名で破綻しない |
| M8c | 日本語別名の拡充（主要作品）、作品プリセット | 主要作品の日本語名で解決できる |

GPU版（M6.1）はM8aの後。

---

## 9. 未決事項（ユーザー確認）

1. 入力方式: 「名前＋作品名」の2欄＋候補（推奨）か、1欄の自由入力か、チップ選択か。
2. 未登録キャラ: 警告して続行（推奨）か、候補選択を必須にするか。
3. 既定外見の補完: M8aでは無効（推奨）か、任意トグルで有効にするか。
4. 初期収録: ニケ＋英語全キャラ（推奨）か、主要作品の日本語名も最初から入れるか。
5. 複数キャラ: M8aは1人（推奨）か、最初から2人まで対応するか。

---

## 10. 参考: 既存コードの接続点

- `LocalLlmPrompt.buildUserPrompt(input, offlineTags, unmatched)` にキャラクター情報を追加する。
- `MainActivity.convert` の `sourceInput` と `currentEngine` の間に解決処理を挟む。
- `FidelityGuard.correctTags(input, tags, isKnownTag, statedTags)` の `isKnownTag` を
  「同梱辞書 + 解決済みキャラクタータグ」に合成する。
- `HistoryStore` は v1 の `onUpgrade` がDROPなので、v2でALTERに変更する。
- 既存の `unmatched` 表示はキャラクター名を除外し、未登録警告と混ざらないようにする。
