package com.fdzaki.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.logDataStore by preferencesDataStore(name = "promptvault_logs")

/**
 * Persists sort activity history so "Recent Activity" survives app restarts,
 * instead of living only in in-memory Compose state (previous behavior).
 * Keeps only the most recent [MAX_ENTRIES] to bound storage size.
 */
class LogRepository(private val context: Context) {

    private val logsKey = stringSetPreferencesKey("sort_logs")

    val logs: Flow<List<SortLogEntry>> = context.logDataStore.data.map { prefs ->
        (prefs[logsKey] ?: emptySet())
            .mapNotNull { decode(it) }
            .sortedBy { it.timestampMillis }
    }

    suspend fun append(newEntries: List<SortLogEntry>) {
        if (newEntries.isEmpty()) return
        context.logDataStore.edit { prefs ->
            val current = (prefs[logsKey] ?: emptySet())
                .mapNotNull { decode(it) }
                .toMutableList()
            current.addAll(newEntries)
            val trimmed = current.sortedBy { it.timestampMillis }.takeLast(MAX_ENTRIES)
            prefs[logsKey] = trimmed.map { encode(it) }.toSet()
        }
    }

    suspend fun clear() {
        context.logDataStore.edit { prefs -> prefs[logsKey] = emptySet() }
    }

    private fun encode(entry: SortLogEntry): String =
        "${entry.timestampMillis}||${entry.fileName}||${entry.matchedPattern}||${entry.destinationFolder}"

    private fun decode(raw: String): SortLogEntry? {
        val parts = raw.split("||")
        if (parts.size < 4) return null
        val timestamp = parts[0].toLongOrNull() ?: return null
        return SortLogEntry(
            fileName = parts[1],
            matchedPattern = parts[2],
            destinationFolder = parts[3],
            timestampMillis = timestamp
        )
    }

    companion object {
        private const val MAX_ENTRIES = 200
    }
}
