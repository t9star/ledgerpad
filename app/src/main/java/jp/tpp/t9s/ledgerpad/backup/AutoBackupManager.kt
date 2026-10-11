package jp.tpp.t9s.ledgerpad.backup

import android.content.Context
import jp.tpp.t9s.ledgerpad.data.LedgerRepository
import jp.tpp.t9s.ledgerpad.data.Prefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AutoBackupItem(
    val file: File,
    val timestampMs: Long,
    val sizeBytes: Long
)

/**
 * 100% Offline, Zero-Data-Usage Local Auto-Backup Manager.
 * Automatically saves rolling daily JSON snapshots (up to 7 recent days) in internal storage
 * whenever ledger transactions are recorded, allowing instant 1-tap recovery or sharing.
 */
object AutoBackupManager {

    private const val DIR_NAME = "auto_backups"
    private const val MAX_SNAPSHOTS = 7

    private fun getBackupDir(context: Context): File {
        return File(context.filesDir, DIR_NAME).apply { mkdirs() }
    }

    suspend fun triggerAutoBackupIfEnabled(
        context: Context,
        repository: LedgerRepository,
        prefs: Prefs
    ) {
        if (!prefs.autoBackupEnabled.value) return
        runCatching {
            val dir = getBackupDir(context)
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val targetFile = File(dir, "ledgerpad_auto_$dateKey.json")

            val snapshot = repository.snapshot(
                shopName = prefs.shopName.value,
                currency = prefs.currency.value
            )
            val json = BackupCodec.encodeJson(snapshot)
            targetFile.writeText(json)

            // Prune older snapshots beyond MAX_SNAPSHOTS
            val allFiles = dir.listFiles { f -> f.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            if (allFiles.size > MAX_SNAPSHOTS) {
                allFiles.drop(MAX_SNAPSHOTS).forEach { it.delete() }
            }
        }
    }

    fun listSnapshots(context: Context): List<AutoBackupItem> {
        val dir = getBackupDir(context)
        return dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                AutoBackupItem(
                    file = f,
                    timestampMs = f.lastModified(),
                    sizeBytes = f.length()
                )
            }
            ?: emptyList()
    }
}
