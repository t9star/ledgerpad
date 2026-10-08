package jp.tpp.t9s.ledgerpad.data

import android.content.Context
import android.content.SharedPreferences
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small settings store. SharedPreferences is enough here and keeps the dependency list short. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _shopName = MutableStateFlow(sp.getString(K_SHOP, "") ?: "")
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    private val _currency = MutableStateFlow(sp.getString(K_CURRENCY, null) ?: Money.defaultCurrencyCode())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _lockEnabled = MutableStateFlow(sp.getBoolean(K_LOCK, false))
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    /** Cached "remove ads" entitlement, so ads do not flash before Billing answers. */
    private val _adFree = MutableStateFlow(sp.getBoolean(K_AD_FREE, false))
    val adFree: StateFlow<Boolean> = _adFree.asStateFlow()

    private val _sortMode = MutableStateFlow(
        runCatching { SortMode.valueOf(sp.getString(K_SORT, null) ?: "") }.getOrDefault(SortMode.RECENT)
    )
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val qrCodeFile = java.io.File(context.filesDir, "payment_qr.png")
    private val _hasQrCode = MutableStateFlow(qrCodeFile.exists())
    val hasQrCode: StateFlow<Boolean> = _hasQrCode.asStateFlow()

    fun setShopName(v: String) { _shopName.value = v; sp.edit().putString(K_SHOP, v).apply() }
    fun setCurrency(v: String) { _currency.value = v; sp.edit().putString(K_CURRENCY, v).apply() }
    fun setLockEnabled(v: Boolean) { _lockEnabled.value = v; sp.edit().putBoolean(K_LOCK, v).apply() }
    fun setAdFree(v: Boolean) { _adFree.value = v; sp.edit().putBoolean(K_AD_FREE, v).apply() }
    fun setSortMode(v: SortMode) { _sortMode.value = v; sp.edit().putString(K_SORT, v.name).apply() }

    fun notifyQrCodeUpdated() {
        _hasQrCode.value = qrCodeFile.exists()
    }

    fun deleteQrCode() {
        if (qrCodeFile.exists()) {
            qrCodeFile.delete()
        }
        _hasQrCode.value = false
    }

    private companion object {
        const val K_SHOP = "shop_name"
        const val K_CURRENCY = "currency"
        const val K_LOCK = "lock_enabled"
        const val K_AD_FREE = "ad_free"
        const val K_SORT = "sort_mode"
    }
}
