# 確定した要件（2026-10-23）

ユーザーへのヒアリングで決まった前提。今後の実装はこのファイルを優先する。

## 1. 対象サービス

- **第一対象: PixAI の DiT 系モデル**
  - Tsubaki.3（最新フラッグシップ）
  - Tsubaki.2 / Tsubaki.1
  - 必要なら Serin 系
- PixAI DiT は **タグ列ではなく英語の自然言語プロンプト** を強く推奨する。
  - 基本式: `Subject + Action + Scene + Mood + Style`
  - 詳細は [`PIXAI_DIT.md`](PIXAI_DIT.md) を参照。
- 第二対象: タグ形式のモデル
  - PixAI Haruka v2（SDXL）
  - Stable Diffusion / SDXL / Pony / Illustrious
  - A1111 / ComfyUI
  - NovelAI
  - Flux
  - Midjourney

→ **PixAI DiT 用の「自然文モード」を既定**にし、タグモードは別に残す。両方を同じ入力から生成する。

## 2. AI モード

- **OpenAI API 互換** のクライアントを実装する。
- 既定の接続先は **OpenRouter**:
  - Base URL: `https://openrouter.ai/api/v1`
  - Endpoint: `/chat/completions`
  - モデルは固定せず、設定画面で選択・変更できるようにする。
- **任意 ON**。オフラインでも辞書 + ML Kit で動く。
- API キーは **BYOK**。アプリに開発者のキーを埋め込まない。
- Base URL とモデル名を変更できるようにし、Ollama / LM Studio などのローカル OpenAI 互換サーバーにも将来対応できる余地を残す。
- 構造化出力（JSON Schema）を使い、使えないモデルでは JSON プロンプト + パースでフォールバックする。

## 3. AI を使わない人向けのオンデバイスモデル

- 条件: **4B 以下（実効パラメータ）**、日本語 → 英語の自然文、Android、12GB RAM で動く。
- 第一候補: **Gemma 4 E2B**（実効 2.3B、2.58GB、Apache-2.0、LiteRT-LM 公式対応）。
- 第二候補: **Gemma 4 E4B**（実効 4.5B、3.65GB）。厳密な「4B 以下」ではないため、12GB 端末向けの任意「高品質パック」扱い。
- Qwen3-4B / Danbot 260M は今回の第一候補にしない。
  - Qwen3-4B は LiteRT-LM 公式モデルではなく、llama.cpp/JNI の追加実装が必要。
  - Danbot 260M はタグ専用で、PixAI DiT に必要な自然文生成には向かない。
- 詳細は [`MODEL_SELECTION.md`](MODEL_SELECTION.md)。

## 4. 配布

- 最初は **自分用 APK**。Play Store 公開はしない。
- 実機: **arrows Alpha**
  - Android 15 / 16
  - RAM 12GB / ROM 512GB
  - SoC: MediaTek Dimensity 8350 Extreme
  - GPU: Mali-G615 系
  - arm64-v8a
- APK は GitHub Actions でビルドし、Termux から `adb install` する。
- モデルは APK に同梱せず、初回に任意ダウンロードする。

## 5. 成人向けタグ

- **そのまま扱う。フィルタしない。**
- 個人用 APK なので、年齢制限・フィルタ・審査対応は MVP では行わない。
- 設定に「成人向けタグを含める」トグルを置く（既定 ON）。ただしデータ自体は削除しない。
- PixAI 側・OpenRouter のプロバイダ側でブロックされる可能性は、アプリ内で注意書きを出す。
- 公開・配布する場合は、法的・倫理的・各サービスの規約を別途確認する。

## 6. アプリ名・その他

- アプリ名は暫定で **Prompter**。
- 画像生成そのものの API 連携はしない（プロンプトを作るところまで）。
- カスタム IME は MVP では作らない。`ACTION_PROCESS_TEXT` / `ACTION_SEND` で他アプリに送る。
- クラウド同期・アカウント・課金は作らない。

## 7. 未決事項

1. ローカル LLM を最初の APK に入れるか、コア完成後の追加にするか。  
   推奨: コア（M2〜M5）を先に完成させ、M6 で E2B を追加。E4B は実機ベンチで判断。
2. OpenRouter の API キーをいつ設定するか。  
   M1 の別名生成と M4 の AI テストで必要。キーが無い間は、既存の手作業訳 + 機械訳 + 手動キュレーションで進める。
3. アプリ名を `Prompter` のままにするか。  
   実装には影響しない。
