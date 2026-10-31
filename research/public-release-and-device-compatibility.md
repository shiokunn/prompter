# 公開配布に向けた端末対応と署名の調査

- 更新日: 2026-10-31
- 対象: Prompter `0.10.0`（versionCode 24）
- 関連: `.github/workflows/release-apk.yml`、`.github/workflows/device-matrix.yml`、`tools/verify_apk.py`、`app/build.gradle.kts`

## 結論

- 公開版は **Android 10（API 29）以上 / arm64-v8a・x86_64** を対象とする。32bit 端末は同梱ライブラリの都合で対象外。
- 公開版のアプリケーション ID は `com.prompter.app.release`。開発版 `com.prompter.app` と共存できる。
- 公開用署名は新規の RSA 4096bit 鍵で作成し、GitHub Actions Secrets にのみ保存した。リポジトリには含めていない。
- Android 10 / 13 / 14 / 15 / 16 の x86_64 エミュレータで起動スモークテストが成功した。
- 署名済み Release APK は CI でパッケージ名、同梱 ABI、16KB ページ整合、署名を検証済み。

## ABI の調査

APK に含まれるネイティブライブラリの ABI は、依存ライブラリ側の同梱状況で決まる。

| 依存 | 同梱 ABI |
| --- | --- |
| LiteRT-LM `0.18.1`（`liblitertlm_jni.so`） | arm64-v8a, x86_64 |
| ML Kit Translate `17.0.3`（`libtranslate_jni.so`） | arm64-v8a, armeabi-v7a, x86, x86_64 |
| androidx.graphics path | arm64-v8a, x86_64 |

LiteRT-LM が 32bit を提供しないため、端末内 AI を任意機能にしても 32bit ビルドは成立しない。`ndk.abiFilters` を `arm64-v8a` と `x86_64` の 2 つに限定し、APK サイズの増加を抑えつつエミュレータ検証を可能にした。

- 変更前の debug APK: 約 88MB（arm64-v8a のみ）
- 変更後の debug APK: 約 132MB（arm64-v8a + x86_64）
- 署名済み release APK `Prompter-0.10.0.apk`: 約 123MB

### 16KB ページサイズ

Android 15 以降、一部端末は 16KB ページで動作する。APK 内の全 64bit ELF の `PT_LOAD` セグメントが 16384 バイト境界に揃っていることを確認した。`tools/verify_apk.py` が CI で同じ検査を行う。

## OS バージョン

- `minSdk = 29`（Android 10）。LiteRT-LM の AAR は `minSdkVersion 24`、ML Kit は 21 以上。
- `targetSdk = 36`。
- API 29 では Compose・Room・ML Kit の動作に追加の分岐を必要としない。
- 端末内 AI は CPU 実行で、8GB 以上の RAM と約 2.6GB の空き容量を要求する（`LocalModelSpec.MIN_RAM_BYTES`）。この条件は変更していない。

## エミュレータ検証

`.github/workflows/device-matrix.yml` を追加し、`connectedDebugAndroidTest` で起動スモークテストを実行した。

- 対象: API 29（default）, 33 / 34 / 35 / 36（google_apis）、いずれも x86_64
- テスト内容: 同梱の `promptdb.sqlite` と `characterdb.sqlite` を開けること、`MainActivity` が終了せず起動すること
- 結果: 初回 run `41155091291` は 5 ジョブすべて失敗。原因はエミュレータ起動前の空き容量不足（`Not enough space to create userdata partition`）。
- 修正: ランナーの未使用ツールチェーンを削除して空き容量を確保し、run [41156886414](https://github.com/shiokunn/prompter/actions/runs/41156886414) で 5 ジョブすべて成功。

## 署名

- 鍵種別: RSA 4096bit、PKCS12、有効期限 10,000 日（2054-03-18 まで）
- DN: `CN=Prompter, OU=Release, O=Prompter, L=Tokyo, ST=Tokyo, C=JP`
- 証明書 SHA-256: `7F:B2:A7:A7:C8:76:0F:E5:E7:B2:FB:BA:24:F8:E2:D1:19:83:AF:71:48:40:0B:6C:AC:8B:62:34:DD:11:D0:DB`
- APK Signature Scheme v2 で署名（minSdk 29 のため v1 は不要）

GitHub Secrets:

| Secret | 用途 |
| --- | --- |
| `PROMPTER_KEYSTORE_BASE64` | keystore の Base64 |
| `PROMPTER_STORE_PASSWORD` | keystore パスワード |
| `PROMPTER_KEY_ALIAS` | 鍵エイリアス |
| `PROMPTER_KEY_PASSWORD` | 鍵パスワード |

keystore とパスワードの控えはリポジトリ外（`~/.prompter-release/`）に置いた。この環境を失うと更新版を配布できなくなるため、オフラインでのバックアップが必要。

## CI での検証

`release-apk.yml` は次の順で実行する。

1. Python の辞書テストと Kotlin 単体テスト
2. `assembleRelease`
3. `tools/verify_apk.py` によるアプリID・ABI・16KB 整合の検証
4. `apksigner verify --print-certs` による署名検証
5. `v*` タグのとき GitHub Release を作成

run [41155081899](https://github.com/shiokunn/prompter/actions/runs/41155081899) で、`com.prompter.app.release` / versionCode 24 / arm64-v8a + x86_64 / ネイティブライブラリ 6 件 / v2 署名を確認した。

## 未検証・制限

- 実機は arrows Alpha（arm64-v8a / Android 16）のみ。他メーカー・他SoCの実機では未確認。
- x86_64 の ChromeOS 実機では未確認（エミュレータのみ）。
- 8GB RAM 端末での端末内 AI 実行は未確認。
- 32bit 端末と Android 9 以下は対象外。
- 公開版 `com.prompter.app.release` は `127.0.0.1:5555` にインストールし、開発版と共存したまま起動することを確認済み（2026-10-31）。他メーカー実機での公開版起動は未確認。

## ランチャーアイコン

- ユーザー提供の万年筆アートワーク（1254×1254）をアプリアイコンに採用した（`0.10.1`）。
- アダプティブアイコンの表示領域は 72dp、レイヤーは 108dp のため、アートワークを 108dp の中央 2/3（72dp）に配置し、外周は背景色で埋めた。これによりランチャーの円形・角丸マスクで万年筆が欠けない。
- アートワーク外周の黒い余白は、アルファマスクを作成して背景色 `#0B247A` と合成し、マスク形状が変わっても黒い角が出ないようにした。
- 前景は `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.png`、従来アイコン用に `ic_launcher.png` / `ic_launcher_round.png` も生成。Android 13 のテーマアイコン用モノクロームは、元アートワークの再現性を優先して提供しない。
- 実機 `127.0.0.1:5555` のアプリ情報画面とアプリ一覧で、円形マスク内に万年筆が表示されることを確認。開発版（Prompter Dev）で確認し、`v0.10.1` のリリースAPKにも同じリソースを含む。
