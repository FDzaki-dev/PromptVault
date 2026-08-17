package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Apa yang terjadi kalau file tujuan sudah ada nama yang sama persis. */
enum class ConflictStrategy {
    RENAME,     // default lama: tambah _1, _2, dst
    SKIP,       // biarkan file di Downloads, jangan dipindah
    OVERWRITE   // timpa file yang ada di tujuan (destruktif, tidak bisa di-undo file lamanya)
}

/**
 * Menyimpan interval auto-scan dan strategi konflik nama file.
 *
 * v2.16.0 -- `ThemeMode` (SYSTEM/LIGHT/DARK) DIHAPUS TOTAL (technical debt
 * closure). Sejak tema di-override ke AMOLED Glassmorphism Hybrid (v2.14.0),
 * `PromptVaultTheme` sudah HARDCODE satu skema gelap -- `darkTheme` di sana
 * cuma parameter mati yang selalu diabaikan. Opsi "Terang"/"Ikuti Sistem" di
 * Pengaturan TIDAK PERNAH benar-benar mengubah tampilan sejak saat itu (known
 * limitation yang tercatat di PROJECT_STATE.md). Daripada terus dibiarkan
 * sebagai UI yang berbohong ke user, opsinya dihapus sampai ke akar -- kalau
 * suatu saat mode terang beneran diminta lagi, itu FITUR BARU (implementasi
 * ulang dari nol di Theme.kt + Color.kt), bukan "mengaktifkan lagi" kode ini.
 */
class SettingsRepository(private val context: Context) {

    private val intervalKey = intPreferencesKey("auto_scan_interval_minutes")
    private val conflictKey = stringPreferencesKey("conflict_strategy")
    private val safTreeUriKey = stringPreferencesKey("saf_tree_uri")
    private val scanConcurrencyKey = intPreferencesKey("scan_concurrency")
    // [Fitur baru 2026-08-17, integrasi Shizuku] Path filesystem absolut
    // (BUKAN content:// URI -- Shizuku bypass SAF sepenuhnya) folder tujuan
    // kustom via Shizuku. `useShizukuKey` menentukan mode ini AKTIF atau
    // tidak -- kalau true, FileSorter SAMA SEKALI tidak menyentuh cabang SAF
    // lama (lihat FileSorter.scanAndSort), jadi kedua mode ini SALING
    // EKSKLUSIF by design (bukan campur/prioritas implisit).
    private val shizukuDestPathKey = stringPreferencesKey("shizuku_dest_path")
    private val useShizukuKey = booleanPreferencesKey("use_shizuku")

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 15
        val ALLOWED_INTERVALS = listOf(15, 30, 60, 120, 240)
        val DEFAULT_CONFLICT_STRATEGY = ConflictStrategy.RENAME

        /**
         * [Technical debt #4 di PROJECT_STATE.md, dieksekusi 2026-08-13 atas
         * instruksi eksplisit user] `SCAN_CONCURRENCY` dulunya konstanta mati
         * `private const val SCAN_CONCURRENCY = 6` di FileSorter.kt (v2.4.0),
         * DICATAT sebagai "asumsi teknis AI, belum divalidasi profiling nyata,
         * belum configurable" -- sengaja TIDAK diubah waktu itu karena tidak
         * ada data profiling utk pilih angka lain yang lebih benar (ganti
         * tebakan dengan tebakan lain = tidak ada gunanya).
         *
         * Fix ini TIDAK mengklaim akhirnya ada data profiling (tetap tidak
         * ada) -- yang berubah HANYA "belum configurable" jadi "configurable".
         * `DEFAULT_SCAN_CONCURRENCY` tetap 6 (nilai lama, PERILAKU DEFAULT
         * TIDAK BERUBAH utk siapa pun yang tidak pernah membuka setting ini --
         * nol regresi utk mayoritas user). User yang device-nya kelas
         * atas/Downloads-nya berisi ribuan file sekarang BISA menaikkan
         * sendiri tanpa perlu rilis baru; yang device-nya lemah bisa
         * menurunkan kalau scan terasa berat. `ALLOWED_SCAN_CONCURRENCY`
         * dibatasi 2..12 (bukan bebas/unbounded): di bawah 2 nyaris
         * menghilangkan manfaat paralelisme yang jadi alasan fitur ini ada
         * (v2.4.0), di atas 12 berisiko membuka terlalu banyak file handle/
         * RandomAccessFile bersamaan di HP kelas bawah (alasan asli angka 6
         * dipilih, lihat komentar lama di FileSorter.kt) -- rentang ini
         * MEMBATASI risiko tanpa perlu data profiling utk menentukan batas
         * amannya (batas atas & bawah masuk akal secara teknis, bukan cuma
         * tebakan sembarang).
         */
        const val DEFAULT_SCAN_CONCURRENCY = 6
        val ALLOWED_SCAN_CONCURRENCY = listOf(2, 4, 6, 8, 12)

