package com.elprompter.promptvault.data

import android.content.Context
import com.elprompter.promptvault.data.db.AppDatabase
import com.elprompter.promptvault.data.db.MoveHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
 */
class MoveHistoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).moveHistoryDao()

    companion object {
        private const val MAX_ENTRIES = 200
    }

    val historyFlow: Flow<List<MoveHistoryEntry>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun record(entry: MoveHistoryEntry) {
        dao.insert(entry.toEntity())
        dao.trimToMax(MAX_ENTRIES)
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
