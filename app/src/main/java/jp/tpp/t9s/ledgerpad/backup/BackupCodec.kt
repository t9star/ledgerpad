package jp.tpp.t9s.ledgerpad.backup

import jp.tpp.t9s.ledgerpad.data.BackupFile
import jp.tpp.t9s.ledgerpad.data.CustomerSummary
import jp.tpp.t9s.ledgerpad.data.TxnRow
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeJson(backup: BackupFile): String = json.encodeToString(backup)

    fun decodeJson(content: String): BackupFile = json.decodeFromString(content)

    /**
     * Generates a CSV export of customer statement transactions.
     */
    fun exportCustomerCsv(
        customerName: String,
        currencyCode: String,
        rows: List<TxnRow>
    ): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Amount,Running Balance,Note\n")
        // rows are newest first, order oldest first for clear accounting statement
        rows.asReversed().forEach { row ->
            val date = Dates.formatDateTime(row.txn.occurredAt)
            val typeStr = if (row.txn.type == TxnType.GAVE) "Gave (Debit)" else "Got (Credit)"
            val amt = Money.format(row.txn.amountMinor, currencyCode)
            val bal = Money.format(row.runningBalanceMinor, currencyCode)
            val escapedNote = "\"${row.txn.note.replace("\"", "\"\"")}\""
            sb.append("$date,$typeStr,$amt,$bal,$escapedNote\n")
        }
        return sb.toString()
    }

    /**
     * Generates a CSV export of all customers and their outstanding balances.
     */
    fun exportAllCustomersCsv(
        currencyCode: String,
        summaries: List<CustomerSummary>
    ): String {
        val sb = StringBuilder()
        sb.append("Customer Name,Phone,Balance,Status,Due Date\n")
        summaries.forEach { s ->
            val name = "\"${s.customer.name.replace("\"", "\"\"")}\""
            val phone = s.customer.phone
            val bal = Money.format(s.balanceMinor, currencyCode)
            val status = if (s.balanceMinor > 0) "To Receive" else if (s.balanceMinor < 0) "To Give" else "Settled"
            val due = s.customer.dueAt?.let { Dates.formatDate(it) } ?: "-"
            sb.append("$name,$phone,$bal,$status,$due\n")
        }
        return sb.toString()
    }
}
