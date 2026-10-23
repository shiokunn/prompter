# オンデバイスモデル選定（4B 以下・日本語 → 英語プロンプト）

- 日付: 2026-10-23
- 目的: OpenRouter を使わない人向けに、arrows Alpha（12GB RAM）で動く 4B 以下のモデルを選ぶ。
- 結論: **Gemma 4 E2B を本命、Gemma 4 E4B を 12GB 端末向けの任意高品質パックにする。**

---

## 1. 要件

| 要件 | 内容 |
|---|---|
| パラメータ | 実効 4B 以下（できれば 3B 以下） |
| 言語 | 日本語の自然文を理解し、英語の自然文を生成できる |
| 出力 | PixAI DiT 向けの 60〜180 語の英語プロンプト。タグも補助的に出せる |
| 端末 | arrows Alpha: Android 15/16、RAM 12GB、ROM 512GB、Dimensity 8350 Extreme、arm64 |
| 配布 | APK に同梱せず、任意ダウンロード |
| ライセンス | 個人利用可。できれば Apache-2.0 / MIT |
| 実装 | Android の Kotlin から呼べる。LiteRT-LM が第一候補 |

> 「4B 以下」は **実効パラメータ** と解釈する。Gemma 4 E2B は実効 2.3B だが、埋め込みを含む総パラメータは 5.1B。総パラメータを厳密に 4B 以下にしたい場合は Qwen3-4B しかないが、LiteRT-LM 公式外になる。

---

## 2. 候補比較

| モデル | 実効/総パラメータ | モデルサイズ | ライセンス | Android 実装 | 日本語 | 自然文 | 評価 |
|---|---:|---:|---|---|---|---|---|
| **Gemma 4 E2B** | 2.3B / 5.1B | 2.58GB（CPU）<br>2.01GB（GPU） | Apache-2.0 | **LiteRT-LM 公式** | 140+ 言語。日本語可 | 得意 | **◎ 本命** |
| **Gemma 4 E4B** | 4.5B / 8B | 3.65GB（CPU）<br>2.97GB（GPU） | Apache-2.0 | **LiteRT-LM 公式** | E2B より強い | 得意 | **○ 高品質パック**（4B 超） |
| Qwen3-4B-Instruct-2507 | 4B | 約2.5GB（Q4_K_M 目安） | Apache-2.0 | llama.cpp JNI 等。LiteRT-LM 公式外 | 強い | 得意 | △ 実装コスト高 |
| Phi-4-mini | 3.8B | 3.91GB | MIT | LiteRT-LM 公式 | 英語中心。日本語は弱い | 普通 | △ 日本語用途で不利 |
| Qwen2.5-1.5B | 1.5B | 1.60GB | Apache-2.0 | LiteRT-LM 公式 | 限定的 | 弱い | △ 単純な翻訳向け |
| Qwen3-0.6B | 0.6B | 586MB | Apache-2.0 | LiteRT-LM 公式 | 弱い | 弱い | × メイン用途には不足 |
| Gemma 3 1B | 1B | 1.0GB | Gemma license | LiteRT-LM 公式 | 弱い | 弱い | × メイン用途には不足 |
| DanbotNL-2408-260M | 0.26B | 524MB | Apache-2.0 | 独自アーキテクチャ。要変換 | 日本語→タグ専用 | タグのみ | △ タグ専用・自然文不可 |

### 2.1 Gemma 4 E2B を選ぶ理由

1. **LiteRT-LM 公式対応**。Kotlin API があり、Android への組み込みが最も現実的。
2. **モバイル向け設計**。Per-Layer Embeddings と量子化で、実行時メモリを抑えている。
3. **多言語**。140+ 言語をサポートし、日本語入力から英語出力に向く。
4. **自然文生成**。PixAI DiT が必要とする「説明的な英文」を作れる。Danbot のようにタグだけではない。
5. **ライセンスが Apache-2.0**。個人利用・改変・再配布の制約が小さい。
6. **サイズが現実的**。CPU 版 2.58GB / GPU 版 2.01GB。arrows Alpha の 512GB に余裕で収まる。

### 2.2 E4B を「任意」にする理由

- 実効 4.5B で、厳密な「4B 以下」を超える。
- モデル 3.65GB、CPU メモリ約 3.3GB。12GB 端末なら動くが、E2B より遅い。
- 公式ベンチ（S26 Ultra）で E2B の約 2.5 倍遅い:
  - E2B: CPU 47 tok/s / GPU 52 tok/s
  - E4B: CPU 18 tok/s / GPU 22 tok/s
- arrows Alpha は S26 Ultra より GPU/CPU が弱いため、E4B は 120 語生成に 20〜40 秒かかる可能性がある。
- したがって、**E2B を既定**にし、E4B は「12GB 以上・高品質」設定で任意ダウンロードにする。

### 2.3 Qwen3-4B を第一候補にしない理由

