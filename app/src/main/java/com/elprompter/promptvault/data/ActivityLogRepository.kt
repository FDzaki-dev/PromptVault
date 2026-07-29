package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Riwayat aktivitas PERMANEN (tersimpan lewat DataStore, tidak hilang saat app ditutup).
 * Dibatasi MAX_ENTRIES agar tidak tumbuh tanpa batas.
 */
class ActivityLogRepository(private val context: Context) {

    private val key = stringPreferencesKey("activity_log_json")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val MAX_ENTRIES = 500
    }

    val logFlow: Flow<List<ActivityLogEntry>> = context.promptVaultDataStore.data.map { prefs ->
        val raw = prefs[key] ?: "[]"
        runCatching { json.decodeFromString<List<ActivityLogEntry>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun add(level: LogLevel, message: String) {
        val current = logFlow.first().toMutableList()
        current.add(0, ActivityLogEntry(UUID.randomUUID().toString(), System.currentTimeMillis(), level, message))
        val trimmed = current.take(MAX_ENTRIES)
        context.promptVaultDataStore.edit { prefs -> prefs[key] = json.encodeToString(trimmed) }
    }

    suspend fun clear() {
        context.promptVaultDataStore.edit { prefs -> prefs[key] = "[]" }
    }
}
