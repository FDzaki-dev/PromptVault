package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elprompter.promptvault.data.db.ActivityLogEntity
import com.elprompter.promptvault.data.db.AppDatabase
import com.elprompter.promptvault.data.db.MoveHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * [Technical debt #3 di PROJECT_STATE.md, dieksekusi 2026-08-13 atas instruksi
 * eksplisit user "kerjakan tanpa regresi"] Sejak Batch 1 Room (v2.2.0),
 * `ActivityLogRepository`/`MoveHistoryRepository` pindah dari JSON blob di
 * DataStore ke Room SQLite -- data lama di DataStore SENGAJA TIDAK
 * dimigrasikan waktu itu (disepakati tidak urgent, lihat CHANGELOG v2.2.0).
 * Objek ini akhirnya mengerjakan migrasi itu, SATU KALI, best-effort.
 *
 * **PERINGATAN JUJUR (WAJIB dibaca sebelum menyentuh file ini lagi)**: kode
 * pre-v2.2.0 yang menulis ke DataStore itu SUDAH TERHAPUS TOTAL sejak v2.2.0
 * -- snapshot project ini juga TIDAK punya riwayat git (ZIP-only, tanpa folder
 * `.git`), jadi nama key literal DataStore yang dipakai era itu TIDAK BISA
 * diverifikasi langsung dari source lama. Key di bawah (`"activity_log_json"`,
 * `"move_history_json"`) adalah INFERENSI berdasar konvensi penamaan yang
 * konsisten dipakai project ini (lihat `RuleRepository.kt`, key
 * `"rules_json"` -- pola `{noun}_json` untuk blob DataStore), BUKAN nilai
 * yang dikonfirmasi dari source asli. `ActivityLogEntry`/`MoveHistoryEntry`
 * sendiri sudah `@Serializable` sejak awal (lihat file masing-masing) dengan
 * bentuk field IDENTIK ke entity Room sekarang (dikonfirmasi lewat komentar
 * `ActivityLogEntity`/`MoveHistoryEntity`: "Bentuknya sengaja identik dengan
 * domain model") -- jadi BENTUK JSON kemungkinan besar benar, yang tidak
 * pasti murni NAMA KEY-nya.
 *
 * Desain di bawah aman WALAU TEBAKAN KEY SALAH: kalau key tidak ditemukan
 * atau isinya bukan JSON yang valid, hasilnya nol baris termigrasi (no-op),
 * BUKAN crash atau data korup -- persis skenario "tidak menemukan apa-apa"
 * yang sudah terjadi setiap hari sejak v2.2.0, tidak ada yang berubah jadi
 * lebih buruk. Kalau nanti user melaporkan tab Log/Undo tetap kosong padahal
 * yakin pernah pakai versi sangat lama, itu sinyal key-nya perlu dikoreksi
 * (baru bisa dipastikan lewat sampel data nyata dari user tsb, bukan ditebak
 * ulang lagi tanpa data).
 *
 * Berjalan SEKALI SEUMUR INSTALL (guard flag [migrationDoneKey], di-set
 * `true` di blok `finally` APAPUN hasilnya -- termasuk kalau gagal/exception
 * -- supaya tidak jadi retry-loop tiap app dibuka kalau datanya memang
 * korup/tidak kompatibel). Dibungkus try-catch total (fail-safe): ini
 * pemulihan best-effort data historis TIDAK KRITIS, kalau gagal tidak ada
 * dampak ke user (tidak lebih buruk dari status quo v2.2.0-v2.20.1).
 */
object LegacyDataMigration {

    private val migrationDoneKey = booleanPreferencesKey("legacy_datastore_migration_done")
    private val json = Json { ignoreUnknownKeys = true }

    // Selaras manual dengan ActivityLogRepository.MAX_ENTRIES / MoveHistoryRepository.MAX_ENTRIES
    // (private di sana) -- cuma jaring pengaman batas atas 1x insert massal, bukan sumber
    // kebenaran; kalau nilai itu berubah, ketidaksamaan di sini tidak berbahaya (cuma pangkas
    // sedikit lebih longgar/ketat dari yang seharusnya, tidak menyebabkan bug fungsional).
    private const val MAX_LOG_ROWS = 500
    private const val MAX_HISTORY_ROWS = 200

    suspend fun runIfNeeded(context: Context) {
        try {
            val alreadyDone = context.promptVaultDataStore.data.first()[migrationDoneKey] ?: false
            if (alreadyDone) return

            migrateActivityLog(context)
            migrateMoveHistory(context)
        } catch (_: Exception) {
            // Best-effort, non-kritis -- lihat dokumentasi kelas ini. Sengaja tidak
            // dilempar ulang/dicatat ke ActivityLogRepository (bisa saja itu sendiri
            // yang belum siap kalau exception terjadi sangat awal di proses ini).
        } finally {
            // Ditandai selesai APAPUN hasilnya supaya tidak retry-loop tiap app dibuka.
            runCatching {
                context.promptVaultDataStore.edit { prefs -> prefs[migrationDoneKey] = true }
            }
        }
    }

    private suspend fun migrateActivityLog(context: Context) {
        val key = stringPreferencesKey("activity_log_json")
        val raw = context.promptVaultDataStore.data.first()[key] ?: return
        val entries = runCatching { json.decodeFromString<List<ActivityLogEntry>>(raw) }.getOrDefault(emptyList())
        if (entries.isEmpty()) return

        val dao = AppDatabase.getInstance(context).activityLogDao()
        entries.sortedByDescending { it.timestampMillis }.take(MAX_LOG_ROWS).forEach { entry ->
            dao.insert(
                ActivityLogEntity(
                    id = entry.id,
                    timestampMillis = entry.timestampMillis,
                    level = entry.level,
                    message = entry.message
                )
            )
        }
    }

    private suspend fun migrateMoveHistory(context: Context) {
        val key = stringPreferencesKey("move_history_json")
        val raw = context.promptVaultDataStore.data.first()[key] ?: return
        val entries = runCatching { json.decodeFromString<List<MoveHistoryEntry>>(raw) }.getOrDefault(emptyList())
        if (entries.isEmpty()) return

        val dao = AppDatabase.getInstance(context).moveHistoryDao()
        entries.sortedByDescending { it.timestampMillis }.take(MAX_HISTORY_ROWS).forEach { entry ->
            dao.insert(
                MoveHistoryEntity(
                    id = entry.id,
                    timestampMillis = entry.timestampMillis,
                    fileName = entry.fileName,
                    originalParentUri = entry.originalParentUri,
                    destUri = entry.destUri,
                    ruleFolderName = entry.ruleFolderName,
                    undone = entry.undone
                )
            )
        }
    }
}