- モデル自体は 4B で日本語も強いが、**LiteRT-LM の公式対応モデルではない**。
- llama.cpp を JNI で組み込む必要があり、ビルド・量子化・チャットテンプレート・メモリ管理の検証コストが増える。
- 個人用アプリの初期版では、公式 Kotlin API がある Gemma 4 E2B の方がリスクが低い。
- 将来、E2B の品質が不足した場合の比較候補として残す。

### 2.4 Danbot 260M を第一候補にしない理由

- 日本語 → Danbooru タグ専用。PixAI DiT に必要な自然文を生成しない。
- 2024/08 までのデータで、独自アーキテクチャのため ONNX/LiteRT 変換が必要。
- タグモードの補助（候補タグ抽出）には使えるが、メインの自然文生成には不向き。
- 将来、タグ専用の小型モデルとして再評価する。

---

## 3. 性能の見積もり

### 3.1 公式ベンチマーク（S26 Ultra）

| モデル | バックエンド | Prefill (tok/s) | Decode (tok/s) | 初回トークン | ピークメモリ |
|---|---|---:|---:|---:|---:|
| Gemma 4 E2B | CPU | 557 | 47 | 1.8s | 1,733MB |
| Gemma 4 E2B | GPU | 3,808 | 52 | 0.3s | 676MB |
| Gemma 4 E4B | CPU | 195 | 18 | 5.3s | 3,283MB |
| Gemma 4 E4B | GPU | 1,293 | 22 | 0.8s | 710MB |

### 3.2 arrows Alpha での推定

Dimensity 8350 Extreme / Mali-G615 は S26 Ultra（Snapdragon 8 Elite 系）よりかなり遅い。以下は**推定**で、M6 で実測する。

| モデル | バックエンド | Decode 推定 | 120語（約180トークン）の生成時間 |
|---|---|---:|---:|
| E2B | CPU | 20〜30 tok/s | 6〜9秒 |
| E2B | GPU | 15〜25 tok/s | 7〜12秒 |
| E4B | CPU | 8〜12 tok/s | 15〜23秒 |
| E4B | GPU | 8〜12 tok/s | 15〜23秒 |

- 初回ロードは 3〜10 秒。2回目以降はキャッシュで短縮。
- 生成的には 10 秒以内なら許容。E2B が第一候補。
- GPU が Mali で動かない場合は CPU にフォールバックする。

---

## 4. 実装方法（LiteRT-LM）

### 4.1 依存関係

```kotlin
dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
}
```

バージョンは実装時に固定する。`latest.release` のままにしない。

### 4.2 Manifest

GPU バックエンドを使う場合:

```xml
<application>
    <uses-native-library android:name="libvndksupport.so" android:required="false"/>
    <uses-native-library android:name="libOpenCL.so" android:required="false"/>
</application>
```

### 4.3 初期化

```kotlin
val config = EngineConfig(
    modelPath = modelFile.absolutePath,
    backend = Backend.GPU(), // 失敗したら Backend.CPU()
    cacheDir = context.cacheDir.path,
)
val engine = Engine(config)
engine.initialize()
val conversation = engine.createConversation(
    ConversationConfig(
        systemInstruction = Contents.of(SYSTEM_PROMPT),
        samplerConfig = SamplerConfig(topK = 10, topP = 0.95, temperature = 0.3),
    )
)
```

- `initialize()` は 10 秒以上かかる場合がある。必ず IO ディスパッチャで実行する。
- `engine` はアプリ内で再利用する。画面回転のたびにロードしない。
- メモリ圧迫時は `close()` し、次回リロードする。

### 4.4 MTP（Multi-Token Prediction）

GPU では MTP を有効にすると最大 2.2 倍高速になる。

```kotlin
@OptIn(ExperimentalApi::class)
ExperimentalFlags.enableSpeculativeDecoding = true
```

- E2B の CPU では自由生成時に逆効果になる場合があるため、GPU 優先。
- E4B は CPU でも有効。

### 4.5 モデルファイル

| モデル | ファイル | サイズ | SHA-256 |
|---|---|---:|---|
| E2B CPU | `gemma-4-E2B-it.litertlm` | 2,588,147,712 B | `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` |
| E2B GPU | `gemma-4-E2B-it-gpu.litertlm` | 2,008,432,640 B | `a53a59001894c58e6bdb5b9b227709f91a2e3e556baa7d85acf9c55402ba5cf5` |
| E4B CPU | `gemma-4-E4B-it.litertlm` | 3,659,530,240 B | `0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0` |
| E4B GPU | `gemma-4-E4B-it-gpu.litertlm` | 2,969,059,328 B | `4912bb5a9c30993c51a7711f763212077458529312175df0573a78323a2bb7ff` |

取得元:

- https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm

ダウンロード URL の例:

```
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm
```

- Android の `DownloadManager` を使い、Wi-Fi 限定・通知・再開を任せる。
- ダウンロード後に SHA-256 を検証する。
- 失敗時は再試行。途中ファイルは削除。
- モデルは `filesDir/models/` に置く。`cacheDir` は OS に消される可能性がある。

### 4.6 端末チェック

