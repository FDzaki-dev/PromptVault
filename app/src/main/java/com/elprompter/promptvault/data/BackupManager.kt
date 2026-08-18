package com.elprompter.promptvault.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [Fitur baru 2026-08-18, permintaan eksplisit user -- "restore data setelah
 * uninstall/reinstall"] Rule (DataStore) dan ActivityLog/MoveHistory (Room,
 * lihat AppDatabase.kt) SEMUANYA disimpan di storage PRIVAT app -- uninstall
 * menghapus total isinya (perilaku standar Android, bukan bug). Folder tujuan
 * kustom SAF (di luar sandbox app) TIDAK ikut terhapus, tapi sebelum fitur
 * ini TIDAK ADA salinan rule/log di sana -- user yang install ulang & pilih
 * folder SAF yang SAMA PERSIS akan melihat folder "PromptVault" lama dengan
 * file-file lama, TAPI rule/log/riwayat undo-nya kosong sama sekali.
 *
 * Scope SENGAJA dibatasi ke folder tujuan kustom SAF SAJA (bukan Downloads
 * default, bukan mode Shizuku) -- SAF SATU-SATUNYA jalur yang punya langkah
 * eksplisit "user memilih folder" (ActivityResultContracts.OpenDocumentTree),
 * titik natural untuk deteksi "folder ini pernah dipakai sebelumnya".
 * Downloads default tidak pernah "dipilih" (selalu path tetap), dan mode
 * Shizuku SENGAJA butuh folder yang sudah ada dibuat manual (lihat KDoc
 * SettingsRepository.shizukuDestPathFlow) -- constraint desainnya sudah beda,
 * di luar scope batch ini.
 *
 * Cadangan ditulis sebagai SATU file JSON biasa (TERLIHAT, bukan disembunyikan)
 * di root folder "PromptVault" sendiri -- transparan, user bisa lihat/hapus
 * manual lewat file manager, semangatnya sama seperti fitur Export JSON manual
 * yang sudah ada (RuleRepository.exportAsJson) tapi otomatis & mencakup log +
 * riwayat juga (bukan cuma rule).
 *
 * Catatan batas jujur (dicatat di sini, bukan disembunyikan dari user):
 * [MoveHistoryEntry.originalParentUri]/[destUri] hasil restore menunjuk ke
 * Uri/permission instalasi LAMA -- Android mencabut SEMUA persistable URI
 * permission app saat di-uninstall. Entri lama akan tetap muncul di riwayat
 * (nilai audit/histori tetap ada), tapi tombol Undo untuk entri SEBELUM
 * reinstall bisa gagal dengan pesan error biasa (folder API sudah tidak
 * berizin) -- FileSorter.undo() SUDAH menangani kegagalan generik semacam
 * ini (bukan crash), jadi tidak butuh penanganan baru.
 */
@Serializable
data class BackupSnapshot(
    val schemaVersion: Int = 1,
    val savedAtMillis: Long,
    val rules: List<Rule>,
    val activityLog: List<ActivityLogEntry>,
    val moveHistory: List<MoveHistoryEntry>
)

/** Hasil deteksi folder root SAF yang baru dipilih user, SEBELUM disimpan sebagai folder aktif. */
sealed class FolderBackupDetection {
    /** Folder "PromptVault" belum ada di lokasi ini -- alur normal, tidak perlu dialog apa pun. */
    object NoPriorRoot : FolderBackupDetection()
    /** Folder "PromptVault" sudah ada tapi TIDAK ada file cadangan (mis. dibuat versi app sebelum fitur ini ada) -- tidak ada yang bisa dipulihkan, alur normal. */
    object RootExistsNoBackup : FolderBackupDetection()
    /** Ditemukan cadangan valid -- tampilkan dialog konfirmasi ke user. */
    data class BackupFound(val snapshot: BackupSnapshot) : FolderBackupDetection()
}

