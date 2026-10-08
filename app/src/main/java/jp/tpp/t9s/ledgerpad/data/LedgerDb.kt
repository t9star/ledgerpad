package jp.tpp.t9s.ledgerpad.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Plain SQLite (no Room / annotation processing) to keep the APK small and the build simple.
 * The schema is tiny: customers + txns.
 */
class LedgerDb(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                due_at INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE txns (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                type INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                occurred_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_txns_customer ON txns(customer_id, occurred_at)")
        db.execSQL(
            """
            CREATE TABLE cash_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                occurred_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_cash_occurred ON cash_entries(occurred_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 only. Future migrations go here.
    }

    companion object {
        const val NAME = "ledger.db"
        const val VERSION = 1
    }
}

internal fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""
internal fun Cursor.long(col: String): Long = getLong(getColumnIndexOrThrow(col))
internal fun Cursor.longOrNull(col: String): Long? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getLong(i)
}

internal fun customerValues(name: String, phone: String, dueAt: Long?, createdAt: Long? = null) =
    ContentValues().apply {
        put("name", name)
        put("phone", phone)
        if (dueAt == null) putNull("due_at") else put("due_at", dueAt)
        if (createdAt != null) put("created_at", createdAt)
    }
