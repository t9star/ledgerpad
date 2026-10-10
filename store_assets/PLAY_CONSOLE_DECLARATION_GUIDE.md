# Google Play Console 申告・入力完全ガイド（LedgerPad）

このチェックシートに沿って Google Play Console の入力・申告を行うことで、近年厳格化されたポリシー審査をスムーズに通過できます。

---

## 1. アプリのアクセス権（App Access）
- **選択**: **「すべての機能は制限なしで利用可能（All functionality is available without special access）」**
  - アカウント登録やログイン不要の完全ローカルアプリのため、テスト用アカウント情報の提出は不要です。

---

## 2. 広告（Ads）
- **選択**: **「はい、アプリに広告が含まれています（Yes, my app contains ads）」**
  - AdMob（バナー、インタースティシャル）を組み込んでいるため。

---

## 3. コンテンツのレーティング（Content Rating）
- **カテゴリ**: **「ユーティリティ、生産性、ツール、その他（Utility, Productivity, Communication, or Other）」**
- **暴力・性的コンテンツ・違法薬物**: すべて「いいえ（No）」
- **ユーザー間のコンテンツ共有**: 「いいえ（No）」（直接のメッセージングサービスではなく外部アプリ共有のため）
- **身体的位置の共有**: 「いいえ（No）」
- **デジタル商品の購入**: 「はい（Yes）」（アプリ内アイテム「広告非表示」があるため）
- **結果**: 全年齢（3歳以上 / Everyone）のレーティングが付与されます。

---

## 4. ターゲット層（Target Audience）
- **対象年齢**: **「18歳以上（18 and over）」**
  - 店舗・自営業者の帳簿ツールであるため、18歳以上を選択してください（ファミリーポリシー・子供向け広告規制の審査対象外となり、審査が格段に通りやすくなります）。
- **子供へのアピール**: 「いいえ（No）」

---

## 5. 金融機能の申告（Financial Features Declaration）※超重要！
Google Play で最もリジェクトされやすい項目です。
- **選択**: **「このアプリは金融機能を提供していません（My app doesn't provide any financial features）」**
  - ※もし機能選択を求められた場合でも、**「パーソナルファイナンス / 予算管理 / 帳簿ツール（Personal Finance / Bookkeeping / Ledger Management）」** を選択し、**ローン（Lending）、暗号資産（Crypto）、送金・バンキング（Banking/Payment Processing）ではない**ことを確認してください。
  - LedgerPad は「個人間の貸し借りやツケをメモするだけの電卓・帳簿ツール」であり、実際の送金や融資、預金受入は行わないため、金融業ライセンスの提出は不要です。

---

## 6. データセーフティ（Data Safety）
- **データの収集・共有**:
  - **「はい、アプリはデータを収集します（Yes, app collects data）」**（広告ID等のため）
  - **「すべてのユーザーデータは暗号化されて転送されますか？」**: 「はい（Yes）」
  - **「ユーザーがデータ削除をリクエストする方法を提供していますか？」**: 「はい（Yes）」（アプリ内の設定から「データを初期化/削除」またはアンインストールで端末から完全削除されるため）

### 収集するデータ項目：
1. **財務情報（Financial info）**:
   - ユーザーが入力する帳簿データは「端末内にのみ保存され、外部サーバーに送信・収集されない」ため、**「収集しない（Not collected）」** を選択。
2. **端末またはその他の識別子（Device or other IDs）**:
   - **「広告ID（Advertising ID）」**: **収集あり（Collected）**
     - 目的：**広告・マーケティング（Advertising or marketing）**、**アナリティクス（Analytics）**
     - ユーザーアカウントとの関連付け：**いいえ（No）**
3. **アプリの購入履歴（App interactions / Purchase history）**:
   - Google Play Billing による広告非表示アイテムの購入確認のため。
     - 目的：**アプリの機能（App functionality）**

---

## 7. プライバシーポリシー（Privacy Policy）
- **URL**: `https://t9star.github.io/ledgerpad/`
  - GitHub Pages で常時稼働中。AdMob および完全ローカルストレージの条項を含んでおり、Google の規約に準拠しています。

---

## 8. アプリ内アイテム（In-app Products）
Google Play Console > 収益化 > アプリ内アイテム で登録：
- **商品ID（Product ID）**: `ledgerpad_remove_ads`
- **商品名**: 「広告を非表示（Remove Ads - Lifetime）」
- **説明**: 「一度の購入で、すべての広告を永久に非表示にします。」
- **ステータス**: 有効（Active）
- **価格設定例**:
  - 日本: ¥300
  - インド: ₹49 〜 ₹99
  - ブラジル: R$ 4.90
  - 米国: $1.99
