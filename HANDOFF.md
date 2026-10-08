# LedgerPad 引き継ぎ書（HANDOFF）

> このファイルは、Claude（Antigravity）から別のAIエージェント（Gemini等）へ作業を引き継ぐための資料です。
> **作業を始める前に必ず全文を読み、作業を終えたら「進捗状況」と「作業ログ」を更新してください。**

最終更新：2026-10-08（Claude）

---

## 1. プロジェクト概要

- **アプリ**：新興国（インド・ブラジル・インドネシア・フィリピン等）の個人商店向け「ツケ帳＋現金出納帳」アプリ
- **アプリ名 / パッケージ名**：LedgerPad / `jp.tpp.t9s.ledgerpad`
- **方針**：完全オフライン・アカウント不要・軽量（リリースAPK 10MB以下目標）・**ランニングコスト0（サーバーなし）**
- **収益**：AdMob（ネイティブ・バナー・インタースティシャル）＋「広告非表示」の買い切り（商品ID `ledgerpad_remove_ads`）
- **ローカルパス**：`C:\Users\frog9\.gemini\antigravity\scratch\ledgerpad`
- **GitHub**：`https://github.com/t9star/ledgerpad`（公開リポジトリ）
- **実装計画書**：`C:\Users\frog9\.gemini\antigravity\brain\a6003b41-5c2e-41f4-93f0-6c12a4500677\implementation_plan.md`

### ユーザーの決定事項
1. アプリ名は LedgerPad で確定
2. リポジトリは公開。プライバシーポリシーは GitHub Pages で公開
3. 言語は 英語(既定) / ヒンディー語 `hi` / ブラジルポルトガル語 `pt-rBR` / インドネシア語 `in` / スペイン語 `es` / ベンガル語 `bn` / 日本語 `ja` / **フィリピン語（タガログ語） `tl`** の8言語
4. 現金出納帳（売上・経費）をMVPに含める

---

## 2. 開発環境（Windows / PowerShell）

| 項目 | 値 |
|---|---|
| JDK | `C:\Program Files\Android\Android Studio\jbr`（PATHに java がないので `JAVA_HOME` を指定する） |
| Android SDK | `C:\Users\frog9\AppData\Local\Android\Sdk`（`local.properties` に記載済み） |
| Gradle | Wrapper 9.1.0（SonicFixプロジェクトからコピー） |
| git | `C:\Program Files\Git\cmd\git.exe`（PATHにない場合は絶対パスで実行） |
| gh | `C:\Program Files\GitHub CLI\gh.exe` |

