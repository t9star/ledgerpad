package jp.tpp.t9s.ledgerpad.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import jp.tpp.t9s.ledgerpad.BuildConfig
import jp.tpp.t9s.ledgerpad.data.Prefs

/**
 * Manages Google Mobile Ads (AdMob) initialization, UMP GDPR/privacy consent,
 * and frequency-capped Interstitial ads.
 */
class AdsManager(
    private val context: Context,
    private val prefs: Prefs
) {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var lastInterstitialShownAt = 0L

    companion object {
        // Cooldown: at least 3 minutes between interstitial ads
        private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L
    }

    /**
     * Gathers consent with UMP SDK, and once ready, initializes MobileAds.
     */
    fun initializeConsentAndAds(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError == null && consentInfo.canRequestAds()) {
                        startMobileAds()
                    }
                }
            },
            { _ ->
                if (consentInfo.canRequestAds()) {
                    startMobileAds()
                }
            }
        )

        if (consentInfo.canRequestAds()) {
            startMobileAds()
        }
    }

    private fun startMobileAds() {
        MobileAds.initialize(context) {
            loadInterstitial()
        }
    }

    fun loadInterstitial() {
        if (prefs.adFree.value) return
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.AD_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                }
            }
        )
    }

    /**
     * Displays an interstitial only after non-disruptive actions (e.g. Export PDF / Backup complete)
     * adhering to frequency capping.
     */
    fun showInterstitialIfAllowed(activity: Activity, onDismiss: () -> Unit = {}) {
        if (prefs.adFree.value) {
            onDismiss()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastInterstitialShownAt < INTERSTITIAL_COOLDOWN_MS) {
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShownAt = System.currentTimeMillis()
                    loadInterstitial()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitial()
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial()
            onDismiss()
        }
    }
}
