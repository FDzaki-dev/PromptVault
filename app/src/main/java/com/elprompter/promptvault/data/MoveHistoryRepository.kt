package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Riwayat pemindahan file, dasar dari fitur UNDO (TODO #1).
 */
class MoveHistoryRepository(private val context: Context) {

    private val key = stringPreferencesKey("move_history_json")
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val MAX_ENTRIES = 200
    }

    val historyFlow: Flow<List<MoveHistoryEntry>> = context.promptVaultDataStore.data.map { prefs ->
        val raw = prefs[key] ?: "[]"
        runCatching { json.decodeFromString<List<MoveHistoryEntry>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun record(entry: MoveHistoryEntry) {
        val current = historyFlow.first().toMutableList()
        current.add(0, entry)
        persist(current.take(MAX_ENTRIES))
    }

    suspend fun markUndone(entryId: String) {
        val current = historyFlow.first().map { if (it.id == entryId) it.copy(undone = true) else it }
        persist(current)
    }

    suspend fun getUndoableEntries(): List<MoveHistoryEntry> =
        historyFlow.first().filter { !it.undone }

    private suspend fun persist(entries: List<MoveHistoryEntry>) {
        context.promptVaultDataStore.edit { prefs -> prefs[key] = json.encodeToString(entries) }
    }
}
