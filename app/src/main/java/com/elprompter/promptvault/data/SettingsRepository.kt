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
 * Menyimpan interval auto-scan (TODO #2, sebelumnya hardcoded 15 menit) dan
 * strategi konflik nama file saat tujuan sudah terisi.
 */
class SettingsRepository(private val context: Context) {

    private val intervalKey = intPreferencesKey("auto_scan_interval_minutes")
    private val conflictKey = stringPreferencesKey("conflict_strategy")

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
}
