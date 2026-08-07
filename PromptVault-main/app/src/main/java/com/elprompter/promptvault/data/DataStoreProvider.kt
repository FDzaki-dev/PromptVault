package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.promptVaultDataStore by preferencesDataStore(name = "prompt_vault_store")
