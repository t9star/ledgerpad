package jp.tpp.t9s.ledgerpad.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import jp.tpp.t9s.ledgerpad.R
import java.io.File
import java.net.URLEncoder

object Sharing {

    /**
     * Sends a friendly payment reminder via WhatsApp or fallback to generic share sheet.
     */
    fun sendReminder(
        context: Context,
        phoneNumber: String,
        customerName: String,
        shopName: String,
        balanceFormatted: String,
        dueFormatted: String?
    ) {
        val storeLabel = if (shopName.isNotBlank()) shopName else context.getString(R.string.app_name)
        val text = if (dueFormatted != null) {
            context.getString(R.string.reminder_msg_with_due, customerName, balanceFormatted, storeLabel, dueFormatted)
        } else {
            context.getString(R.string.reminder_msg_simple, customerName, balanceFormatted, storeLabel)
        }

        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isNotEmpty()) {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val waUri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedText")
                val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // WhatsApp not installed, fallback to share chooser
            }
        }

        // Generic text share
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_reminder)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    /**
     * Sends a friendly payment receipt / acknowledgment via WhatsApp or fallback to generic share sheet.
     */
    fun sendPaymentReceipt(
        context: Context,
        phoneNumber: String,
        customerName: String,
        shopName: String,
        paidAmountFormatted: String,
        remainingBalanceFormatted: String
    ) {
        val storeLabel = if (shopName.isNotBlank()) shopName else context.getString(R.string.app_name)
        val text = context.getString(
            R.string.receipt_msg,
            customerName,
            paidAmountFormatted,
            storeLabel,
            remainingBalanceFormatted
        )

        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isNotEmpty()) {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val waUri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedText")
                val waIntent = Intent(Intent.ACTION_VIEW, waUri)
                waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(waIntent)
                return
            } catch (_: Exception) {
                // Fallback to share chooser
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_receipt_title)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    /**
     * Shares a file (PDF, CSV, JSON) via FileProvider and Android Sharesheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    /**
     * Dials customer phone number via dialer intent (no CALL_PHONE permission required).
     */
    fun dialNumber(context: Context, phoneNumber: String) {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
