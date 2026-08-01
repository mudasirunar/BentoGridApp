package com.example.bentoapp.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class PreferenceManager(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val BIOMETRIC_LOCK_KEY = booleanPreferencesKey("biometric_lock_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(modeString)
    }

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_LOCK_KEY] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_LOCK_KEY] = enabled
        }
    }
}
