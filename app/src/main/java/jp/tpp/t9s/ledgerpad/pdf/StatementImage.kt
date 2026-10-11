package jp.tpp.t9s.ledgerpad.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.data.Customer
import jp.tpp.t9s.ledgerpad.data.TxnRow
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a mobile-friendly PNG receipt/statement image for instant sharing on WhatsApp, LINE, etc.
 */
object StatementImage {

    fun createCustomerStatementImage(
        context: Context,
        shopName: String,
        customer: Customer,
        currencyCode: String,
        balanceMinor: Long,
        rows: List<TxnRow>,
        shopSubHeader: String = ""
    ): File {
        val displayRows = rows.take(25).asReversed() // Up to 25 recent transactions in chronological order
        val width = 840
        val rowHeight = 46
        val tableRowsHeight = (displayRows.size.coerceAtLeast(1)) * rowHeight
        val height = 470 + tableRowsHeight + 90

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 2. Top Banner (Forest Green)
        paint.color = Color.parseColor("#1B5E20")
        canvas.drawRect(0f, 0f, width.toFloat(), 172f, paint)

        // Shop Title
        paint.color = Color.WHITE
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val title = if (shopName.isNotBlank()) shopName else context.getString(R.string.app_name)
        canvas.drawText(title, 40f, 54f, paint)

        if (shopSubHeader.isNotBlank()) {
            paint.textSize = 16f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#E8F5E9")
            canvas.drawText(shopSubHeader, 40f, 84f, paint)
        }

        // Subtitle & Timestamp
        paint.color = Color.WHITE
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.account_statement), 40f, if (shopSubHeader.isNotBlank()) 118f else 102f, paint)

        val reportDate = Dates.formatDateTime(System.currentTimeMillis())
        paint.textSize = 15f
        paint.color = Color.parseColor("#C8E6C9")
        canvas.drawText("${context.getString(R.string.generated_on)}: $reportDate", 40f, 148f, paint)

        // 3. Customer & Balance Summary Card
        val cardRect = RectF(36f, 195f, (width - 36).toFloat(), 355f)
        paint.color = Color.parseColor("#F5F7F6")
        canvas.drawRoundRect(cardRect, 18f, 18f, paint)

        // Left side: Customer details
        paint.color = Color.parseColor("#212121")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${context.getString(R.string.customer)}: ${customer.name}", 60f, 240f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#616161")
        val phoneStr = if (customer.phone.isNotBlank()) customer.phone else context.getString(R.string.no_phone)
        canvas.drawText("${context.getString(R.string.phone)}: $phoneStr", 60f, 280f, paint)

        val dueStr = customer.dueAt?.let { Dates.formatDate(it) } ?: context.getString(R.string.none)
        canvas.drawText("${context.getString(R.string.due_date)}: $dueStr", 60f, 320f, paint)

        // Right side: Net Balance
        paint.textAlign = Paint.Align.RIGHT
        val rightX = (width - 60).toFloat()
        paint.textSize = 18f
        paint.color = Color.parseColor("#616161")
        canvas.drawText(context.getString(R.string.net_balance), rightX, 240f, paint)

        val balFormatted = Money.format(kotlin.math.abs(balanceMinor), currencyCode)
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = when {
            balanceMinor > 0 -> Color.parseColor("#C62828")
            balanceMinor < 0 -> Color.parseColor("#2E7D32")
            else -> Color.parseColor("#424242")
        }
        canvas.drawText(balFormatted, rightX, 288f, paint)

        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT
        val statusLabel = when {
            balanceMinor > 0 -> context.getString(R.string.customer_owes_you)
            balanceMinor < 0 -> context.getString(R.string.you_owe_customer)
            else -> context.getString(R.string.badge_settled)
        }
        canvas.drawText(statusLabel, rightX, 320f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 4. Table Header
        var y = 385f
        val headerRect = RectF(36f, y, (width - 36).toFloat(), y + 48f)
        paint.color = Color.parseColor("#E8ECE9")
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)

        paint.color = Color.parseColor("#37474F")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.table_date), 52f, y + 30f, paint)
        canvas.drawText(context.getString(R.string.table_note), 195f, y + 30f, paint)
        canvas.drawText(context.getString(R.string.table_gave), 460f, y + 30f, paint)
        canvas.drawText(context.getString(R.string.table_got), 580f, y + 30f, paint)
        canvas.drawText(context.getString(R.string.table_balance), 695f, y + 30f, paint)

        y += 56f
        paint.typeface = Typeface.DEFAULT

        if (displayRows.isEmpty()) {
            paint.color = Color.parseColor("#9E9E9E")
            paint.textSize = 18f
            canvas.drawText(context.getString(R.string.no_activity_yet), 52f, y + 30f, paint)
            y += rowHeight
        } else {
            for (row in displayRows) {
                paint.color = Color.parseColor("#424242")
                paint.textSize = 16f
                canvas.drawText(Dates.formatShortDate(row.txn.occurredAt), 52f, y + 26f, paint)

                val note = if (row.txn.note.length > 20) row.txn.note.take(18) + "…" else row.txn.note
                canvas.drawText(note.ifBlank { "-" }, 195f, y + 26f, paint)

                if (row.txn.type == TxnType.GAVE) {
                    paint.color = Color.parseColor("#C62828")
                    canvas.drawText(Money.format(row.txn.amountMinor, currencyCode), 460f, y + 26f, paint)
                    paint.color = Color.parseColor("#BDBDBD")
                    canvas.drawText("-", 580f, y + 26f, paint)
                } else {
                    paint.color = Color.parseColor("#BDBDBD")
                    canvas.drawText("-", 460f, y + 26f, paint)
                    paint.color = Color.parseColor("#2E7D32")
                    canvas.drawText(Money.format(row.txn.amountMinor, currencyCode), 580f, y + 26f, paint)
                }

                paint.color = Color.parseColor("#212121")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(Money.format(row.runningBalanceMinor, currencyCode), 695f, y + 26f, paint)
                paint.typeface = Typeface.DEFAULT

                // Divider line
                paint.color = Color.parseColor("#EEEEEE")
                canvas.drawLine(40f, y + 40f, (width - 40).toFloat(), y + 40f, paint)

                y += rowHeight
            }
        }

        // 5. Footer
        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 15f
        canvas.drawText(context.getString(R.string.pdf_footer_note), 40f, (height - 32).toFloat(), paint)

        val outDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val safeName = customer.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(outDir, "statement_img_${safeName}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }
}
