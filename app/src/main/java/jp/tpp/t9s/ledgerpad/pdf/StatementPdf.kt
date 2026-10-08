package jp.tpp.t9s.ledgerpad.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.data.Customer
import jp.tpp.t9s.ledgerpad.data.TxnRow
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import java.io.File
import java.io.FileOutputStream

object StatementPdf {

    /**
     * Generates an official A4 customer transaction statement PDF using standard Android PdfDocument APIs.
     * Stored in the app's cache directory ready for sharing.
     */
    fun createCustomerStatement(
        context: Context,
        shopName: String,
        customer: Customer,
        currencyCode: String,
        balanceMinor: Long,
        rows: List<TxnRow>
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595 // A4 standard pt width at 72dpi
        val pageHeight = 842 // A4 standard pt height
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header Background
        paint.color = Color.parseColor("#1B5E20") // Deep Forest Green
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

        // Shop Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val title = if (shopName.isNotBlank()) shopName else context.getString(R.string.app_name)
        canvas.drawText(title, 30f, 45f, paint)

        // Subtitle
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.account_statement), 30f, 70f, paint)

        val reportDate = Dates.formatDateTime(System.currentTimeMillis())
        paint.textSize = 10f
        canvas.drawText("${context.getString(R.string.generated_on)}: $reportDate", 30f, 92f, paint)

        // Customer Info Card
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRoundRect(30f, 130f, (pageWidth - 30).toFloat(), 210f, 8f, 8f, paint)

        paint.color = Color.parseColor("#212121")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${context.getString(R.string.customer)}: ${customer.name}", 45f, 155f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#616161")
        val phoneStr = if (customer.phone.isNotBlank()) customer.phone else context.getString(R.string.no_phone)
        canvas.drawText("${context.getString(R.string.phone)}: $phoneStr", 45f, 175f, paint)

        val dueStr = customer.dueAt?.let { Dates.formatDate(it) } ?: context.getString(R.string.none)
        canvas.drawText("${context.getString(R.string.due_date)}: $dueStr", 45f, 195f, paint)

        // Total Balance (Right aligned in Customer Card)
        val balFormatted = Money.format(balanceMinor, currencyCode)
        paint.textSize = 11f
        paint.color = Color.parseColor("#616161")
        canvas.drawText(context.getString(R.string.net_balance), 400f, 155f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (balanceMinor > 0) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
        canvas.drawText(balFormatted, 400f, 185f, paint)

        // Table Header
        var y = 240f
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 25f, paint)

        paint.color = Color.parseColor("#424242")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.table_date), 40f, y + 17f, paint)
        canvas.drawText(context.getString(R.string.table_note), 150f, y + 17f, paint)
        canvas.drawText(context.getString(R.string.table_gave), 330f, y + 17f, paint)
        canvas.drawText(context.getString(R.string.table_got), 410f, y + 17f, paint)
        canvas.drawText(context.getString(R.string.table_balance), 490f, y + 17f, paint)

        // Table Rows (chronological: oldest to newest)
        y += 30f
        paint.typeface = Typeface.DEFAULT
        val orderedRows = rows.asReversed()

        for (row in orderedRows) {
            if (y > pageHeight - 50) break // Fit to 1 page for brevity in MVP

            paint.color = Color.parseColor("#424242")
            paint.textSize = 9f
            canvas.drawText(Dates.formatShortDate(row.txn.occurredAt), 40f, y + 12f, paint)

            val note = if (row.txn.note.length > 25) row.txn.note.take(22) + "..." else row.txn.note
            canvas.drawText(note.ifBlank { "-" }, 150f, y + 12f, paint)

            if (row.txn.type == TxnType.GAVE) {
                paint.color = Color.parseColor("#C62828")
                canvas.drawText(Money.format(row.txn.amountMinor, currencyCode), 330f, y + 12f, paint)
                paint.color = Color.parseColor("#9E9E9E")
                canvas.drawText("-", 410f, y + 12f, paint)
            } else {
                paint.color = Color.parseColor("#9E9E9E")
                canvas.drawText("-", 330f, y + 12f, paint)
                paint.color = Color.parseColor("#2E7D32")
                canvas.drawText(Money.format(row.txn.amountMinor, currencyCode), 410f, y + 12f, paint)
            }

            paint.color = Color.parseColor("#212121")
            canvas.drawText(Money.format(row.runningBalanceMinor, currencyCode), 490f, y + 12f, paint)

            // subtle divider
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(30f, y + 20f, (pageWidth - 30).toFloat(), y + 20f, paint)

            y += 22f
        }

        // Footer
        paint.color = Color.parseColor("#9E9E9E")
        paint.textSize = 9f
        canvas.drawText(context.getString(R.string.pdf_footer_note), 30f, (pageHeight - 20).toFloat(), paint)

        pdfDocument.finishPage(page)

        val outDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val safeName = customer.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(outDir, "statement_${safeName}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return outFile
    }
}
