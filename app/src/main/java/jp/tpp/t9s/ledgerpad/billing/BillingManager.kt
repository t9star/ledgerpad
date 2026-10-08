package jp.tpp.t9s.ledgerpad.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import jp.tpp.t9s.ledgerpad.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles Google Play Billing for in-app one-time purchase: "Remove Ads" (lifetime).
 * Complies with Play Billing Library 8.0.0+ rules and acknowledges purchases.
 */
class BillingManager(
    private val context: Context,
    private val prefs: Prefs
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_REMOVE_ADS = "ledgerpad_remove_ads"
    }

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _formattedPrice = MutableStateFlow("$1.99")
    val formattedPrice: StateFlow<String> = _formattedPrice.asStateFlow()

    init {
        initBilling()
    }

    private fun initBilling() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry if requested
            }
        })
    }

    private fun queryPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var found = false
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        found = true
                        acknowledgeIfNeeded(purchase)
                        break
                    }
                }
                prefs.setAdFree(found)
            }
        }
    }

    private fun queryProducts() {
        val client = billingClient ?: return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = result.productDetailsList.firstOrNull { it.productId == PRODUCT_ID_REMOVE_ADS }
                if (details != null) {
                    _productDetails.value = details
                    details.oneTimePurchaseOfferDetails?.formattedPrice?.let {
                        _formattedPrice.value = it
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val client = billingClient ?: return
        val details = _productDetails.value

        if (details != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            client.launchBillingFlow(activity, flowParams)
        } else {
            // Debug/Test toggle when no Play Console account connected
            prefs.setAdFree(true)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    prefs.setAdFree(true)
                    acknowledgeIfNeeded(purchase)
                }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val client = billingClient ?: return
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(acknowledgePurchaseParams) { _ -> }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
