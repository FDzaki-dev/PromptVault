package com.elprompter.promptvault.data

import android.content.Context
import com.elprompter.promptvault.data.db.ActivityLogEntity
import com.elprompter.promptvault.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Riwayat aktivitas PERMANEN.
 *
 * Sejak v2.2.0: backend disimpan di Room SQLite (sebelumnya JSON blob di
 * DataStore). Alasannya murni performa -- decode JSON ratusan/ribuan baris
 * setiap kali ada 1 entri baru jadi lambat & boros memori. API publik class
 * ini (logFlow, add, clear) TIDAK berubah sama sekali, jadi tidak ada
 * pemanggil (MainViewModel, FileSorter, AutoSortWorker) yang perlu disentuh.
 *
 * Catatan migrasi: riwayat log lama yang tersimpan di DataStore TIDAK
 * dipindahkan otomatis ke Room (disepakati tidak urgent, data ini bukan data
 * kritis pengguna). Log akan mulai kosong kembali setelah update ke versi ini.
 */
class ActivityLogRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).activityLogDao()

    companion object {
        private const val MAX_ENTRIES = 500
    }

    val logFlow: Flow<List<ActivityLogEntry>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun add(level: LogLevel, message: String) {
        dao.insert(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                timestampMillis = System.currentTimeMillis(),
                level = level,
                message = message
            )
        )
        dao.trimToMax(MAX_ENTRIES)
    }

    suspend fun clear() {
        dao.clearAll()
    }
}

private fun ActivityLogEntity.toDomain() = ActivityLogEntry(
    id = id,
    timestampMillis = timestampMillis,
    level = level,
    message = message
)
