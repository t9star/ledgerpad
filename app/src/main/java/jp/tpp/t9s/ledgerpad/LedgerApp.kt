package jp.tpp.t9s.ledgerpad

import android.app.Application
import jp.tpp.t9s.ledgerpad.ads.AdsManager
import jp.tpp.t9s.ledgerpad.billing.BillingManager
import jp.tpp.t9s.ledgerpad.data.LedgerDb
import jp.tpp.t9s.ledgerpad.data.LedgerRepository
import jp.tpp.t9s.ledgerpad.data.Prefs
import jp.tpp.t9s.ledgerpad.reminder.DueReminderWorker

class AppContainer(val app: Application) {
    val db by lazy { LedgerDb(app) }
    val repository by lazy { LedgerRepository(db) }
    val prefs by lazy { Prefs(app) }
    val adsManager by lazy { AdsManager(app, prefs) }
    val billingManager by lazy { BillingManager(app, prefs) }
}

class LedgerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DueReminderWorker.schedule(this)
    }
}
