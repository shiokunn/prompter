# 戻るボタン削除とプロンプト→PixAI画像確認

- 日付: 2026-10-30
- 対象コミット: `04aadcd`（設定・履歴のヘッダー戻るボタン削除）
- CI: [41065666386](https://github.com/shiokunn/prompter/actions/runs/41065666386) 成功（M1、Kotlinユニットテスト、debug APK）
- 実機: `127.0.0.1:5555`（arrows Alpha）。`emulator-5554` は操作していない。

## UI変更

- `ScreenHeader` から `onBack` 引数と「戻る」ボタンを削除した。
- 設定・履歴は既存の `BackHandler` を残し、Androidのシステム戻る操作でメイン画面へ戻る。
- 設定の未保存変更がある場合の破棄確認は `leave()` 経由で従来どおり動作する。
- `ScreenInteractionTest` は設定画面に「戻る」が存在しないことと、保存操作が維持されることを確認する形に更新した。

### 実機確認

- 設定を開いた状態でヘッダーに「戻る」が表示されないこと、システム戻るでメイン画面へ戻ることを確認。
- 履歴を開いた状態でヘッダーに「戻る」が表示されないこと、システム戻るでメイン画面へ戻ることを確認。
- どちらの操作でもPrompterのクラッシュやアプリ例外は観測されなかった。

## プロンプト生成と画像確認

入力した情景:

```text
銀髪の少女が、赤いドレスで、夜の街の木の椅子に座っている
```

アプリが生成したプロンプト:

```text
An anime-style illustration of a solo girl with grey hair. The character is wearing a red dress. The character is sitting. The scene is set on a chair, in a city, and at night. The image uses high-quality anime coloring, clean linework, and detailed shading, giving it a polished, high-quality finish. Additional visual details: tree. Additional context from the input: Of / in / / / / / /. The composition keeps the subject as the clear focal point. Soft lighting and careful shading bring out the details of the hair, face, and clothing.
```

このプロンプトをアプリの「コピー」からクリップボードへ入れ、PixAI Android `4.25.7`（`io.mewtant.pixaiart`）の「テキスト/画像から画像へ」に貼り付けて生成した。生成時間は約2分、消費クレジットは4,400。

### 判定

生成画像は以下を満たしており、プロンプトの主要内容と一致した。

- 銀髪（グレー寄り）の少女
- 赤いドレス
- 椅子に座った姿勢
- 夜の街
- 木の存在
- アニメ調の描画

画像の再生成は不要と判断した。

### 見つかったプロンプト側の問題

1. `木の椅子` が `wooden chair` ではなく `chair` と `tree` に分解され、画像に意図しない大きな木が入った。入力の複合名詞を保つ必要がある。
2. `Additional context from the input: Of / in / / / / / /.` という助詞だけの不自然な文が残った。画像への影響は小さかったが、プロンプト品質上のノイズである。

どちらも今回の指示ではコード修正していない。次回修正する場合は、複合名詞（`木の椅子` など）の辞書処理と、助詞のみの未一致コンテキスト除去が候補になる。
