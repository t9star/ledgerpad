package jp.tpp.t9s.ledgerpad.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory
import jp.tpp.t9s.ledgerpad.data.Prefs

/**
 * Handles Google Play In-App Review prompts at positive user milestones
 * (e.g., reaching transaction count milestones, settling a customer's balance, or exporting a statement).
 */
object ReviewHelper {

    private val MILESTONES = setOf(5, 15, 35, 75, 150)
    private const val MIN_DAYS_BETWEEN_PROMPTS_MS = 14L * 24 * 60 * 60 * 1000L

    /**
     * Records a successful user action (transaction saved, statement exported, etc.)
     * and triggers the native Google Play In-App Review sheet if a milestone is met.
     */
    fun recordPositiveAction(context: Context, prefs: Prefs, isMajorMilestone: Boolean = false) {
        val count = prefs.incrementAndGetActionCount()
        val now = System.currentTimeMillis()
        val lastPrompt = prefs.getLastReviewPromptAt()

        val cooldownElapsed = (now - lastPrompt) >= MIN_DAYS_BETWEEN_PROMPTS_MS
        val shouldTrigger = cooldownElapsed && (count in MILESTONES || (isMajorMilestone && count >= 3))

        if (shouldTrigger && context is Activity) {
            launchInAppReview(context, prefs)
        }
    }

    private fun launchInAppReview(activity: Activity, prefs: Prefs) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    prefs.markReviewPrompted()
                    manager.launchReviewFlow(activity, reviewInfo)
                }
            }
        } catch (_: Exception) {
            // Safely ignore if Play Store is unavailable on device
        }
    }

    /**
     * Opens the Google Play Store listing page directly when the user taps "Rate App" in Settings.
     */
    fun openPlayStoreListing(context: Context) {
        val pkg = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(webIntent) }
        }
    }
}