class BackupManager(
    private val context: Context,
    private val ruleRepository: RuleRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val moveHistoryRepository: MoveHistoryRepository
) {
    companion object {
        // Duplikasi SENGAJA dari FileSorter.SAF_ROOT_FOLDER_NAME (private di
        // sana, protected asset) -- literal "PromptVault" sudah jadi kontrak
        // stabil di banyak tempat (string UI onboarding, dll), lebih aman
        // direplikasi di sini daripada mengubah visibility FileSorter hanya
        // demi 1 pemakai baru.
        private const val ROOT_FOLDER_NAME = "PromptVault"
        private const val BACKUP_FILE_NAME = "promptvault_backup.json"
        private const val BACKUP_MIME = "application/json"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** Dipanggil PERSIS setelah user memilih tree URI baru lewat SAF picker, SEBELUM disimpan sbg folder aktif. */
    suspend fun detect(treeUri: Uri): FolderBackupDetection {
        val treeRoot = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
            ?: return FolderBackupDetection.NoPriorRoot
        val rootDir = runCatching { treeRoot.listFiles() }.getOrNull()
            ?.firstOrNull { it.isDirectory && it.name == ROOT_FOLDER_NAME }
            ?: return FolderBackupDetection.NoPriorRoot
        val backupFile = runCatching { rootDir.listFiles() }.getOrNull()
            ?.firstOrNull { it.isFile && it.name == BACKUP_FILE_NAME }
            ?: return FolderBackupDetection.RootExistsNoBackup
        val text = runCatching {
            context.contentResolver.openInputStream(backupFile.uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull() ?: return FolderBackupDetection.RootExistsNoBackup
        val snapshot = runCatching { json.decodeFromString<BackupSnapshot>(text) }.getOrNull()
            ?: return FolderBackupDetection.RootExistsNoBackup
        return FolderBackupDetection.BackupFound(snapshot)
    }

    /** Gabungkan (merge by id) isi cadangan ke storage privat app saat ini. Rule lewat jalur import yang sudah ada; log & riwayat lewat insert-ignore (lihat DAO). */
    suspend fun restore(snapshot: BackupSnapshot) {
        if (snapshot.rules.isNotEmpty()) {
            ruleRepository.importFromJson(json.encodeToString(snapshot.rules))
        }
        if (snapshot.activityLog.isNotEmpty()) {
            activityLogRepository.restoreEntries(snapshot.activityLog)
        }
        if (snapshot.moveHistory.isNotEmpty()) {
            moveHistoryRepository.restoreEntries(snapshot.moveHistory)
        }
    }

    /**
     * Tulis ulang cadangan TERKINI ke root folder SAF yang sedang aktif.
     * Dipanggil di titik checkpoint saja (bukan tiap 1 perubahan kecil --
     * pola sama seperti TRIM_CHECK_INTERVAL di ActivityLogRepository/
     * MoveHistoryRepository, alasan sama: I/O SAF per baris log akan jadi
     * bottleneck saat scan paralel Semaphore(N)): setelah scan SAF berhasil
     * (lihat FileSorter.scanAndSortToDestination), dan setelah perubahan
     * rule tersimpan dari layar Pengaturan/Rule (lihat MainViewModel).
     *
     * Selalu hapus file lama lalu buat baru (bukan andalkan truncate-write)
     * -- pola paling aman lintas provider SAF, konsisten dengan tidak
     * adanya pemakaian mode "wt" di FileSorter.copyDocumentBytes.
     *
     * runCatching membungkus SELURUH fungsi: backup BUKAN fitur kritis,
     * kegagalannya tidak boleh menggagalkan scan/simpan rule utama.
     */
    suspend fun writeBackup(rootDir: DocumentFile) {
        runCatching {
            val snapshot = BackupSnapshot(
                savedAtMillis = System.currentTimeMillis(),
                rules = ruleRepository.getRules(),
                activityLog = activityLogRepository.logFlow.first(),
                moveHistory = moveHistoryRepository.historyFlow.first()
            )
            val text = json.encodeToString(snapshot)
            rootDir.listFiles().firstOrNull { it.isFile && it.name == BACKUP_FILE_NAME }
                ?.let { old -> runCatching { old.delete() } }
            val target = rootDir.createFile(BACKUP_MIME, BACKUP_FILE_NAME) ?: return
            context.contentResolver.openOutputStream(target.uri)?.use { out ->
                out.write(text.toByteArray())
            }
        }
    }
}
