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
    private val useAltThemeKey = booleanPreferencesKey("use_alt_theme")

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

        /**
         * [Fitur baru, 2026-08-15] Toggle tema di Pengaturan -- SATU switch
         * ON/OFF antara 2 preset TETAP (bukan color picker bebas, sesuai
         * pilihan eksplisit user lewat pertanyaan klarifikasi). `false`
         * (default) = Deep Navy + Brass (v7.0.0, current). `true` = preset
         * alternatif "Charcoal + Copper" (BARU, dirancang sesi ini --
         * BUKAN mengembalikan palet lama manapun, hex Platinum/Ruby v6.0.0
         * sudah dihapus total & TIDAK tercatat presisi di mana pun utk
         * direkonstruksi dgn aman, lihat Color.kt utk detail & perhitungan
         * WCAG lengkap kedua preset).
         *
         * [Pelajaran v2.16.0, WAJIB dihormati] `ThemeMode` lama (SYSTEM/
         * LIGHT/DARK) DIHAPUS TOTAL krn togglenya TIDAK PERNAH benar-benar
         * mengubah apa pun (`PromptVaultTheme` hardcode 1 skema, parameter
         * diabaikan) -- UI yang "berbohong". Toggle INI BEDA: `PromptVaultTheme`
         * (Theme.kt) SEKARANG benar-benar `@Composable` yang membaca
         * parameter ini secara reaktif & memilih `ColorScheme` berbeda --
         * diverifikasi manual di setiap file yang disentuh batch ini,
         * BUKAN switch UI kosong seperti insiden v2.16.0.
         */
        const val DEFAULT_USE_ALT_THEME = false
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
        context.promptVaultDataStore.edit { prefs -> prefs[safTreeUriKey] = uri }
    }

    suspend fun clearSafTreeUri() {
        context.promptVaultDataStore.edit { prefs -> prefs.remove(safTreeUriKey) }
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

    /** Lihat dokumentasi lengkap di [DEFAULT_USE_ALT_THEME]. */
    val useAltThemeFlow: Flow<Boolean> = context.promptVaultDataStore.data.map { prefs ->
        prefs[useAltThemeKey] ?: DEFAULT_USE_ALT_THEME
    }

    suspend fun getUseAltTheme(): Boolean = useAltThemeFlow.first()

    suspend fun setUseAltTheme(value: Boolean) {
        context.promptVaultDataStore.edit { prefs -> prefs[useAltThemeKey] = value }
    }
}
