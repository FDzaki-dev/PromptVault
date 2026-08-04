package com.elprompter.promptvault.data

import android.content.Context
import com.elprompter.promptvault.data.db.AppDatabase
import com.elprompter.promptvault.data.db.MoveHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicInteger

/**
 * Riwayat pemindahan file, dasar dari fitur UNDO.
 *
 * Sejak v2.2.0: backend disimpan di Room SQLite (sebelumnya JSON blob di
 * DataStore), dengan alasan yang sama seperti [ActivityLogRepository]. API
 * publik class ini (historyFlow, record, markUndone, getUndoableEntries)
 * TIDAK berubah, jadi FileSorter/MainViewModel/AutoSortWorker tetap sama.
 *
 * Catatan migrasi: riwayat undo lama di DataStore TIDAK dipindahkan otomatis
 * (disepakati tidak urgent). File yang sudah terlanjur dipindah SEBELUM
 * update ini tetap aman di lokasi barunya, hanya saja tidak lagi muncul di
 * tab "Undo Pemindahan" setelah update.
 *
 * [perf v2.4.1] Trim berkala, sama seperti [ActivityLogRepository] -- lihat
 * penjelasan lengkap di sana. `record()` dipanggil per file yang berhasil
 * dipindah selama scan paralel (Semaphore 6), jadi pola bottleneck-nya
 * identik dengan `add()` di log.
 */
class MoveHistoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).moveHistoryDao()
    private val insertCounter = AtomicInteger(0)

    companion object {
        private const val MAX_ENTRIES = 200
        private const val TRIM_CHECK_INTERVAL = 20
    }

    val historyFlow: Flow<List<MoveHistoryEntry>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun record(entry: MoveHistoryEntry) {
        dao.insert(entry.toEntity())
        if (insertCounter.incrementAndGet() % TRIM_CHECK_INTERVAL == 0) {
            dao.trimToMax(MAX_ENTRIES)
        }
    }

    suspend fun markUndone(entryId: String) {
        dao.markUndone(entryId)
    }

    suspend fun getUndoableEntries(): List<MoveHistoryEntry> =
        dao.getUndoable().map { it.toDomain() }
}

private fun MoveHistoryEntity.toDomain() = MoveHistoryEntry(
    id = id,
    timestampMillis = timestampMillis,
    fileName = fileName,
    originalParentUri = originalParentUri,
    destUri = destUri,
    ruleFolderName = ruleFolderName,
    undone = undone
)

private fun MoveHistoryEntry.toEntity() = MoveHistoryEntity(
    id = id,
    timestampMillis = timestampMillis,
    fileName = fileName,
    originalParentUri = originalParentUri,
    destUri = destUri,
    ruleFolderName = ruleFolderName,
    undone = undone
)