### ビルドコマンド例
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug        # デバッグAPK
.\gradlew.bat :app:testDebugUnitTest    # 単体テスト
.\gradlew.bat :app:bundleRelease        # リリースAAB（keystore.properties が必要）
```

### ユーザー共通ルール（必ず守る）
- コードを変更したら **git commit & push**
- 実装計画書は**日本語**で書く
- R8：`proguard-rules.pro` に kotlinx.serialization のルールと `-keep class androidx.work.impl.WorkDatabase_Impl { *; }` を入れる（入れてある）
- 古いSDKの警告が出たら、`:app:dependencies` で原因を調べ、該当ライブラリの新しいバージョンを明示的に依存関係へ追加する（fragment / work は追加済み）
- adb は必ず `-s <serial>` を指定する
- Playに再アップロードするときは `versionCode` を上げる
- Billing Library は v6以上（**8.0.0 を使用**）
- リリース後は、成果物を `app/release/` にコピーする

---

## 3. アーキテクチャ

```
app/src/main/java/jp/tpp/t9s/ledgerpad/
├── LedgerApp.kt            Application。AppContainer（手動DI）を保持
├── MainActivity.kt         FragmentActivity（BiometricPrompt のため）。UMPの同意取得、アプリロック、画面遷移
├── data/
│   ├── Models.kt           ドメインモデル＋バックアップ用DTO（@Serializable）
│   ├── LedgerDb.kt         SQLiteOpenHelper（Roomは使わない）。テーブル：customers / txns / cash_entries
│   ├── LedgerRepository.kt CRUD＋Flow（version カウンタで再クエリする方式）
│   └── Prefs.kt            SharedPreferences（店名・通貨・ロック・広告非表示キャッシュ）
├── util/                   Money（金額の表示・解析）、Calc（テンキーの式の計算）、Dates
├── backup/BackupCodec.kt   JSON/CSV の変換（純粋関数なので単体テスト可能）
├── share/Sharing.kt        WhatsAppのインテント、ファイル共有（FileProvider）、電話の発信
├── pdf/StatementPdf.kt     android.graphics.pdf.PdfDocument で取引明細のPDFを作る
├── reminder/DueReminderWorker.kt  WorkManager で毎日、期日の来た顧客を通知
├── ads/                    AdsManager（UMP・インタースティシャルの表示間隔制御）＋ Compose用の広告View
├── billing/BillingManager.kt  広告非表示の買い切り（acknowledge 処理あり）
└── ui/                     Compose画面（Home: 顧客/出納帳タブ、Customer、Settings、AmountEntry、Lock）
```

### 重要な設計ルール
- **金額は `Long` 型の1/100単位**（全通貨共通。IDR等の小数がない通貨は表示で小数を隠すだけ）。浮動小数は使わない。
- 残高 = SUM(GAVE) − SUM(GOT)。DBには保存せず、毎回集計する。
- **権限は最小限**：INTERNET / ACCESS_NETWORK_STATE / POST_NOTIFICATIONS / USE_BIOMETRIC / BILLING / AD_ID。**SMS・連絡先・ストレージの権限は使わない**（連絡先は `ACTION_PICK`、ファイルは SAF を使う）。
- インタースティシャル広告は **PDF出力・バックアップの後だけ**（最低3分の間隔）。**記録の操作中は絶対に出さない**。
- AdMob ID：debug は Google公式のテストID。release は `keystore.properties`（gitignore済み）の `admobAppId / adBannerId / adInterstitialId / adNativeId` を読む（未設定ならテストIDになる）。

---

## 4. 進捗状況

| フェーズ | 内容 | 状態 |
|---|---|---|
| 0 | Gradleの構成、バージョンカタログ、R8ルール | ✅ 完了 |
| 1 | DB・リポジトリ・顧客/取引の画面・テンキー入力 | 🔄 作業中 |
| 1b | 現金出納帳（売上・経費）タブ | ⏳ 未着手 |
| 2 | WhatsAppでの催促、PDF、期日リマインダー | ⏳ 未着手 |
| 3 | バックアップ（Auto Backup＋JSON/CSV）、アプリロック | ⏳ 未着手 |
| 4 | 8言語の翻訳、AdMob＋UMP、Billing | ⏳ 未着手 |
| 5 | リリース署名、サイズ最適化、ストア素材、プライバシーポリシー（GitHub Pages） | ⏳ 未着手 |

## 5. 次にやること（TODO）
- [ ] 残りのソース（Repository, util, ui, ads, billing ...）を実装する
- [ ] `assembleDebug` が通るようにする
- [ ] 単体テスト（Calc / Money / BackupCodec）
- [ ] GitHubリポジトリ `t9star/ledgerpad` を作成してpushする
- [ ] リリース用keystoreを作成し、`keystore.properties` に設定する（**keystore はコミットしない**）
- [ ] ユーザーが AdMob でアプリを登録し、実際の広告IDを `keystore.properties` に書く
- [ ] プライバシーポリシーを GitHub Pages で公開する
- [ ] Play Console：アプリ内アイテム `ledgerpad_remove_ads` を登録、データセーフティ（広告IDのみ）、金融機能の申告（記帳ツールのみ・ローンなし）

## 6. 作業ログ
- 2026-10-08 Claude：市場調査 → アイディアを選定 → 実装計画書を作成。プロジェクトの雛形、Gradleの構成、Models / LedgerDb を作成。
