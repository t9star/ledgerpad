# Google Play Console アプリ内アイテム（Pro版）設定ガイド

このドキュメントには、Google Play Console の「アプリ内アイテム（In-app products）」に登録するアイテム情報および、全8言語分の翻訳テキスト一式が記載されています。

---

## 1. 基本設定（アイテム作成時）

| 項目 | 設定値 | 備考 |
|:---|:---|:---|
| **商品 ID (Product ID)** | **`ledgerpad_remove_ads`** | ⚠️ **アプリ内コードと完全一致必須** |
| **デフォルト言語** | 英語 (en-US) | デフォルトとして登録 |
| **デフォルト商品名** | `Remove Ads Forever` | 55文字以内 |
| **デフォルト説明** | `Enjoy a completely ad-free experience with a single one-time purchase.` | 80文字以内 |
| **ステータス** | **有効 (Active)** | 保存後に「アイテムを有効化」を押す |
| **価格** | **$1.99**（日本円なら約300円） | 「為替レートを適用」で各国通貨に自動換算 |
| **登録用アイコン** | [`store_assets/inapp_product_icon_512.png`](./inapp_product_icon_512.png) | 512×512 PNG（ゴールド＆エメラルド盾） |

---

## 2. 全8言語分の名前と説明（「翻訳を管理」で登録）

Google Play Console のアイテム詳細画面で **「翻訳を管理」 ＞ 「独自の翻訳テキストを追加」** を選択し、言語ごとに入力してください。

### ① 英語 (en-US) - デフォルト
- **名前 (Name)**:
  `Remove Ads Forever`
- **説明 (Description)**:
  `Enjoy a completely ad-free experience with a single one-time purchase.`

---

### ② 日本語 (ja-JP)
- **名前 (Name)**:
  `広告を非表示（買い切り）`
- **説明 (Description)**:
  `1回のお支払いでずっと広告なし。快適に帳簿管理をご利用いただけます。`

---

### ③ ヒンディー語 (hi-IN)
- **名前 (Name)**:
  `विज्ञापन हटाएं (लाइफटाइम)`
- **説明 (Description)**:
  `एक बार खरीदें और हमेशा के लिए बिना किसी विज्ञापन के बहीखाता चलाएं।`

---

### ④ ポルトガル語 (pt-BR)
- **名前 (Name)**:
  `Remover Anúncios (Vitalício)`
- **説明 (Description)**:
  `Aproveite uma experiência 100% sem anúncios com uma compra única.`

---

### ⑤ インドネシア語 (id)
- **名前 (Name)**:
  `Hapus Iklan (Selamanya)`
- **説明 (Description)**:
  `Nikmati buku kas tanpa iklan selamanya hanya dengan sekali bayar.`

---

### ⑥ フィリピン語 / タガログ語 (fil)
- **名前 (Name)**:
  `Alisin ang Ads (Panghabambuhay)`
- **説明 (Description)**:
  `Gamitin ang app nang walang anumang ads sa isang bayad lamang.`

---

### ⑦ スペイン語 (es-419)
- **名前 (Name)**:
  `Eliminar Anuncios (De por vida)`
- **説明 (Description)**:
  `Disfruta de una experiencia sin anuncios con un único pago para siempre.`

---

### ⑧ ベンガル語 (bn-BD)
- **名前 (Name)**:
  `বিজ্ঞাপন সরান (আজীবন)`
- **説明 (Description)**:
  `এককালীন পেমেন্টে আজীবনের জন্য বিজ্ঞাপনমুক্ত খাতার সুবিধা উপভোগ করুন।`

---

## 3. 生成されたアイテムアイコン一覧

Google Play Console ではアイテム1件につき **1枚の共通アイコン（512×512 PNG）** をアップロードする仕様となっております。そのため、言語を問わず高級感と価値が伝わるユニバーサルアイコンを用意しました。

- **Google Play Console 登録推奨アイコン**:
  [`store_assets/inapp_product_icon_512.png`](./inapp_product_icon_512.png)
  （エメラルドダイヤモンド × ゴールドシールド＆クラウン）

- **言語別バナー / アプリ内表示用アイコン** (`store_assets/inapp_icons/`):
  - 英語: [`icon_en.png`](./inapp_icons/icon_en.png) (`PRO ・ NO ADS`)
  - 日本語: [`icon_ja.png`](./inapp_icons/icon_ja.png) (`PRO ・ 広告非表示`)
  - ヒンディー語: [`icon_hi.png`](./inapp_icons/icon_hi.png) (`PRO ・ विज्ञापन हटाएं`)
  - ポルトガル語: [`icon_pt.png`](./inapp_icons/icon_pt.png) (`PRO ・ SEM ANÚNCIOS`)
  - インドネシア語: [`icon_id.png`](./inapp_icons/icon_id.png) (`PRO ・ BEBAS IKLAN`)
  - フィリピン語: [`icon_fil.png`](./inapp_icons/icon_fil.png) (`PRO ・ WALANG ADS`)
  - スペイン語: [`icon_es.png`](./inapp_icons/icon_es.png) (`PRO ・ SIN ANUNCIOS`)
  - ベンガル語: [`icon_bn.png`](./inapp_icons/icon_bn.png) (`PRO ・ বিজ্ঞাপনমুক্ত`)
