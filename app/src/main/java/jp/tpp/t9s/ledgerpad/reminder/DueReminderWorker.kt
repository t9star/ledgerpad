package jp.tpp.t9s.ledgerpad.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import jp.tpp.t9s.ledgerpad.LedgerApp
import jp.tpp.t9s.ledgerpad.MainActivity
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.data.CustomerSummary
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DueReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as? LedgerApp ?: return Result.success()
        val repo = app.container.repository
        val prefs = app.container.prefs

        if (!prefs.remindersEnabled.value) {
            return Result.success()
        }

        val todayEnd = Dates.endOfDay(System.currentTimeMillis())
        val dueCustomers = repo.dueCustomers(todayEnd)

        if (dueCustomers.isNotEmpty()) {
            showNotification(dueCustomers, prefs.currency.value)
        }
        return Result.success()
    }

    private fun showNotification(dueList: List<CustomerSummary>, currencyCode: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "due_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_reminders_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val count = dueList.size
        val totalDueMinor = dueList.sumOf { it.balanceMinor }
        val totalFormatted = Money.format(totalDueMinor, currencyCode)

        val title = context.getString(R.string.notification_due_title)
        val baseMsg = context.resources.getQuantityString(
            R.plurals.notification_due_message,
            count,
            count
        )
        val shortSummary = "$baseMsg ($totalFormatted)"

        val detailsLines = dueList.take(5).joinToString("\n") { item ->
            "• ${item.customer.name}: ${Money.format(item.balanceMinor, currencyCode)}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(shortSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$shortSummary\n$detailsLines"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "daily_due_reminders"

        fun schedule(context: Context) {
            // Calculate delay until next 9:00 AM local time
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(15 * 60 * 1000L)

            val request = PeriodicWorkRequestBuilder<DueReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
