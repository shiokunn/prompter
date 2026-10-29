# キャラクター名の収集台帳

更新日: 2026-10-29（追加調査）

## このファイルについて

指定された10作品のキャラクターについて、日本語名と英語表記をDB化する前に記録する作業台帳。**収集途中であり、全キャラクターの一覧ではない。** ゲームの継続的な追加、NPC、コラボ等の収録範囲は今後確認する。

- 作品ごとに分けて同名キャラクターを区別する。通常キャラクターと別形態・別衣装・別人格・別ユニットは別レコードにし、同じ人物IDへまとめない。
- 下記の英語表記は出典に記載された名前であり、必ずしも英語版の権利者公式名称ではない。「二次資料」は公式名称確認前の候補として扱う。
- 「英語表記未確認」は空欄のままにし、推測によるローマ字変換を正解扱いしない。
- 公式・二次資料それぞれに適用される利用条件や再配布条件を確認してからDB・公開配布に使う。本文や画像をコピーしない。
- 本台帳はまだアプリやDBに接続されていない。DB化時には作品ID、レコードID、形態ID、別名、出典、確認状態を保持し、名前だけで自動的に統合しない。
- 別名は**同一形態の表記揺れ・実際に使われる呼称だけ**同一レコードの検索語として保存する。別形態・別衣装・別人格・別ユニットは必ず独立レコードにする。正規名・別名が複数候補に一致する場合は作品名・形態名付きで候補表示し、自動決定しない。

## 収集の完了条件と照合結果（2026-10-29）

完了条件は、次の3点をすべて満たすこととした。

1. 対象10作品それぞれについて、下記の参照一覧に載るプレイアブル／主要キャラクターを、日本語名・英語表記・出典・別名・別形態区分付きで記録する。
2. 参照一覧との行単位の照合を終え、一覧側の項目が台帳に無い状態を解消する。
3. 別形態・別衣装・別人格・別ユニットを独立レコードとして保持する。

| 作品 | 照合した一覧 | 台帳行数 | 照合結果 |
|---|---|---:|---|
| 干物妹!うまるちゃん | Wikipedia 英語版の登場人物節 | 10 | 記載人物を収録 |
| 崩壊：スターレイル | HoYoverse同好会の英語名一覧（2026-08-25更新） | 93 | 一覧の全キャラクターを収録 |
| 原神 | Genshin Dictionary のプレイアブル分類の対訳データ | 144 | プレイアブル分類を収録。NPCは対象外 |
| ゼンレスゾーンゼロ | HoYoverse同好会の英語名一覧（2026-08-25更新） | 58 | 一覧の全キャラクターを収録 |
| 鳴潮 | Wuthering.gg キャラクター一覧（全61表示） | 62 | 一覧の全項目を収録（漂泊者の属性形態を独立行で保持） |
| 勝利の女神：NIKKE | 有志wikiの海外名一覧＋Nikke.gg プレイアブル一覧＋2026年公式告知 | 145 | 3資料の和集合を収録。JP名未確認の新規ユニットは候補状態 |
| 学園アイドルマスター | 公式学園名簿（日英併記）＋Wikipedia | 15 | 名簿掲載の生徒・教員を収録 |
| マブラヴ ガールズガーデン | 公式サイト CHARACTER（日本語／英語版） | 42 | 公式掲載の全キャラクターを収録 |
| Re:ゼロから始める異世界生活 | Wikipedia 英語版の登場人物一覧 | 27 | 主要人物を収録 |
| この素晴らしい世界に祝福を！ | Wikipedia 英語版の登場人物一覧 | 25 | 主要人物を収録 |

原神のNPCはユーザーの指定により対象外。コラボキャラクターは作品横断の同名と区別するため、作品ID付きで保持する。行単位の照合で一覧側の未収録は解消したが、**英語名が英語版の公式ローカライズ名であること**、NIKKEの新規ユニットの**日本語公式表記**、および各資料の再配布条件は未確認のまま記録している。

## 干物妹!うまるちゃん

