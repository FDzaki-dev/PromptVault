package com.elprompter.promptvault.data

import android.content.Context
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

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 15
        val ALLOWED_INTERVALS = listOf(15, 30, 60, 120, 240)
        val DEFAULT_CONFLICT_STRATEGY = ConflictStrategy.RENAME
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
}
