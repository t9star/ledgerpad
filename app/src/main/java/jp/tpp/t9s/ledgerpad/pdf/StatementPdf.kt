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
        rows: List<TxnRow>,
        shopSubHeader: String = ""
    ): File {
        val pdfDocument = PdfDocument()

        val pageWidth = 595 // A4 standard pt width at 72dpi
        val pageHeight = 842 // A4 standard pt height
        var currentPageNumber = 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun drawTableHeader(c: Canvas, topY: Float) {
            paint.color = Color.parseColor("#EEEEEE")
            c.drawRect(30f, topY, (pageWidth - 30).toFloat(), topY + 25f, paint)

            paint.color = Color.parseColor("#424242")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText(context.getString(R.string.table_date), 40f, topY + 17f, paint)
            c.drawText(context.getString(R.string.table_note), 150f, topY + 17f, paint)
            c.drawText(context.getString(R.string.table_gave), 330f, topY + 17f, paint)
            c.drawText(context.getString(R.string.table_got), 410f, topY + 17f, paint)
            c.drawText(context.getString(R.string.table_balance), 490f, topY + 17f, paint)
        }

        fun drawPageFooter(c: Canvas, pageNum: Int) {
            paint.color = Color.parseColor("#9E9E9E")
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            c.drawText(context.getString(R.string.pdf_footer_note), 30f, (pageHeight - 20).toFloat(), paint)
            val pageStr = "Page $pageNum"
            val textWidth = paint.measureText(pageStr)
            c.drawText(pageStr, pageWidth - 30f - textWidth, (pageHeight - 20).toFloat(), paint)
        }

        // Header Background
        paint.color = Color.parseColor("#1B5E20") // Deep Forest Green
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 115f, paint)

        // Shop Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val title = if (shopName.isNotBlank()) shopName else context.getString(R.string.app_name)
        canvas.drawText(title, 30f, 38f, paint)

        // Optional Shop Sub-Header (Tax ID / Invoice No. / Address / Receipt Note)
        if (shopSubHeader.isNotBlank()) {
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.parseColor("#E8F5E9")
            canvas.drawText(shopSubHeader, 30f, 56f, paint)
        }

        // Subtitle
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(context.getString(R.string.account_statement), 30f, if (shopSubHeader.isNotBlank()) 76f else 66f, paint)

        val reportDate = Dates.formatDateTime(System.currentTimeMillis())
        paint.textSize = 10f
        canvas.drawText("${context.getString(R.string.generated_on)}: $reportDate", 30f, 96f, paint)

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

        // First Page Table Header
        var y = 240f
        drawTableHeader(canvas, y)

        // Table Rows (chronological: oldest to newest)
        y += 30f
        paint.typeface = Typeface.DEFAULT
        val orderedRows = rows.asReversed()

        for (row in orderedRows) {
            // Check if we need to advance to a new page
            if (y + 24f > pageHeight - 45f) {
                drawPageFooter(canvas, currentPageNumber)
                pdfDocument.finishPage(page)

                currentPageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(nextInfo)
                canvas = page.canvas

                // Continuation mini-header
                paint.color = Color.parseColor("#1B5E20")
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 35f, paint)

                paint.color = Color.WHITE
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val contTitle = "${customer.name} – ${context.getString(R.string.account_statement)} ($currentPageNumber)"
                canvas.drawText(contTitle, 30f, 22f, paint)

                // Table Header on continuation page
                y = 45f
                drawTableHeader(canvas, y)
                y += 30f
                paint.typeface = Typeface.DEFAULT
            }

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

        // Draw footer on final page
        drawPageFooter(canvas, currentPageNumber)
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
