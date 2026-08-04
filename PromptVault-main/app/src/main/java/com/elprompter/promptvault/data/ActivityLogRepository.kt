package com.elprompter.promptvault.data

import android.content.Context
import com.elprompter.promptvault.data.db.ActivityLogEntity
import com.elprompter.promptvault.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

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
 *
 * [perf v2.4.1] Trim SEKARANG BERKALA (tiap [TRIM_CHECK_INTERVAL] insert),
 * bukan tiap panggilan [add]. Sebelumnya `trimToMax()` (DELETE dengan
 * subquery ORDER BY + LIMIT, scan seluruh tabel) jalan di SETIAP insert --
 * saat scan v2.4.0 memproses banyak file paralel lewat Semaphore(6), tiap
 * kandidat bisa memicu 1+ log line, jadi ratusan trim beruntun yang saling
 * berebut write-lock SQLite (Room menyerialkan write transaction), justru
 * MENAHAN konkurensi yang baru dioptimasi di FileSorter. [insertCounter]
 * (AtomicInteger, di-reset tiap trim) aman dipanggil concurrent lintas
 * coroutine tanpa Mutex tambahan. Tabel boleh melebihi MAX_ENTRIES sampai
 * maksimal (TRIM_CHECK_INTERVAL - 1) baris di antara trim -- tidak terlihat
 * user, jauh lebih murah daripada trim tiap baris.
 */
class ActivityLogRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).activityLogDao()
    private val insertCounter = AtomicInteger(0)

    companion object {
        private const val MAX_ENTRIES = 500
        private const val TRIM_CHECK_INTERVAL = 20
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
        if (insertCounter.incrementAndGet() % TRIM_CHECK_INTERVAL == 0) {
            dao.trimToMax(MAX_ENTRIES)
        }
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
