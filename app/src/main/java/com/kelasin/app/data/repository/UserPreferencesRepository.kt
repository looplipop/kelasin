package com.kelasin.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")

    // 0 = System, 1 = Light, 2 = Dark
    val themeMode: Flow<Int> = context.userPrefsDataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: 0
    }

    suspend fun setThemeMode(mode: Int) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }
}
