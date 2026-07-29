package com.fdzaki.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "promptvault_onboarding")

/** Tracks whether the first-launch onboarding walkthrough has been completed. */
class OnboardingRepository(private val context: Context) {

    private val completedKey = booleanPreferencesKey("onboarding_completed")

    val isCompleted: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[completedKey] ?: false
    }

    suspend fun markCompleted() {
        context.onboardingDataStore.edit { prefs -> prefs[completedKey] = true }
    }
}
