package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Menyimpan interval auto-scan (TODO #2, sebelumnya hardcoded 15 menit).
 * WorkManager PeriodicWorkRequest punya batas minimum 15 menit, jadi pilihan
 * yang ditawarkan ke user selalu >= 15.
 */
class SettingsRepository(private val context: Context) {

    private val intervalKey = intPreferencesKey("auto_scan_interval_minutes")

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 15
        val ALLOWED_INTERVALS = listOf(15, 30, 60, 120, 240)
    }

    val intervalMinutesFlow: Flow<Int> = context.promptVaultDataStore.data.map { prefs ->
        prefs[intervalKey] ?: DEFAULT_INTERVAL_MINUTES
    }

    suspend fun getIntervalMinutes(): Int = intervalMinutesFlow.first()

    suspend fun setIntervalMinutes(minutes: Int) {
        val safe = if (minutes in ALLOWED_INTERVALS) minutes else DEFAULT_INTERVAL_MINUTES
        context.promptVaultDataStore.edit { prefs -> prefs[intervalKey] = safe }
    }
}