- RAM 8GB 以上: E2B を表示。
- RAM 12GB 以上: E4B も表示。
- 空き容量: モデルサイズ + 1GB 以上。
- API 29 以上、arm64-v8a。
- GPU 初期化に失敗したら CPU にフォールバック。
- バッテリー残量が少ない場合は警告。

---

## 5. 品質検証計画

### 5.1 比較する方式

1. 辞書 + ML Kit + テンプレート（LLM なし）
2. Gemma 4 E2B（CPU / GPU）
3. Gemma 4 E4B（CPU / GPU）
4. OpenRouter: `google/gemini-3.1-flash-lite`
5. OpenRouter: `google/gemini-2.5-flash-lite`（安価な比較）

### 5.2 評価セット

- 30プロンプト:
  - 10: 単純（1キャラ、服装、場所）
  - 10: 複雑（構図、照明、スタイル、複数要素）
  - 5: 複数キャラ（2〜3人、属性の混線チェック）
  - 5: 成人向けタグを含む入力（タグの保持と拒否しないことの確認）
- 各プロンプトで、元の日本語に含まれる要素を箇条書きにした正解リストを作る。

### 5.3 指標

| 指標 | 内容 | 目標 |
|---|---|---|
| Fidelity | 元の要素をどれだけ保持したか（人手 1〜5） | E2B 3.5 以上 / E4B 4.0 以上 |
| English quality | 英文として自然か（人手 1〜5） | 3.5 以上 |
| Tag recall | 正解タグのうち出力に含まれた割合 | 50% 以上（補助） |
| Tag precision | 出力タグのうち正解の割合 | 75% 以上（補助） |
| Latency | 入力から完了まで | E2B 15秒以内 / E4B 30秒以内 |
| Memory | ピークメモリ | 4GB 以下 |
| Stability | クラッシュ・OOM | 0 |

### 5.4 Go / No-Go

- **E2B Go**: Fidelity 3.5 以上、15秒以内、クラッシュなし。
- **E4B Go**: Fidelity 4.0 以上、30秒以内。12GB 端末限定で表示。
- 届かない場合:
  - プロンプトを短くする（80語）。
  - few-shot を追加する。
  - 辞書の候補タグを渡す（RAG）。
  - それでもダメなら「ベータ」扱いにし、既定は辞書 + ML Kit にする。

---

## 6. ローカル LLM のプロンプト設計

小型モデルは長いシステムプロンプトや複雑な JSON が苦手。以下を守る。

- システムプロンプトは 300〜600 トークン程度。
- few-shot は 2〜3 例。
- 出力は JSON より、区切り文字形式の方が安定する場合がある。

例:

```
You convert Japanese image prompts into English for PixAI DiT.
Follow: Subject + Action + Scene + Mood + Style.
Output exactly:

PROMPT_BEGIN
<English prompt, 80-150 words>
PROMPT_END
TAGS: <comma-separated canonical tags, max 20>
NEGATIVE: <comma-separated, or empty>
NOTES: <short Japanese note>
```

- `PROMPT_BEGIN` / `PROMPT_END` で自然文を抽出する。
- タグは辞書で検証し、存在しないものは捨てる。
- 日本語が混入したら再生成 or 除去。
- 温度は 0.2〜0.4。創造性より忠実性を優先。
- 入力をそのまま英訳するのではなく、PixAI の構文に再構成する。

---

## 7. リスク

| リスク | 影響 | 対策 |
|---|---|---|
| E2B の日本語→英語品質が低い | ローカル AI の価値が下がる | 30例で評価。不足なら辞書 + ML Kit を既定にし、E2B はベータ |
| Mali GPU で LiteRT-LM が動かない | 速度低下 | CPU フォールバック。GPU 版は任意 |
| 2.6GB ダウンロードが失敗する | 使えない | DownloadManager、再開、SHA-256、Wi-Fi 限定 |
| 12GB RAM でも OS に kill される | 生成中断 | フォアグラウンド処理、画面維持、モデルを小まめに解放 |
| E4B が遅い | UX 悪化 | 12GB 端末のみ、任意ダウンロード、推定時間を表示 |
| モデルのライセンス条項 | 再配布時に問題 | Apache-2.0 を確認。NOTICE を同梱 |
| 成人向け表現でモデルが拒否する | タグが欠落 | ローカルモデルは検閲しないプロンプトにする。クラウドはプロバイダ依存と明記 |

---

## 8. 参考 URL

- LiteRT-LM Overview / Supported Models: https://developers.google.com/edge/litert-lm/overview
- LiteRT-LM Android Kotlin: https://developers.google.com/edge/litert-lm/android
- Gemma 4 E2B (LiteRT): https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Gemma 4 E4B (LiteRT): https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm
- Gemma 4 model card: https://huggingface.co/google/gemma-4-E2B-it
- Gemma 4 docs: https://ai.google.dev/gemma/docs/core
- Qwen3-4B-Instruct-2507: https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507
- DanbotNL-2408-260M: https://huggingface.co/dartags/DanbotNL-2408-260M

（2026-10-23 時点）
