package com.elprompter.promptvault.util

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.elprompter.promptvault.data.ActivityLogEntry
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.data.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [Fitur baru, 2026-08-18, permintaan eksplisit user] "Selamatkan uninstall":
 * uninstall Android menghapus TOTAL data privat app (DataStore Rules/
 * Settings + Room ActivityLog/MoveHistory) -- itu perilaku OS, TIDAK BISA
 * dihindari dari sisi app. SATU-SATUNYA data yang SELAMAT dari uninstall
 * adalah yang ditulis DI LUAR sandbox privat app -- yaitu di folder tujuan
 * kustom SAF (kalau dikonfigurasi). Root vault "PromptVault" di dalamnya
 * SUDAH punya mekanisme anti-duplikat yang matang sejak v7.5.0/v8.x
 * ([FileSorter.resolveCanonicalRootDirSaf], self-healing lewat regex+cache)
 * -- TIDAK disentuh/diulang di sini, sesuai pelajaran permanen Insiden #7
 * ("reuse jalur yang sama, jangan bikin implementasi kedua independen").
 *
 * File ini HANYA menangani lapisan BARU: manifest/cermin konfigurasi (rule,
 * setting relevan, log aktivitas, riwayat pemindahan) disimpan sbg 1 file
 * JSON tersembunyi ([BACKUP_FILE_NAME]) di root vault SAF yang SAMA. Kalau
 * user tidak sengaja uninstall lalu install ulang & memilih folder SAF yang
 * SAMA lagi (root vault "PromptVault" di dalamnya masih ada, berisi banyak
 * file lama), [FileSorter.peekVaultBackup] (dipanggil [MainViewModel])
 * mendeteksi file ini & menawarkan restore -- root folder TIDAK dibuat
 * ulang/duplikat, cukup dipakai lagi, DAN "rule, log, dsb" yang hilang saat
 * uninstall bisa dikembalikan tanpa user mengetik ulang dari nol.
 *
 * SENGAJA scope TERBATAS ke mode SAF saja (bukan mode Shizuku/path lokal
 * manual) -- Shizuku pakai path filesystem yang user ketik sendiri (bukan
 * picker folder), jadi tidak ada titik alami "pilih folder" utk memicu
 * deteksi ini.
 *
 * Class ini murni I/O serialisasi (baca/tulis 1 file JSON via DocumentFile)
 * + 2 fungsi pure logic ([isPayloadWorthOffering]/[countRules],
 * unit-testable). Orkestrasi domain (rule mana yg direstore, dsb) SENGAJA
 * ada di [FileSorter] ([FileSorter.applyVaultRestore]/
 * [FileSorter.syncConfigBackupToSaf]) -- FileSorter SUDAH punya akses ke
 * semua repository yang relevan, tidak perlu wiring dependency baru di sini.
 */
object VaultConfigBackup {

    /**
     * Nama diawali "." (konvensi hidden-file Unix/Android) -- tidak
     * mengganggu tampilan folder di file manager biasa & TIDAK PERNAH cocok
     * pattern rule manapun (semua glob rule berbasis nama file/ekstensi
     * biasa, bukan dotfile tersembunyi).
     */
    const val BACKUP_FILE_NAME = ".promptvault_config_backup.json"

    private const val SCHEMA_VERSION = 1
    private const val MAX_LOG_ENTRIES_IN_BACKUP = 200
    private const val MAX_HISTORY_ENTRIES_IN_BACKUP = 200

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Serializable
    data class Settings(
        val intervalMinutes: Int,
        val conflictStrategy: String,
        val scanConcurrency: Int
    )

    @Serializable
    data class Payload(
        val schemaVersion: Int = SCHEMA_VERSION,
        val savedAtEpochMillis: Long,
        val appVersionName: String,
        val rulesJson: String,
        val settings: Settings,
        val activityLog: List<ActivityLogEntry> = emptyList(),
        val moveHistory: List<MoveHistoryEntry> = emptyList()
    )

    fun buildPayload(
        appVersionName: String,
        rulesJson: String,
        intervalMinutes: Int,
        conflictStrategy: String,
        scanConcurrency: Int,
        log: List<ActivityLogEntry>,
        history: List<MoveHistoryEntry>
    ): Payload = Payload(
        savedAtEpochMillis = System.currentTimeMillis(),
        appVersionName = appVersionName,
        rulesJson = rulesJson,
        settings = Settings(intervalMinutes, conflictStrategy, scanConcurrency),
        activityLog = log.sortedByDescending { it.timestampMillis }.take(MAX_LOG_ENTRIES_IN_BACKUP),
        moveHistory = history.sortedByDescending { it.timestampMillis }.take(MAX_HISTORY_ENTRIES_IN_BACKUP)
    )

