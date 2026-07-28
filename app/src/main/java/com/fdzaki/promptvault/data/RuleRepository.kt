package com.fdzaki.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "promptvault_rules")

/**
 * Persists sort rules as "pattern||folderName||enabled" strings in a Preferences DataStore.
 * Kept intentionally simple (no Room) since rule count is small and offline-only.
 */
class RuleRepository(private val context: Context) {

    private val rulesKey = stringSetPreferencesKey("sort_rules")

    val rules: Flow<List<SortRule>> = context.dataStore.data.map { prefs ->
        val raw = prefs[rulesKey] ?: defaultRules()
        raw.mapNotNull { decode(it) }.sortedBy { it.pattern }
    }

    suspend fun saveRule(rule: SortRule) {
        context.dataStore.edit { prefs ->
            val current = (prefs[rulesKey] ?: defaultRules()).toMutableSet()
            current.removeAll { decode(it)?.pattern == rule.pattern }
            current.add(encode(rule))
            prefs[rulesKey] = current
        }
    }

    suspend fun deleteRule(pattern: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[rulesKey] ?: defaultRules()).toMutableSet()
            current.removeAll { decode(it)?.pattern == pattern }
            prefs[rulesKey] = current
        }
    }

    private fun encode(rule: SortRule): String =
        "${rule.pattern}||${rule.folderName}||${rule.enabled}"

    private fun decode(raw: String): SortRule? {
        val parts = raw.split("||")
        if (parts.size < 3) return null
        return SortRule(
            pattern = parts[0],
            folderName = parts[1],
            enabled = parts[2].toBooleanStrictOrNull() ?: true
        )
    }

    private fun defaultRules(): Set<String> = setOf(
        "AudioPlayer*||AudioPlayer||true",
        "GalleryCleaner*||GalleryCleaner||true",
        "PromptVault*||PromptVault||true"
    )
}