        // v8.0.0 -- Toggle tema `useAltTheme` (2026-08-15, 2 preset tetap)
        // DIHAPUS TOTAL bersama seluruh key/flow/getter/setter-nya (lihat
        // Theme.kt/Color.kt): rombak total ke "default Material 3 murni" =
        // SATU ColorScheme baku, bukan lagi toggle antar 2 preset kustom.
    }

    val intervalMinutesFlow: Flow<Int> = context.promptVaultDataStore.data.map { prefs ->
        prefs[intervalKey] ?: DEFAULT_INTERVAL_MINUTES
    }

    suspend fun getIntervalMinutes(): Int = intervalMinutesFlow.first()

    suspend fun setIntervalMinutes(minutes: Int) {
        val safe = if (minutes in ALLOWED_INTERVALS) minutes else DEFAULT_INTERVAL_MINUTES
        context.promptVaultDataStore.edit { prefs -> prefs[intervalKey] = safe }
    }

    val conflictStrategyFlow: Flow<ConflictStrategy> = context.promptVaultDataStore.data.map { prefs ->
        runCatching { ConflictStrategy.valueOf(prefs[conflictKey] ?: "") }.getOrDefault(DEFAULT_CONFLICT_STRATEGY)
    }

    suspend fun getConflictStrategy(): ConflictStrategy = conflictStrategyFlow.first()

    suspend fun setConflictStrategy(strategy: ConflictStrategy) {
        context.promptVaultDataStore.edit { prefs -> prefs[conflictKey] = strategy.name }
    }

    /**
     * [SAF, syarat (c) Insiden #7] URI folder TUJUAN kustom (tree URI dari
     * ACTION_OPEN_DOCUMENT_TREE), disimpan sebagai String biar reuse
     * DataStore yang sama seperti setting lain -- tidak butuh tabel/skema
     * baru. [Klarifikasi peran, 2026-08-13, SAF_FINAL_VERDICT_FIX.txt] URI
     * ini HANYA menentukan KE MANA hasil sortir ditulis -- SUMBER scan tetap
     * SELALU Downloads, tidak pernah folder ini (lihat [FileSorter.scanAndSort]).
     * `null` = belum pernah diset ATAU sudah dikosongkan user
     * ([clearSafTreeUri]) -> [FileSorter] pakai Downloads/PromptVault biasa
     * sebagai tujuan.
     */
    val safTreeUriFlow: Flow<String?> = context.promptVaultDataStore.data.map { prefs -> prefs[safTreeUriKey] }

    suspend fun getSafTreeUri(): String? = safTreeUriFlow.first()

    suspend fun setSafTreeUri(uri: String) {
        context.promptVaultDataStore.edit { prefs ->
            if (prefs[safTreeUriKey] != uri) prefs.remove(safFolderCacheKey) // root beda -> cache lama tidak valid
            prefs[safTreeUriKey] = uri
        }
    }

    suspend fun clearSafTreeUri() {
        context.promptVaultDataStore.edit { prefs ->
            prefs.remove(safTreeUriKey)
            prefs.remove(safFolderCacheKey) // cache folder di bawah root lama jadi tidak relevan
        }
    }

    /**
     * [Fix duplikat folder "PromptVault"/"PromptVault (N)" berulang, 2026-08-16]
     * ROOT CAUSE BARU (beda dari race-fix 2026-08-13 yang sudah menyerialkan
     * pembuatan folder DALAM satu scan): `DocumentFile.findFile(name)` di
     * [FileSorter.findOrCreateChildDirSaf] melakukan `listFiles()` (query
     * cursor children) ULANG setiap scan dipanggil -- pada sebagian provider
     * (cache FUSE/indexing OEM tertentu), listing ini bisa STALE sesaat
     * setelah `createDirectory()` sukses di scan sebelumnya. Scan berikutnya
     * (mis. AutoSortWorker periodik, TETAP serial berkat `scanMutex` --
     * ini BUKAN race antar-coroutine) query listing, tidak melihat folder
     * yang SUDAH ADA secara fisik, lalu memanggil `createDirectory()` lagi --
     * provider mendeteksi tabrakan nama di level FILESYSTEM (bukan di level
     * listing yang stale tadi) -> auto-suffix "(1)", "(2)", dst. Pola ini
     * konsisten dgn laporan user: beberapa folder "PromptVault"/"(N)" utuh
     * (bukan 1 file terpecah spt bug lama), tiap folder isinya SET LENGKAP
     * subfolder rule -- tiap kemunculan adalah 1 scan yang gagal menemukan
     * root lama, bukan 1 file yang salah folder.
     *
     * Fix: cache `Uri` folder hasil resolve (root "PromptVault" + tiap
     * subfolder rule) di sini, key = path relatif thd `safTreeUri` saat ini.
     * Scan berikutnya resolve LANGSUNG lewat `DocumentFile.fromSingleUri()`
     * (query 1 dokumen spesifik by Uri) -- BUKAN query listing by-nama lagi
     * -- baru fallback ke `findFile()`/`createDirectory()` kalau cache
     * kosong/URI ternyata sudah tidak valid. Cache otomatis dibuang saat
     * `safTreeUri` berubah/dihapus ([setSafTreeUri]/[clearSafTreeUri]) --
     * mencegah cache "nyasar" nunjuk folder di root lama.
     */
    private val safFolderCacheKey = stringPreferencesKey("saf_folder_uri_cache")

    suspend fun getCachedFolderUri(relativePath: String): String? {
        val raw = context.promptVaultDataStore.data.map { prefs -> prefs[safFolderCacheKey] }.first() ?: return null
        return raw.lineSequence()
            .map { line -> line.split("::", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == relativePath }
            ?.get(1)
    }

    suspend fun setCachedFolderUri(relativePath: String, uri: String) {
        context.promptVaultDataStore.edit { prefs ->
            val existing = prefs[safFolderCacheKey].orEmpty()
            val filtered = existing.lineSequence().filterNot { it.startsWith("$relativePath::") }
            prefs[safFolderCacheKey] = (filtered + "$relativePath::$uri").joinToString("\n")
        }
    }

    /** Lihat dokumentasi lengkap di [DEFAULT_SCAN_CONCURRENCY]/[ALLOWED_SCAN_CONCURRENCY]. */
    val scanConcurrencyFlow: Flow<Int> = context.promptVaultDataStore.data.map { prefs ->
        prefs[scanConcurrencyKey]?.takeIf { it in ALLOWED_SCAN_CONCURRENCY } ?: DEFAULT_SCAN_CONCURRENCY
    }

    suspend fun getScanConcurrency(): Int = scanConcurrencyFlow.first()

    /** Nilai di luar [ALLOWED_SCAN_CONCURRENCY] diam-diam jatuh ke default -- pola sama seperti [setIntervalMinutes]. */
    suspend fun setScanConcurrency(value: Int) {
        val safe = if (value in ALLOWED_SCAN_CONCURRENCY) value else DEFAULT_SCAN_CONCURRENCY
        context.promptVaultDataStore.edit { prefs -> prefs[scanConcurrencyKey] = safe }
    }

    /**
     * [Fitur baru 2026-08-17, integrasi Shizuku] Lihat dokumentasi lengkap
     * di [shizukuDestPathKey]/[useShizukuKey] di atas.
     *
     * **PERINGATAN WAJIB DIBACA USER (permintaan eksplisit 2026-08-17)**:
     * path di sini HARUS berupa folder yang SUDAH ADA secara fisik di
     * storage device -- app INI TIDAK PERNAH membuat folder root secara
     * otomatis lewat Shizuku (persis pelajaran yang sama dengan folder
     * tujuan kustom SAF sejak v7.2.0, lihat [FileSorter.resolveSafRuleDestinations]).
     * User WAJIB membuat folder itu sendiri lewat file manager SEBELUM
     * mengisi path ini -- kalau belum ada, [FileSorter] akan MENOLAK scan
     * dengan pesan error eksplisit, BUKAN membuatkannya diam-diam.
     */
    val shizukuDestPathFlow: Flow<String?> = context.promptVaultDataStore.data.map { prefs -> prefs[shizukuDestPathKey] }

    suspend fun getShizukuDestPath(): String? = shizukuDestPathFlow.first()

    suspend fun setShizukuDestPath(path: String) {
        context.promptVaultDataStore.edit { prefs -> prefs[shizukuDestPathKey] = path.trim() }
    }

    suspend fun clearShizukuDestPath() {
        context.promptVaultDataStore.edit { prefs -> prefs.remove(shizukuDestPathKey) }
    }

    /** `true` = tujuan kustom lewat Shizuku aktif (mengesampingkan cabang SAF lama sepenuhnya). Default `false`, nol regresi untuk user yang tidak pernah menyentuh fitur ini. */
    val useShizukuFlow: Flow<Boolean> = context.promptVaultDataStore.data.map { prefs -> prefs[useShizukuKey] ?: false }

    suspend fun getUseShizuku(): Boolean = useShizukuFlow.first()

    suspend fun setUseShizuku(value: Boolean) {
        context.promptVaultDataStore.edit { prefs -> prefs[useShizukuKey] = value }
    }
}