    /**
     * Pure logic (unit-testable, lihat VaultConfigBackupTest.kt) -- backup
     * yang "kosong" (belum pernah ada rule/log/riwayat sama sekali, mis.
     * folder baru yang cuma sempat 1x scan tanpa rule aktif) tidak perlu
     * ditawarkan ke user sbg restore, cuma bikin dialog tanpa nilai.
     */
    fun isPayloadWorthOffering(payload: Payload): Boolean {
        val hasRules = payload.rulesJson.isNotBlank() && payload.rulesJson.trim() !in EMPTY_RULES_JSON_MARKERS
        return hasRules || payload.activityLog.isNotEmpty() || payload.moveHistory.isNotEmpty()
    }

    /**
     * Pure logic (unit-testable) -- hitung jumlah rule dari `rulesJson`
     * mentah tanpa perlu ganti tipe [Payload.rulesJson] jadi `List<Rule>`
     * (tetap String biar bisa langsung dioper apa adanya ke
     * [com.elprompter.promptvault.data.RuleRepository.importFromJson],
     * konsisten dgn pola export/import yang sudah ada -- bukan skema baru).
     * `0` kalau parse gagal (jangan sampai UI crash gara-gara angka ringkasan).
     */
    fun countRules(rulesJson: String): Int =
        runCatching { json.decodeFromString<List<Rule>>(rulesJson).size }.getOrDefault(0)

    private val EMPTY_RULES_JSON_MARKERS = setOf("", "[]")

    /**
     * Overwrite sederhana (hapus lama kalau ada, tulis baru) -- BUKAN pola
     * temp-file-lalu-rename spt [FileSorter]'s `copyThenDelete` (fix P0-2,
     * v7.1.4). Beda kelas risiko SENGAJA: file di sana adalah data USER
     * (foto/dokumen, hilang = kerugian nyata), file INI cuma cermin/cache
     * konfigurasi milik app sendiri -- kalau proses sempat terganggu di
     * tengah jalan, paling parah backup LAMA hilang (bukan data asli user
     * yg rusak), dan akan ditulis ulang otomatis di sync berikutnya.
     * `false` = gagal -- SELALU ditelan diam-diam oleh pemanggil
     * ([FileSorter.syncConfigBackupToSaf]), best-effort, BUKAN gerbang yang
     * boleh menggagalkan scan/simpan rule utama.
     */
    suspend fun writeBackup(context: Context, vaultRootDoc: DocumentFile, payload: Payload): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
                vaultRootDoc.findFile(BACKUP_FILE_NAME)?.delete()
                val file = vaultRootDoc.createFile("application/json", BACKUP_FILE_NAME) ?: return@withContext false
                context.contentResolver.openOutputStream(file.uri)?.use { out -> out.write(bytes) } ?: return@withContext false
                true
            } catch (e: Exception) {
                false
            }
        }

    /**
     * `null` = tidak ada backup terbaca (folder baru/belum pernah dipakai
     * app ini SEBELUMNYA, ATAU file rusak/corrupt) -- caller
     * ([FileSorter.peekVaultBackup]) membaca `null` sbg "tidak ada tawaran
     * restore", BUKAN error yang perlu ditampilkan ke user.
     */
    suspend fun tryReadBackup(context: Context, vaultRootDoc: DocumentFile): Payload? =
        withContext(Dispatchers.IO) {
            try {
                // Fallback listFiles()-scan kalau findFile() (query by-nama
                // langsung) miss -- jaga-jaga provider sempat mengubah nama
                // file saat createFile() dulu (kewaspadaan sama dgn
                // findOrCreateChildDirSaf, walau konsekuensi di sini cuma
                // "gagal ketemu backup", bukan folder duplikat).
                val doc = vaultRootDoc.findFile(BACKUP_FILE_NAME)
                    ?: vaultRootDoc.listFiles().firstOrNull { it.name?.startsWith(BACKUP_FILE_NAME) == true }
                    ?: return@withContext null
                if (!doc.isFile) return@withContext null
                val text = context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext null
                json.decodeFromString<Payload>(text)
            } catch (e: Exception) {
                null
            }
        }
}
