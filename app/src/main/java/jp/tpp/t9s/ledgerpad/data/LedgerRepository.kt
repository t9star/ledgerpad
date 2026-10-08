package jp.tpp.t9s.ledgerpad.data

import android.content.ContentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Data access for customers, credit transactions and the cashbook.
 * Reactive reads re-query whenever [version] is bumped by a write.
 */
class LedgerRepository(private val db: LedgerDb) {

    private val version = MutableStateFlow(0L)
    private fun changed() { version.value = version.value + 1 }

    private fun <T> observe(block: () -> T): Flow<T> =
        version.map { block() }.flowOn(Dispatchers.IO)

    // ---------------- Customers ----------------

    fun observeCustomers(): Flow<List<CustomerSummary>> = observe { queryCustomers() }

    private fun queryCustomers(): List<CustomerSummary> {
        val sql = """
            SELECT c.id, c.name, c.phone, c.due_at, c.created_at,
                   COALESCE(SUM(CASE WHEN t.type = 0 THEN t.amount ELSE -t.amount END), 0) AS bal,
                   MAX(t.occurred_at) AS last_at
            FROM customers c LEFT JOIN txns t ON t.customer_id = c.id
            GROUP BY c.id
        """.trimIndent()
        val out = ArrayList<CustomerSummary>()
        db.readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                out += CustomerSummary(
                    customer = Customer(
                        id = c.long("id"),
                        name = c.str("name"),
                        phone = c.str("phone"),
                        dueAt = c.longOrNull("due_at"),
                        createdAt = c.long("created_at"),
                    ),
                    balanceMinor = c.long("bal"),
                    lastActivityAt = c.longOrNull("last_at"),
                )
            }
        }
        return out
    }

    fun observeCustomer(id: Long): Flow<Customer?> = observe { getCustomerSync(id) }

    private fun getCustomerSync(id: Long): Customer? =
        db.readableDatabase.rawQuery(
            "SELECT id, name, phone, due_at, created_at FROM customers WHERE id = ?",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) Customer(
                c.long("id"), c.str("name"), c.str("phone"), c.longOrNull("due_at"), c.long("created_at")
            ) else null
        }

    suspend fun addCustomer(name: String, phone: String): Long = io {
        val id = db.writableDatabase.insertOrThrow(
            "customers", null,
            customerValues(name.trim(), phone.trim(), null, System.currentTimeMillis())
        )
        changed(); id
    }

    suspend fun updateCustomer(id: Long, name: String, phone: String) = io {
        val v = ContentValues().apply { put("name", name.trim()); put("phone", phone.trim()) }
        db.writableDatabase.update("customers", v, "id = ?", arrayOf(id.toString()))
        changed()
    }

    suspend fun setDueDate(id: Long, dueAt: Long?) = io {
        val v = ContentValues().apply { if (dueAt == null) putNull("due_at") else put("due_at", dueAt) }
        db.writableDatabase.update("customers", v, "id = ?", arrayOf(id.toString()))
        changed()
    }

    suspend fun deleteCustomer(id: Long) = io {
        db.writableDatabase.delete("customers", "id = ?", arrayOf(id.toString()))
        changed()
    }

    // ---------------- Credit transactions ----------------

    /** Newest first, each with running balance after that entry. */
    fun observeTxnRows(customerId: Long): Flow<List<TxnRow>> = observe { queryTxns(customerId) }

    fun queryTxns(customerId: Long): List<TxnRow> {
        val asc = ArrayList<Txn>()
        db.readableDatabase.rawQuery(
            "SELECT * FROM txns WHERE customer_id = ? ORDER BY occurred_at ASC, id ASC",
            arrayOf(customerId.toString())
        ).use { c -> while (c.moveToNext()) asc += c.toTxn() }
        var running = 0L
        val rows = asc.map { t ->
            running += if (t.type == TxnType.GAVE) t.amountMinor else -t.amountMinor
            TxnRow(t, running)
        }
        return rows.asReversed()
    }

    suspend fun addTxn(customerId: Long, type: TxnType, amountMinor: Long, note: String, occurredAt: Long): Long = io {
        val v = ContentValues().apply {
            put("customer_id", customerId)
            put("type", type.code)
            put("amount", amountMinor)
            put("note", note.trim())
            put("occurred_at", occurredAt)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.writableDatabase.insertOrThrow("txns", null, v)
        changed(); id
    }

    suspend fun updateTxn(id: Long, type: TxnType, amountMinor: Long, note: String, occurredAt: Long) = io {
        val v = ContentValues().apply {
            put("type", type.code)
            put("amount", amountMinor)
            put("note", note.trim())
            put("occurred_at", occurredAt)
        }
        db.writableDatabase.update("txns", v, "id = ?", arrayOf(id.toString()))
        changed()
    }

    suspend fun deleteTxn(id: Long) = io {
        db.writableDatabase.delete("txns", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Re-insert a deleted txn (Undo). */
    suspend fun restoreTxn(t: Txn) = io {
        val v = ContentValues().apply {
            put("id", t.id)
            put("customer_id", t.customerId)
            put("type", t.type.code)
            put("amount", t.amountMinor)
            put("note", t.note)
            put("occurred_at", t.occurredAt)
            put("created_at", t.createdAt)
        }
        db.writableDatabase.insertWithOnConflict("txns", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        changed()
    }

    fun observeCollectedSince(fromMillis: Long): Flow<Long> = observe {
        db.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(amount), 0) FROM txns WHERE type = 1 AND occurred_at >= ?",
            arrayOf(fromMillis.toString())
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
    }

    // ---------------- Cashbook ----------------

    fun observeCash(fromMillis: Long, toMillis: Long): Flow<List<CashEntry>> = observe {
        val out = ArrayList<CashEntry>()
        db.readableDatabase.rawQuery(
            "SELECT * FROM cash_entries WHERE occurred_at >= ? AND occurred_at < ? ORDER BY occurred_at DESC, id DESC",
            arrayOf(fromMillis.toString(), toMillis.toString())
        ).use { c -> while (c.moveToNext()) out += c.toCash() }
        out
    }

    suspend fun addCash(type: CashType, amountMinor: Long, note: String, occurredAt: Long): Long = io {
        val v = ContentValues().apply {
            put("type", type.code)
            put("amount", amountMinor)
            put("note", note.trim())
            put("occurred_at", occurredAt)
            put("created_at", System.currentTimeMillis())
        }
        val id = db.writableDatabase.insertOrThrow("cash_entries", null, v)
        changed(); id
    }

    suspend fun updateCash(id: Long, type: CashType, amountMinor: Long, note: String, occurredAt: Long) = io {
        val v = ContentValues().apply {
            put("type", type.code)
            put("amount", amountMinor)
            put("note", note.trim())
            put("occurred_at", occurredAt)
        }
        db.writableDatabase.update("cash_entries", v, "id = ?", arrayOf(id.toString()))
        changed()
    }

    suspend fun deleteCash(id: Long) = io {
        db.writableDatabase.delete("cash_entries", "id = ?", arrayOf(id.toString()))
        changed()
    }

    suspend fun restoreCash(e: CashEntry) = io {
        val v = ContentValues().apply {
            put("id", e.id)
            put("type", e.type.code)
            put("amount", e.amountMinor)
            put("note", e.note)
            put("occurred_at", e.occurredAt)
            put("created_at", e.createdAt)
        }
        db.writableDatabase.insertWithOnConflict("cash_entries", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        changed()
    }

    // ---------------- Reminders ----------------

    /** Customers whose due date is before [untilMillis] and who still owe money. */
    suspend fun dueCustomers(untilMillis: Long): List<CustomerSummary> = io {
        queryCustomers().filter { s ->
            val due = s.customer.dueAt
            due != null && due < untilMillis && s.balanceMinor > 0
        }
    }

    // ---------------- Backup ----------------

    suspend fun snapshot(shopName: String, currency: String): BackupFile = io {
        val r = db.readableDatabase
        val customers = ArrayList<BackupCustomer>()
        r.rawQuery("SELECT * FROM customers ORDER BY id", null).use { c ->
            while (c.moveToNext()) customers += BackupCustomer(
                c.long("id"), c.str("name"), c.str("phone"), c.longOrNull("due_at"), c.long("created_at")
            )
        }
        val txns = ArrayList<BackupTxn>()
        r.rawQuery("SELECT * FROM txns ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val t = c.toTxn()
                txns += BackupTxn(t.id, t.customerId, t.type.code, t.amountMinor, t.note, t.occurredAt, t.createdAt)
            }
        }
        val cash = ArrayList<BackupCash>()
        r.rawQuery("SELECT * FROM cash_entries ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                val e = c.toCash()
                cash += BackupCash(e.id, e.type.code, e.amountMinor, e.note, e.occurredAt, e.createdAt)
            }
        }
        BackupFile(
            exportedAt = System.currentTimeMillis(),
            shopName = shopName,
            currency = currency,
            customers = customers,
            txns = txns,
            cash = cash,
        )
    }

    /** Replaces ALL data with the backup contents, in a single transaction. */
    suspend fun restore(b: BackupFile) = io {
        val w = db.writableDatabase
        w.beginTransaction()
        try {
            w.delete("txns", null, null)
            w.delete("customers", null, null)
            w.delete("cash_entries", null, null)
            for (c in b.customers) {
                val v = customerValues(c.name, c.phone, c.dueAt, c.createdAt).apply { put("id", c.id) }
                w.insertOrThrow("customers", null, v)
            }
            for (t in b.txns) {
                w.insertOrThrow("txns", null, ContentValues().apply {
                    put("id", t.id); put("customer_id", t.customerId); put("type", t.type)
                    put("amount", t.amountMinor); put("note", t.note)
                    put("occurred_at", t.occurredAt); put("created_at", t.createdAt)
                })
            }
            for (e in b.cash) {
                w.insertOrThrow("cash_entries", null, ContentValues().apply {
                    put("id", e.id); put("type", e.type); put("amount", e.amountMinor); put("note", e.note)
                    put("occurred_at", e.occurredAt); put("created_at", e.createdAt)
                })
            }
            w.setTransactionSuccessful()
        } finally {
            w.endTransaction()
        }
        changed()
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}

private fun android.database.Cursor.toTxn() = Txn(
    id = long("id"),
    customerId = long("customer_id"),
    type = TxnType.of(getInt(getColumnIndexOrThrow("type"))),
    amountMinor = long("amount"),
    note = str("note"),
    occurredAt = long("occurred_at"),
    createdAt = long("created_at"),
)

private fun android.database.Cursor.toCash() = CashEntry(
    id = long("id"),
    type = CashType.of(getInt(getColumnIndexOrThrow("type"))),
    amountMinor = long("amount"),
    note = str("note"),
    occurredAt = long("occurred_at"),
    createdAt = long("created_at"),
)
