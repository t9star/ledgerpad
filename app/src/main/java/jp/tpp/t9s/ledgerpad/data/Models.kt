package jp.tpp.t9s.ledgerpad.data

import kotlinx.serialization.Serializable

/** Transaction direction. GAVE = shop gave goods/money on credit (customer owes more). GOT = payment received. */
enum class TxnType(val code: Int) {
    GAVE(0), GOT(1);

    companion object {
        fun of(code: Int): TxnType = if (code == 1) GOT else GAVE
    }
}

data class Customer(
    val id: Long,
    val name: String,
    val phone: String,
    val dueAt: Long?,
    val createdAt: Long,
)

/** Customer row for lists, with aggregated balance. balanceMinor > 0 means the customer owes the shop. */
data class CustomerSummary(
    val customer: Customer,
    val balanceMinor: Long,
    val lastActivityAt: Long?,
)

data class Txn(
    val id: Long,
    val customerId: Long,
    val type: TxnType,
    /** Always positive, stored in 1/100 units of the currency. */
    val amountMinor: Long,
    val note: String,
    val occurredAt: Long,
    val createdAt: Long,
)

/** Txn with the customer's running balance after this entry (for the timeline). */
data class TxnRow(val txn: Txn, val runningBalanceMinor: Long)

data class HomeTotals(
    val toGetMinor: Long,
    val toGiveMinor: Long,
    val collectedTodayMinor: Long,
)

enum class SortMode { RECENT, BALANCE, NAME }

/** Cashbook (daily sales / expenses). IN = money in (sale), OUT = money out (expense). */
enum class CashType(val code: Int) {
    IN(0), OUT(1);

    companion object {
        fun of(code: Int): CashType = if (code == 1) OUT else IN
    }
}

data class CashEntry(
    val id: Long,
    val type: CashType,
    val amountMinor: Long,
    val note: String,
    val occurredAt: Long,
    val createdAt: Long,
)

data class CashTotals(val inMinor: Long, val outMinor: Long) {
    val netMinor: Long get() = inMinor - outMinor
}

// ---- Backup DTOs (JSON) ----

@Serializable
data class BackupCash(
    val id: Long,
    val type: Int,
    val amountMinor: Long,
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long,
)

@Serializable
data class BackupCustomer(
    val id: Long,
    val name: String,
    val phone: String = "",
    val dueAt: Long? = null,
    val createdAt: Long,
)

@Serializable
data class BackupTxn(
    val id: Long,
    val customerId: Long,
    val type: Int,
    val amountMinor: Long,
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long,
)

@Serializable
data class BackupFile(
    val format: String = FORMAT_ID,
    val version: Int = 1,
    val exportedAt: Long,
    val shopName: String = "",
    val currency: String = "",
    val customers: List<BackupCustomer>,
    val txns: List<BackupTxn>,
    val cash: List<BackupCash> = emptyList(),
) {
    companion object {
        const val FORMAT_ID = "ledgerpad-backup"
    }
}