出典: [Wikipedia: Himouto! Umaru-chan — Characters](https://en.wikipedia.org/wiki/Himouto!_Umaru-chan#Characters)（二次資料、英語版公式表記は未確認）

| 日本語名 | 英語表記候補 | 状態・備考 |
|---|---|---|
| 土間埋 | Umaru Doma | 二次資料。うまる、UMR、こまるは別名 |
| 土間大平 | Taihei Doma | 二次資料 |
| 海老名菜々 | Nana Ebina | 二次資料 |
| 本場切絵 | Kirie Motoba | 二次資料 |
| 橘・シルフィンフォード | Sylphynford Tachibana | 二次資料 |
| 本場猛 | Takeshi Motoba | 二次資料。ぼんばは別名 |
| 橘・アレックス | Alex Tachibana | 二次資料 |
| 金剛叶 | Kanau Kongou | 二次資料 |
| 金剛ヒカリ | Hikari Kongō | 二次資料。英語表記の長音を要確認 |
| 海老名公一郎 | Kōichirō Ebina | 二次資料。英語表記の長音を要確認 |

## 崩壊：スターレイル

出典: [南山大学HoYoverse同好会 スタレキャラ英語名まとめ](https://nz-hoyobu.github.io/useful-info/hsrEngNames.html)（二次資料、2026-08-25更新）。人物の同一性・公式表記・NPCやコラボの網羅性は未確認。

| 日本語名 | 英語表記候補 | 別名・備考 |
|---|---|---|
| 姫子 | Himeko | — |
| ヴェルト | Welt | — |
| ブローニャ | Bronya | — |
| ジェパード | Gepard | — |
| クラーラ | Clara | — |
| 彦卿 | Yanqing | — |
| 白露 | Bailu | — |
| ゼーレ | Seele | — |
| 三月なのか | March 7th | なのか |
| 丹恒 | Dan Heng | — |
| アーラン | Arlan | — |
| アスター | Asta | — |
| ヘルタ | Herta | マダム・ヘルタとの関係要確認 |
| ナターシャ | Natasha | — |
| セーバル | Serval | — |
| ペラ | Pela | — |
| サンポ | Sampo | — |
| フック | Hook | — |
| 素裳 | Sushang | — |
| 青雀 | Qingque | — |
| 停雲 | Tingyun | 帰忘の流離人との関係要確認 |
| 景元 | Jing Yuan | — |
| 銀狼 | Silverwolf | — |
| 羅刹 | Luocha | — |
| 御空 | Yukong | — |
| 刃 | Blade | — |
| カフカ | Kafka | — |
| ルカ | Luka | — |
| 丹恒・飲月 | Dan Heng: Imbibitor Lunae | 丹恒との関係要確認 |
| 符玄 | Fu Xuan | — |
| リンクス | Lynx | — |
| 鏡流 | Jingliu | — |
| 桂乃芬 | Guinaifen | — |
| フォフォ | Huohuo | — |
| アルジェンティ | Argenti | 出典はアルジェンテイ表記、要確認 |
| 寒鴉 | Hanya | — |
| ルアン・メェイ | Ruan Mei | — |
| Dr.レイシオ | Dr. Ratio | — |
| 雪衣 | Xueyi | — |
| ブラックスワン | Black Swan | — |
| 花火 | Sparkle | — |
| ミーシャ | Misha | — |
| 黄泉 | Acheron | — |
| アベンチュリン | Aventurine | — |
| ギャラガー | Gallagher | — |
| ロビン | Robin | — |
| ブートヒル | Boothill | — |
| ホタル | Firefly | — |
| ジェイド | Jade | — |
| 雲璃 | Yunli | — |
| 椒丘 | Jiaoqiu | — |
| 飛霄 | Feixiao | — |
| 霊砂 | Lingsha | — |
| モゼ | Moze | — |
| 乱破 | Rappa | — |
| サンデー | Sunday | — |
| 帰忘の流離人 | Fugue | 停雲との関係要確認 |
| マダム・ヘルタ | The Herta | ヘルタとの関係要確認 |
| アグライア | Aglaea | — |
| トリビー | Tribbie | — |
| モーディス | Mydei | — |
| キャストリス | Castorice | — |
| アナイクス | Anaxa | — |
| ヒアンシー | Hyacine | — |
| サフェル | Cipher | — |
| ファイノン | Phainon | — |
| セイバー | Saber | コラボ・作品識別要確認 |
| アーチャー | Archer | コラボ・作品識別要確認 |
| セイレンス | Hysilens | — |
| ケリュドラ | Cerydra | — |
| 長夜月 | Evernight | 同一人物・別形態の関係要確認 |
| 丹恒・騰荒 | Dan Heng: Permansor Terrae | 丹恒との関係要確認 |
| キュレネ | Cyrene | — |
| ダリア | Dahlia | — |
| 爻光 | Yao Guang | — |
| 火花 | Sparxie | — |
| 不死途 | Ashveil | — |
| 緋英 | Evanescia | — |
| 遠坂凛 | Rin Tohsaka | コラボ・作品識別要確認 |
| ギルガメッシュ | Gilgamesh | コラボ・作品識別要確認 |
| 銀狼LV.999 | Silver Wolf LV.999 | 銀狼との関係要確認 |
| 千冶・刃 | Mortenax Blade | 刃との関係要確認 |
| 姫子・旅立ち | Himeko Nova | 姫子との関係要確認 |
| ロビン・夏空の歌 | Robin Summerto | ロビンとの関係要確認 |
| パール | Pearl | [GameWith一覧](https://gamewith.jp/houkaistarrail/article/show/387676)、二次資料 |
| 開拓者・星 | Stelle | 開拓者の女性形態として独立レコード |
| 開拓者・穹 | Caelus | 開拓者の男性形態として独立レコード |
| トパーズ | Topaz | Numby/カブとは別のキャラクター |
| カブ | Numby | トパーズとは別のキャラクター |
| 銀狼 LV.999 | Silver Wolf LV.999 | 銀狼とは別形態レコード。別名統合禁止 |
| 水着アベンチュリン | Aventurine • Waveflair | アベンチュリンとは別形態レコード。別名統合禁止 |
| 水着ロビン | Robin • Summeretto | ロビンとは別形態レコード。別名統合禁止 |
| 星神☆アッハ | Aeon Aha | 別キャラクターとして記録。公式名要確認 |

## 原神

出典: [原神 英語・中国語辞典](https://genshin-dictionary.com/ja/tags/character-main)（二次資料）。出典自体に別名・称号も掲載されているため、人物の自動統合はしない。配布条件の確認は未了。

| 日本語名 | 英語表記候補 | 別名・備考 |
|---|---|---|
| 空 | Aether | 男性主人公、旅人は曖昧 |
| 蛍 | Lumine | 女性主人公、旅人は曖昧 |
| 鍾離 | Zhongli | — |
| 閑雲 | Xianyun | 留雲借風真君（出典記載の俗名関係） |
| ベネット | Bennett | — |
| スカーク | Skirk | — |
| タルタリヤ | Tartaglia | 公子・アヤックス等は別レコードとして保持。統合禁止 |
| スカラマシュ | Scaramouche | 散兵・国崩等は別形態レコード。統合禁止 |
| 放浪者 | Wanderer | スカラマシュとは別形態レコード。別名統合禁止 |
| アルレッキーノ | Arlecchino | 召使・ペルヴェーレ等は別形態レコード。統合禁止 |
| 九条裟羅 | Kujou Sara | — |
| 辛炎 | Xinyan | — |
| 魈 | Xiao | — |
| ロサリア | Rosaria | — |
| イファ | Ifa | — |
| イネファ | Ineffa | — |
| ドゥリン | Durin | ちびドゥリンは別個体の可能性、要確認 |
| ラウマ | Lauma | — |
| ネフェル | Nefer | — |
| アイノ | Aino | — |
| ヤフォダ | Jahoda | — |
| ファルカ | Varka | — |
| フリンズ | Flins | Kyryll Chudomirovich Flins |
| リンネア | Linnea | — |
| コロンビーナ・ハイポセレニア | Columbina Hyposelenia | 少女・クータルは別レコードとして保持 |
| アリス | Alice | NIKKEのアリスと同名、作品で区別 |
| ニコ・リヤン | Nicole Reeyn | — |
| ウァサゴ | Vassaga | 人物・別名関係要確認 |
| レーレク | Lelek | — |
| オデット | Odette | — |
| サンドローネ | Sandrone | 傀儡・マリアネッテとの関係要確認 |
| 傀儡 | Marionette | サンドローネと別レコード。人物同一性は未確認 |
| クロックワークプリンセス | Clockwork Princess | 人物か称号か未確認 |
| マリアネッテ・ギヨタン | Marionette Guillotin | サンドローネとの関係要確認 |
| アナスターシャ・フョードロヴナ・スネージナヤ | Anastasya Feodorovna Snezhnaya | 別名関係要確認 |
| ヴォジャニーツァ | Vodyanitsa | — |
| アリョーシャ | Alyosha | — |
| バレリー | Valeriy | — |
| ヴェスナ | Vesna | — |
| ダニカ | Danica | — |
| ノイ | Noy | — |
| ミティヤ | Mitya | — |
| プルーネ | Prune | — |
| ディオナ・カッツェレイン | Diona Kätzlein | ディオナ |
| ローエン | Lohen | — |
| 茲白 | Zibai | しはく、白馬の仙人との関係要確認 |
| イルーガ | Illuga | — |
| ちびドゥリン | Mini Durin | ドゥリンと別個体の可能性 |
| バーバラ・ペッチ / バーバラ・ペイジ | Barbara Pegg | バーバラ。日本語表記揺れ |
| 白朮 | Baizhu | びゃくじゅつ |
| エウルア・ローレンス | Eula Lawrence | エウルア |
| ダリア | Dahlia | — |
| シャルロット | Charlotte | — |
| リネ | Lyney | — |
| リネット | Lynette | — |
| フレミネ | Freminet | — |
| ナヴィア | Navia | — |
| リオセスリ | Wriothesley | — |
| シグウィン | Sigewinne | — |
| クロリンデ | Clorinde | — |
| ヌヴィレット | Neuvillette | — |
| フリーナ | Furina | — |
| シュヴルーズ | Chevreuse | — |
| 千織 | Chiori | ちおり |
| エミリエ | Emilie | — |
| エスコフィエ | Escoffier | — |
| 楓原万葉 | Kaedehara Kazuha | 万葉 |
| 宵宮 | Yoimiya | よいみや |
| 神里綾華 | Kamisato Ayaka | 綾華 |
| 神里綾人 | Kamisato Ayato | 綾人 |
| トーマ | Thoma | — |
| 早柚 | Sayu | さゆ |
| 八重神子 | Yae Miko | — |
| 荒瀧一斗 | Arataki Itto | 一斗 |
| 久岐忍 | Kuki Shinobu | 忍 |
| ゴロー | Gorou | — |
| 珊瑚宮心海 | Sangonomiya Kokomi | 心海 |
| 鹿野院平蔵 | Shikanoin Heizou | 平蔵 |
| 綺良々 | Kirara | きらら |
| 雷電将軍 | Raiden Shogun | 影との関係要確認 |
| 影 | Ei | 雷電将軍との関係要確認 |
| 夢見月瑞希 | Yumemizuki Mizuki | 瑞希 |
| 香菱 | Xiangling | しゃんりん |
| 刻晴 | Keqing | こくせい |
| 凝光 | Ningguang | ぎょうこう |
| 七七 | Qiqi | なな |
| 行秋 | Xingqiu | ゆくあき |
| 重雲 | Chongyun | ちょううん |
| 北斗 | Beidou | ほくと |
| 甘雨 | Ganyu | かんう |
| 胡桃 | Hu Tao | ふーたお |
| 煙緋 | Yanfei | えんひ |
| 雲菫 | Yun Jin | うんきん |
| 申鶴 | Shenhe | しんかく |
| 夜蘭 | Yelan | いぇらん |
| ヨォーヨ | Yaoyao | — |
| 嘉明 | Gaming | がみん |
| 藍硯 | Lan Yan | らんやん |
| ジン・グンヒルド | Jean Gunnhildr | ジン |
| アンバー | Amber | — |
| ガイア・アルベリヒ | Kaeya Alberich | ガイア |
| リサ・ミンツ | Lisa Minci | リサ |
| ウェンティ | Venti | — |
| レザー | Razor | — |
| ノエル | Noelle | — |
| フィッシュル・ヴォン・ルフシュロス・ナフィードット | Fischl von Luftschloss Narfidort | フィッシュル |
| ディルック・ラグヴィンド | Diluc Ragnvindr | ディルック |
| クレー | Klee | — |
| スクロース | Sucrose | — |
| アルベド | Albedo | — |
| アストローギスト・モナ・メギストス | Astrologist Mona Megistus | モナ |
| ミカ・シュミット | Mika Schmidt | ミカ |
| ムアラニ | Mualani | — |
| カチーナ | Kachina | — |
| キィニチ | Kinich | — |
| クフル・アハウ | K'uhul Ajaw | — |
| アハウ | Almighty Dragonlord (Self-Proclaimed) | クフル・アハウと別レコード。呼称・同一性要確認 |
| シトラリ | Citlali | 黒曜石の老婆との関係要確認 |
| 黒曜石の老婆 | Granny Itztli | シトラリと別レコード。呼称・同一性要確認 |
| シロネン | Xilonen | — |
| イアンサ | Iansan | — |
| チャスカ | Chasca | — |
| マーヴィカ | Mavuika | — |
| オロルン | Ororon | — |
| ヴァレサ | Varesa | — |
| セノ | Cyno | — |
| ティナリ | Tighnari | — |
| コレイ | Collei | — |
| ドリー・サングマハベイ | Dori Sangemah Bay | ドリー |
| アルハイゼン | Alhaitham | — |
| ディシア | Dehya | — |
| ニィロウ | Nilou | — |
| キャンディス | Candace | — |
| レイラ | Layla | — |
| ファルザン | Faruzan | — |
| カーヴェ | Kaveh | — |
| ナヒーダ | Nahida | — |
| セトス | Sethos | — |
| 公子 | Childe | タルタリヤとは別レコード。別形態統合禁止 |
| 散兵 | Balladeer | スカラマシュとは別レコード。別形態統合禁止 |
| 国崩 | Kunikuzushi | スカラマシュとは別レコード。別形態統合禁止 |
| 召使 | The Knave | アルレッキーノとは別レコード。別形態統合禁止 |
| 少女 | Damselette | コロンビーナとは別レコード。別形態統合禁止 |
| クータル | Kuutar | 月の少女とは別レコード。人物同一性未確認 |

原神の公開データは [利用条件](https://genshin-dictionary.com/ja/opendata) に再配布の条件付き許可がある。第三者の権利・ゲーム規約等に抵触する場合などは許可が取り消される旨があり、アプリへの配布可否は別途確認する。**原神のNPCはユーザーの指定により対象外。** プレイアブル分類には称号・別名や人物以外の項目も混ざり得るため、件数をそのまま人物数とみなさない。

## ゼンレスゾーンゼロ

出典: [南山大学HoYoverse同好会 ゼンゼロキャラ英語名まとめ](https://nz-hoyobu.github.io/useful-info/zzzEngNames.html)（二次資料、2026-08-25更新）。日本語短名と英語フルネームの組合せは候補として扱う。

| 日本語名 | 英語表記候補 | 別名・備考 |
|---|---|---|
| ニコ | Nicole Demara | — |
| アンビー | Anby Demara | 0号・アンビーとの関係要確認 |
| ビリー | Billy Kid | — |
| アンドー | Anton Ivanov | — |
| ベン | Ben Bigger | — |
| 蒼角 | Soukaku | — |
| カリン | Corin Wickes | — |
| ルーシー | Lucy | Luciana de Montefio |
| パイパー | Piper Wheel | — |
| セス | Seth Lowell | — |
| 猫又 | Nekomata | — |
| クレタ | Koleda Belobog | — |
| グレース | Grace Howard | — |
| 11号 | Soldier 11 | — |
| ライカン | Von Lycaon | — |
| リナ | Rina | Alexandrina Sebastiane |
| エレン | Ellen Joe | — |
| 朱鳶 | Zhu Yuan | — |
| 雅 | Hoshimi Miyabi | — |
| 青衣 | Qingyi | — |
| ジェーン | Jane Doe | — |
| 悠真 | Asaba Harumasa | — |
| シーザー | Caesar King | — |
| バーニス | Burnice White | — |
| 柳 | Tsukishiro Yanagi | — |
| ライト | Lighter | — |
| アストラ | Astra Yao | — |
| イヴリン | Evelyn Chevalier | — |
| 0号・アンビー | Soldier 0 - Anby | アンビーとの関係要確認 |
| プルクラ | Pulchra Fellini | — |
| トリガー | Trigger | — |
| ビビアン | Vivian Banshee | — |
| ヒューゴ | Hugo Vlad | — |
| 儀玄 | Yixuan | — |
| 潘引壺 | Pan Yinhu | — |
| 橘福福 | Ju Fufu | — |
| 柚葉 | Ukinami Yuzuha | — |
| アリス | Alice Thymefield | NIKKE・原神と同名注意 |
| シード | Seed | — |
| リュシア | Lucia Elowen | — |
| イドリー | Yidhali Murphy | — |
| ダイアリン | Dialyn | — |
| 盤岳 | Banyue | — |
| 照 | Zhao | — |
| 葉瞬光 | Ye Shunguang | — |
| 千夏 | Sunna | — |
| アリア | Aria | — |
| 南宮羽 | Nangong Yu | — |
| シーシィア | Cissia | — |
| プロメイア | Promeia | — |
| プロキシ・ワイズ | Proxy Wise | 主人公選択肢の一方。ベルと別レコード |
| プロキシ・ベル | Proxy Belle | 主人公選択肢の一方。ワイズと別レコード |
| オルペウス & 鬼火 | Orphie & Magus | 個別の人物・別名の区別要確認 |
| スターライト・ビリー | Starlight - Billy Kid | ビリーとの関係要確認 |
| ヴェリナ | Velina Airgid | — |
| ピュロイス | Pyrois | — |
| レミエール | Remielle Dan | — |
| シグリッド | Sigrid de L'Azur | — |

## 鳴潮

「めいちょう」は『鳴潮』として扱う。出典: [鳴潮 Wiki 用語の翻訳](https://wikiwiki.jp/w-w/%E7%94%A8%E8%AA%9E%E3%81%AE%E7%BF%BB%E8%A8%B3)（二次資料、2026-10-28更新）および [Wuthering.gg キャラクター一覧](https://wuthering.gg/ja/characters)（二次資料、閲覧時「全61キャラクター」と表示）。括弧内は出典に記載の読み。台帳行数と一覧表示数の差分確認中。公式日本語・英語表記とNPCの網羅性は未確認。

| 日本語名 | 英語表記候補 | 別名・備考 |
|---|---|---|
| 漂泊者 | Rover | 男女の区別要確認 |
| 漂泊者・回折 | Rover: Spectro | 漂泊者とは別の属性形態レコード。別名統合禁止 |
| 漂泊者・消滅 | Rover: Havoc | 漂泊者とは別の属性形態レコード。別名統合禁止 |
| 漂泊者・気動 | Rover: Aero | 漂泊者とは別の属性形態レコード。別名統合禁止 |
| 漂泊者・電導 | Rover: Electro | 同じ属性で一覧に重複表示あり。重複レコードか要照合 |
| 凌陽 | Lingyang | リンヨウ |
| 散華 | Sanhua | サンカ |
| 白芷 | Baizhi | ビャクシ |
| 折枝 | Zhezhi | オリエ |
| 釉瑚 | Youhu | ユウゴ |
| カルロッタ | Carlotta | — |
| 緋雪 | Hiyuki | ヒユキ |
| ルシラー | Lucilla | — |
| 穂穂 | Suisui | スイスイ |
| アンコ | Encore | — |
| 熾霞 | Chixia | シカ |
| モルトフィー | Mortefi | — |
| 長離 | Changli | チョウリ |
| ブラント | Brant | — |
| ルパ | Lupa | — |
| ガルブレーナ | Galbrena | — |
| モーニエ | Mornye | — |
| エイメス | Aemeath | — |
| ダーニャ | Denia | — |
| 景燃 | Jingran | ジンラン |
| カカロ | Calcharo | — |
| 淵武 | Yuanwu | エンブ |
| 吟霖 | Yinlin | インリン |
| 相里要 | Xiangli Yao | ソウリヨウ |
| 灯灯 | Lumi | トウトウ |
| オーガスタ | Augusta | — |
| 卜霊 | Buling | ボクレイ |
| 鑑心 | Jianxin | カンシン |
| 秧秧 | Yangyang | ヤンヤン |
| アールト | Aalto | — |
| 忌炎 | Jiyan | キエン |
| シャコンヌ | Ciaccona | — |
| カルテジア | Cartethyia | — |
| ユーノ | Iuno | — |
| 仇遠 | Qiuyuan | キュウエン |
| シグリカ | Sigrika | — |
| 清宵 | Qingxiao | セイショウ |
| ヴェリーナ | Verina | — |
| 今汐 | Jinhsi | コンシ |
| ショアキーパー | The Shorekeeper | — |
| フィービー | Phoebe | — |
| ザンニー | Zani | — |
| リンネー | Lynae | — |
| リューク・ヘルセン | Luuk Herssen | — |
| 桃祈 | Taoqi | タオキ |
| 丹瑾 | Danjin | タンキン |
| ツバキ | Camellya | — |
| ロココ | Roccia | — |
| カンタレラ | Cantarella | — |
| フローヴァ | Phrolova | — |
| 千咲 | Chisa | — |
| 秧秧・玄翎 | Yangyang:Xuanling | 秧秧との関係要確認 |
| レベッカ | Rebecca | — |
| 鎖暝 | Suoming | サメイ。実装状態要確認 |
| 心 | Hsin | シン。実装状態要確認 |
| ルーシー | Lucy | — |
| リリー | Lily | 実装状態要確認 |

## 勝利の女神：NIKKE

出典: [有志NIKKE wiki キャラクター海外名一覧](https://wiki3.jp/nikke/page/236)（二次資料）および [Nikke.gg playable roster](https://nikke.gg/characters/)（二次資料、2026-10-29閲覧）。海外名一覧は更新が不十分な可能性がある。新規ユニット名はNikke.ggの英語掲載を転記し、日本語表記は検索用候補として作成したもので公式日本語名の確認が必要。シンデレラは[日本語公式](https://nikke-jp.com/events/cinderellapoll/)・[英語公式](https://nikke-en.com/events/cinderellapoll/)を参照。非プレイアブルNPCの範囲は未確定。

| 日本語名 | 英語表記候補 | 別名・備考 |
|---|---|---|
| シンデレラ | Cinderella | 日英公式表記 |
| マリアン：クイーン | Marian: Queen | マリアンとは別形態レコード。公式告知表記 |
| 紅蓮：神殺 | Scarlet: Goddeath | 紅蓮とは別形態レコード。公式告知表記 |
| ラプンツェル：サンクチュアリ | Rapunzel: Sanctuary | ラプンツェルとは別形態レコード。公式告知表記 |
| ベロータ：パンプキンウィッチ | Belorta: Pumpkin Witch | ベロータとは別ユニット。別レコード |
| シン：スウィフトバニー | Sin: Swift Bunny | シンとは別ユニット。別レコード |
| ギルティ：マイティバニー | Guilty: Mighty Bunny | ギルティとは別ユニット。別レコード |
| ドレイク：グレートヴィラン | Drake: Great Villain | ドレイクとは別ユニット。別レコード |
| マクスウェル：オーディナリーメカニック | Maxwell: Ordinary Mechanic | マクスウェルとは別ユニット。別レコード |
| ラプラス：アルティメットヒーロー | Laplace: Ultimate Hero | ラプラスとは別ユニット。別レコード |
| マルチャーナ：マリンスチューデント | Marciana: Marine Study | マルチャーナとは別ユニット。別レコード |
| シンデレラ：クリスタルウェーブ | Cinderella: Crystal Wave | シンデレラとは別ユニット。別レコード |
| アークレンジャー・ブラック | Ark Ranger Black | アークレンジャーズの人物、独立レコード |
| プリカ | Prika | T.T. STAR |
| ミント | Mint | T.T. STAR |
| ネオン：ビジョンアイ | Neon: Vision Eye | ネオンとは別ユニット。別レコード |
| アニス：スター | Anis: Star | アニスとは別ユニット。別レコード |
| アビスター | Avistar | Overseer |
| スノークレーン | Snow Crane | Veiled Order |
| アルカナ：フォーチュンメイト | Arcana: Fortune Mate | アルカナとは別ユニット。別レコード |
| E.H. | E.H. | Exotic |
| タキナ | Takina | コラボ。Lycoris Recoil |
| チサト | Chisato | コラボ。Lycoris Recoil |
| クルミ | Kurumi | コラボ。Lycoris Recoil |
| ベルベット | Velvet | Maid For You |
| ラベル | Label | Best Seller |
| スノーホワイト：ヘビーアームズ | Snow White: Heavy Arms | スノーホワイトとは別ユニット。別レコード |
| ブリッド：サイレントトラック | Brid: Silent Track | ブリッドとは別ユニット。別レコード |
| ディーゼル：ウィンタースイーツ | Diesel: Winter Sweets | ディーゼルとは別ユニット。別レコード |
| ソリン：フロストチケット | Soline: Frost Ticket | ソリンとは別ユニット。別レコード |
| リベラリオ | Liberalio | ヘレティック |
| ナユタ | Nayuta | Pioneer |
| チャイム | Chime | White Knight |
| デルタ：ニンジャシーフ | Delta: Ninja Thief | デルタとは別ユニット。別レコード |
| ジル | Jill | コラボ。VA-11 Hall-A |
| エイダ | Ada | コラボ。Resident Evil |
| クレア | Claire | コラボ。Resident Evil |
| ミルク：ブルーミングバニー | Milk: Blooming Bunny | ミルクとは別ユニット。別レコード |
| エード：エージェントバニー | Ade: Agent Bunny | エードとは別ユニット。別レコード |
| ベスティー：タクティカルアップグレード | Vesti: Tactical Upgrade | ベスティーとは別ユニット。別レコード |
| ウンファ：タクティカルアップグレード | Eunhwa: Tactical Upgrade | ウンファとは別ユニット。別レコード |
| エマ：タクティカルアップグレード | Emma: Tactical Upgrade | エマとは別ユニット。別レコード |
| エレグ：ブームアンドショック | Elegg: Boom and Shock | エレグとは別ユニット。別レコード |
| ドロシー：セレンディピティ | Dorothy: Serendipity | ドロシーとは別ユニット。別レコード |
| ソラ | Sora | Over the Horizon |
| レイヴン | Raven | 2nd Airborne Squad |
| イヴ | EVE | 7th Airborne Squad |
| リリー | Lily | 5th Airborne Squad |
| K | K | Perilous Siege |
| アルカナ | Arcana | Best Seller。別ユニット形態とは別レコード |
| ミハラ：ボンディングチェーン | Mihara: Bonding Chain | ミハラとは別ユニット。別レコード |
| アイギス | Aigis | コラボ。Persona 3 |
| ラピ | Rapi | — |
| アニス | Anis | マブガルのAnis Bennettと区別 |
| ネオン | Neon | — |
| エマ | Emma | — |
| プリバティ | Privaty | — |
| シグナル | Signal | — |
| ポリ | Poli | — |
| ミランダ | Miranda | — |
| ブリッド | Brid | — |
| ソリン | Soline | — |
| ディーゼル | Diesel | — |
| ベスティー | Vesti | — |
| ウンファ | Eunhwa | — |
| ギロチン | Guillotine | — |
| メイデン | Maiden | — |
| D | D | 短い別名は誤ヒット注意 |
| ヘルム | Helm | — |
| マスト | Mast | — |
| マルチャーナ | Marciana | — |
| キリ | Quiry | — |
| ツバイ | Zwei | — |
| マクスウェル | Maxwell | — |
| ユニ | Yuni | — |
| リター | Liter | — |
| ジュリア | Julia | — |
| センチ | Centi | — |
| ドレイク | Drake | — |
| クロウ | Crow | — |
| ペッパー | Pepper | — |
| アドミ | Admi | — |
| ジャッカル | Jackal | — |
| ラプラス | Laplace | — |
| ギルティ | Guilty | — |
| シン | Sin | — |
| クエンシー | Quency | — |
| トーブ | Tove | — |
| エピネル | Epinel | — |
| ナガ | Naga | — |
| ティア | Tia | — |
| エレグ | Elegg | — |
| キロ | Kilo | — |
| トロニー | Trony | — |
| アイン | Ein | — |
| シュガー | Sugar | — |
| エクシア | Exia | — |
| アリス | Alice | 原神・ゼンゼロと同名注意 |
| ブラン | Blanc | — |
| ノワール | Noir | — |
| プリム | Frima | — |
| メアリー | Mary | — |
| ミルク | Milk | — |
| ユルハ | Yulha | — |
| ルドミラ | Ludmilla | — |
| ルピー | Rupee | — |
| ヤン | Yan | — |
| ドラー | Dolla | — |
| ノベル | Novel | — |
| フォルクヴァン | Folkwang | — |
| サクラ | Sakura | — |
| バイパー | Viper | — |
| ココア | Cocoa | — |
| ソーダ | Soda | — |
| ビスケット | Biscuit | — |
| ライ | Rei | — |
| アリア | Aria | — |
| ノイズ | Noise | — |
| ボリューム | Volume | — |
| ロザンナ | Rosanna | — |
| ネロ | Nero | — |
| レオナ | Leona | — |
| モラン | Moran | — |
| エード | Ade | — |
| ベイ | Bay | — |
| クレイ | Clay | — |
| スノーホワイト | Snow White | — |
| イサベル | Isabel | — |
| ラプンツェル | Rapunzel | — |
| 紅蓮 | Scarlet | — |
| ハラン | Harran | — |
| ノア | Noah | — |
| モダニア | Modernia | マリアンとの関係要確認 |
| ニヒリスター | Nihilister | — |
| ドロシー | Dorothy | — |
| レッドフード | Red Hood | — |
| クラウン | Crown | — |
| デルタ | Delta | — |
| ミハラ | Mihara | — |
| N102 | N102 | エヌとの関係要確認 |
| エーテル | Ether | — |
| ベロータ | Belorta | — |
| ミカ | Mica | — |
| ネヴェ | Neve | — |
| アンカー | Anchor | — |

## 学園アイドルマスター

出典: [公式 学園名簿](https://gakuen.idolmaster-official.jp/idol/sena/)の日英併記、および [Wikipedia: Gakuen Idolmaster — Characters](https://en.wikipedia.org/wiki/Gakuen_Idolmaster#Characters)（二次資料）。公式サイト上のローマ字表記であり英語版正式ローカライズ名とは未確認。

| 日本語名 | 英語表記候補 | 状態・備考 |
|---|---|---|
| 花海咲季 | Saki Hanami | 二次資料 |
| 月村手毬 | Temari Tsukimura | 二次資料 |
| 藤田ことね | Kotone Fujita | 二次資料 |
| 姫崎莉波 | Rinami Himesaki | 二次資料 |
| 紫雲清夏 | Sumika Shiun | 二次資料 |
| 篠澤広 | Hiro Shinosawa | 二次資料 |
| 葛城リーリヤ | Lilja Katsuragi | 二次資料 |
| 倉本千奈 | China Kuramoto | 二次資料 |
| 有村麻央 | Mao Arimura | 二次資料 |
| 花海佑芽 | Ume Hanami | 二次資料 |
| 秦谷美鈴 | Misuzu Hataya | 二次資料 |
| 十王星南 | Sena Juo | 公式サイト表記 |
| 雨夜燕 | Tsubame Amaya | 公式サイト表記 |
| 十王邦夫 | Kunio Juo | 公式サイト表記、教員 |
| 根緒亜紗里 | Asari Neo | 公式サイト表記、教員 |

## マブラヴ ガールズガーデン

「マブガル」は[公式サイト](https://muvluv-girls-garden.com/)の表記に基づき本作品として扱う。以下は同公式サイトのCHARACTER欄で日本語とローマ字を併記している人物。英語版正式名称の有無と、サイト未掲載人物・コラボを含む全体の網羅性は未確認。

| 日本語名 | 英語表記候補 | 状態・備考 |
|---|---|---|
| 桃園める | Meru Momozono | 公式サイト表記 |
| 鳴滝七彩 | Nanairo Narutaki | 公式サイト表記 |
| 月ヶ瀬ちゆる | Chiyuru Tsukigase | 公式サイト表記 |
| 朽葉ラミ | Rami Kuchiba | 公式サイト表記 |
| 一条白奈 | Shirona Ichijo | 公式サイト表記 |
| 生駒葵 | Aoi Ikoma | 公式サイト表記 |
| 波瀬うるう | Uruu Namise | 公式サイト表記 |
| 姫川泉花 | Senka Himekawa | 公式サイト表記 |
| 綺羅クララ | Kurara Kira | 公式サイト表記 |
| 夕凪舞亜 | Maia Yunagi | 公式サイト表記 |
| ユリア・バーンズ | Julia Barnes | 公式サイト表記 |
| 劉翠蘭 | Liu Cuilan | 公式サイト表記 |
| 紫雲沙耶 | Saya Shiun | 公式サイト表記 |
| フィー・ドレーゼ | Fee Drese | 公式サイト表記 |
| アニス・ベネット | Anis Bennett | 公式サイト表記 |
| 榊野ヒイロ | Hiiro Sakakino | 公式サイト表記 |
| フルート・メルヴィル | Flute Melville | 公式サイト表記 |
| エヴィ・レーナルト | Evi Leenhardt | 公式サイト表記 |
| シエナ・クラーク | Sienna Clark | 公式サイト表記 |
| レイラ・ジェンキンス | Layla Jenkins | 公式サイト表記 |
| ロージー・ヒューズ | Rosie Hughes | 公式サイト表記 |
| ドロテア・カークランド | Dorothea Kirkland | 公式サイト表記 |
| ミリアム・ヘイワード | Miriam Hayward | 公式サイト表記 |
| ハリエット・ミルズ | Harriet Mills | 公式サイト表記 |
| ケイト・フルニエ | Kate Fournier | 公式サイト表記 |
| リュシー・ムーアクロフト | Lucie Moorcroft | 公式サイト表記 |
| ジュリー・ステイシー | Julie Stacy | 公式サイト表記 |
| 黒森ラウラ | Laura Kuromori | 公式サイト表記 |
| 樋向心香 | Shinka Himukai | 公式サイト表記 |
| ノエル・アルエ | Noelle Arouet | 公式サイト表記 |
| レイヴェル・ブライトリーフ | Reivel Brightleaf | 公式サイト表記 |
| ルナ・メロウ | Luna Mellow | 公式サイト表記 |
| リディア・エルドリッジ | Lydia Eldridge | 公式サイト表記 |
| リリー・ラヴォア | Lilly Lavoix | 公式サイト表記 |
| 姜小花 | Jiang Xiaohua | 公式サイト表記 |
| 大賀真桜 | Mao Taiga | 公式サイト表記 |
| カリナ・ジェンティーレ | Carina Gentile | 公式サイト表記 |
| ナージャ・ヴォルコワ | Nadja Volkova | 公式サイト表記 |
| オルガ・ヴォルコワ | Olga Volkova | 公式サイト表記 |
| エレーナ・パステルコワ | Elena Pastelkova | 公式サイト表記 |
| タチアナ・ドロズドヴァ | Tatyana Drozdova | 公式サイト表記 |
| ニーナ／ジーナ・ミーシナ | Nina/Gina Mishina | 公式サイト表記。人格の別名としての表現はDB設計時に確認 |

## Re:ゼロから始める異世界生活

出典: [Wikipedia: List of Re:Zero characters](https://en.wikipedia.org/wiki/List_of_Re:Zero_characters)（二次資料、英語版公式表記は未確認）

| 日本語名 | 英語表記候補 | 状態・備考 |
|---|---|---|
| ナツキ・スバル | Subaru Natsuki | 二次資料 |
| エミリア | Emilia | 二次資料 |
| パック | Puck | 二次資料 |
| ロズワール・L・メイザース | Roswaal L. Mathers | 二次資料 |
| レム | Rem | 二次資料 |
| ラム | Ram | 二次資料 |
| ベアトリス | Beatrice | 二次資料 |
| ガーフィール・ティンゼル | Garfiel Tinzel | 二次資料 |
| フレデリカ・バウマン | Frederica Baumann | 二次資料 |
| パトラッシュ | Patrasche | 二次資料 |
| オットー・スーウェン | Otto Suwen | 二次資料 |
| ペトラ・レイテ | Petra Leyte | 二次資料 |
| フェルト | Felt | 二次資料 |
| プリシラ・バーリエル | Priscilla Barielle | 二次資料 |
| アナスタシア・ホーシン | Anastasia Hoshin | 二次資料 |
| クルシュ・カルステン | Crusch Karsten | 二次資料 |
| ラインハルト・ヴァン・アストレア | Reinhard van Astrea | 二次資料 |
| フェリックス・アーガイル | Felix Argyle | 二次資料。フェリスは別名 |
| ユリウス・ユークリウス | Julius Juukulius | 二次資料 |
| アルデバラン | Aldebaran | 二次資料。アルは別名 |
| サテラ | Satella | 二次資料 |
| エキドナ | Echidna | 二次資料 |
| ミネルヴァ | Minerva | 二次資料 |
| カーミラ | Carmilla | 二次資料 |
| ダフネ | Daphne | 二次資料 |
| テュフォン | Typhon | 二次資料 |
| セクメト | Sekhmet | 二次資料 |

## この素晴らしい世界に祝福を！

出典: [Wikipedia: List of KonoSuba characters](https://en.wikipedia.org/wiki/List_of_KonoSuba_characters)（二次資料、英語版公式表記は未確認）

| 日本語名 | 英語表記候補 | 状態・備考 |
|---|---|---|
| 佐藤和真 | Kazuma Satou | 二次資料 |
| アクア | Aqua | 二次資料 |
| めぐみん | Megumin | 二次資料 |
| ダクネス | Darkness | 二次資料 |
| ベルディア | Verdia | 二次資料 |
| バニル | Vanir | 二次資料 |
| ハンス | Hans | 二次資料 |
| シルヴィア | Sylvia | 二次資料 |
| ウォルバク | Wolbach | 二次資料 |
| ウィズ | Wiz | 二次資料 |
| ゆんゆん | Yunyun | 二次資料 |
| 御剣響夜 | Kyouya Mitsurugi | 二次資料 |
| ルナ | Luna | 二次資料 |
| クリス | Chris | 二次資料。エリスとの関係は要確認 |
| エリス | Eris | 二次資料 |
| ダスト | Dust | 二次資料 |
| セナ | Sena | 二次資料 |
| アイリス | Iris | 二次資料 |
| こめっこ | Komekko | 二次資料 |
| あるえ | Arue | 二次資料 |
| ふにふら | Funifura | 二次資料 |
| どどんこ | Dodonko | 二次資料 |
| ねりまき | Nerimaki | 二次資料 |
| ちょむすけ | Chomusuke | 二次資料 |
| ぷっちん | Pucchin | 二次資料 |
