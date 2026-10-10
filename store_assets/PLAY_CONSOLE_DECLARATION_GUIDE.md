# Google Play Console 申告・入力完全ガイド（LedgerPad）

このチェックシートに沿って Google Play Console の入力・申告を行うことで、近年厳格化されたポリシー審査をスムーズに通過できます。

---

## 1. アプリのアクセス権（App Access）
- **選択**: **「すべての機能は制限なしで利用可能（All functionality is available without special access）」**
  - **解説**: アカウント登録やログイン不要の完全ローカルアプリのため、テスト用アカウント情報の提出は不要です。

---

## 2. 広告（Ads）
- **選択**: **「はい、アプリに広告が含まれています（Yes, my app contains ads）」**
  - **解説**: AdMob（バナー、インタースティシャル）を組み込んでいるため。

---

## 3. コンテンツのレーティング（Content Rating）
アンケート形式の質問に回答します。
- **カテゴリ**: **「ユーティリティ、生産性、ツール、その他（Utility, Productivity, Communication, or Other）」**
- **暴力・性的コンテンツ・違法薬物**: すべて「いいえ（No）」
- **ユーザー間のコンテンツ共有**: **「いいえ（No）」**
  - **重要**: アプリ内に独自のメッセージ送受信機能やチャット機能はなく、OS標準の外部アプリ連携（WhatsAppや共有インテント）を呼び出すだけのため、「いいえ」が正しい選択です。
- **身体的位置の共有**: 「いいえ（No）」
- **デジタル商品の購入**: **「はい（Yes）」**（アプリ内アイテム「広告非表示」があるため）
- **結果**: 全年齢（3歳以上 / Everyone）のレーティングが付与されます。

---

## 4. ターゲット層（Target Audience）
- **対象年齢**: **「18歳以上（18 and over）」**
  - **重要**: 店舗・自営業者向けの帳簿ツールであるため、18歳以上を選択してください。13歳未満や未成年を含めると、極めて厳格な「ファミリーポリシー」「子供向け広告SDK規制」の審査対象となり、リジェクトリスクが跳ね上がります。
- **子供へのアピール（Neutral Appeal）**: 「いいえ（No）」

---

## 5. ニュースアプリ（News apps）
- **選択**: **「いいえ（No）」**

---

## 6. COVID-19 接触確認アプリ（COVID-19 apps）
- **選択**: **「マイアプリは COVID-19 の接触確認・健康状態確認アプリではありません」**

---

## 7. 金融機能の申告（Financial Features Declaration）※最重要！審査の最大の壁
Google Play が最も厳しく審査・リジェクトを行う項目です。

- **申告の選択**:
  - **第一選択（推奨）**: **「このアプリは金融機能を提供していません（My app doesn't provide any financial features）」**
  - **理由**: LedgerPad は「個人間の貸し借りやツケをメモするだけの電卓・メモ帳（Bookkeeping Memo Tool）」であり、銀行業務、融資（ローン）、決済代行、送金業務、暗号資産取引を一切行いません。
  - **もしカテゴリ選択が必須となる場合**:
    - **「パーソナルファイナンス / 帳簿管理（Personal Finance / Bookkeeping / Ledger Management）」** を選択。
    - **絶対に選択してはいけない項目**:
      - ❌ **個人向け融資（Personal Loans / Peer-to-Peer Lending）**（選択すると各国の金融庁ライセンス提出が求められ即座に停止されます）
      - ❌ **暗号資産（Cryptocurrency）**
      - ❌ **決済処理・バンキング（Payment Processing / Mobile Banking）**
    - 備考・説明欄がある場合：
      > "LedgerPad is an offline-first bookkeeping and calculator utility for shopkeepers to manually record customer debts and daily cash sales. The app does not process payments, does not provide loans, and does not connect to bank accounts."（LedgerPadは決済や融資を行わず、店主が手動でメモを記録するだけのオフライン帳簿ツールです）と記載してください。

---

## 8. 広告 ID（Advertising ID）
- **質問**: **「アプリは広告 ID を使用しますか？」**
- **回答**: **「はい（Yes）」**
- **使用目的のチェック**:
  - ✅ **広告またはマーケティング（Advertising or marketing）**
  - ✅ **アナリティクス（Analytics）**
- **解説**: `AndroidManifest.xml` に `<uses-permission android:name="com.google.android.gms.permission.AD_ID" />` が含まれているため、必ず「はい」と答える必要があります（「いいえ」にするとバイナリ解析で不一致と判定されリジェクトされます）。

---

## 9. データセーフティ（Data Safety）
Google Play の規約上、**「第三者SDK（AdMob / Google Play Billing）が収集するデータ」もアプリ開発者が申告する義務**があります。

### 基本的な質問
- **データの収集または共有**: **「はい（Yes）」**
- **転送時の暗号化**: **「はい（Yes）」**（すべてのデータは HTTPS/TLS で暗号化されて転送されます）
- **アカウント作成**: **「いいえ（No）」**（アカウント不要アプリ）
- **データ削除のリクエスト**: **「はい（Yes）」**
  - URL欄または説明: アプリ内の「設定 > データを初期化」またはアプリアンインストールにより、端末内のすべてのデータが完全に消去される旨を案内。

### 収集・共有するデータ項目の詳細設定

#### ① 財務情報（Financial info）
- **回答**: **「収集しない（Not collected）」**
  - **理由**: ユーザーが入力する帳簿残高や顧客データは、端末内のローカル SQLite にのみ保存され、開発者や外部のサーバーには一切送信されないためです。
  - ※Google Play Billing の決済情報は Google 自体が直接処理するため、本アプリが収集した扱いにはなりません。

#### ② 端末またはその他の識別子（Device or other IDs）
- **「広告 ID（Advertising ID）」**: **「収集あり（Collected）」**
  - **共有の有無**: **「共有あり（Shared）」**（AdMob 広告ネットワークへ配信のために送信されるため）
  - **一時的処理か**: **「いいえ（No）」**
  - **必須か任意か**: **「必須（Required）」**（無料版の広告機能に必須）
  - **収集目的**:
    - ✅ **広告またはマーケティング（Advertising or marketing）**
    - ✅ **アナリティクス（Analytics）**

#### ③ アプリの操作とパフォーマンス（App info and performance）
- **「クラッシュログ（Crash logs）」 / 「診断データ（Diagnostics）」**: **「収集あり（Collected）」**
  - Google Play サービスおよび AdMob がクラッシュや表示不具合の検知のために自動収集するため。
  - **目的**: **アナリティクス（Analytics）**

---

## 10. プライバシーポリシー（Privacy Policy）
- **ポリシー URL**:
  `https://t9star.github.io/ledgerpad/`
- **確認事項**:
  - GitHub Pages で正常に公開中。
  - 完全オフラインであること、個人データを収集しないこと、AdMob・Google Play Billing を使用することが明記されています。

---

## 11. アプリ内アイテム（In-app Products）
Google Play Console > **収益化** > **アプリ内アイテム** で作成：
- **商品 ID（Product ID）**: `ledgerpad_remove_ads`（※コードと完全一致必須）
- **商品名**: 「広告を非表示（Remove Ads - Lifetime）」
- **説明**: 「すべての広告を非表示にします（買い切り）」
- **ステータス**: **有効（Active）**
- **価格設定例**:
  - 日本: ¥300
  - インド: ₹49 〜 ₹99
  - ブラジル: R$ 4.90
  - 米国: $1.99
