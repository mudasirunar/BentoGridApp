package com.example.bentoapp.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        private val COLLAPSED_PROJECT_IDS_KEY = stringSetPreferencesKey("collapsed_project_ids")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(modeString)
    }

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_LOCK_KEY] ?: false
    }

    val collapsedProjectIds: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        val rawSet = preferences[COLLAPSED_PROJECT_IDS_KEY] ?: emptySet()
        rawSet.mapNotNull { it.toIntOrNull() }.toSet()
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

    suspend fun setProjectCollapsed(projectId: Int, isCollapsed: Boolean) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[COLLAPSED_PROJECT_IDS_KEY] ?: emptySet()
            val newSet = currentSet.toMutableSet()
            if (isCollapsed) {
                newSet.add(projectId.toString())
            } else {
                newSet.remove(projectId.toString())
            }
            preferences[COLLAPSED_PROJECT_IDS_KEY] = newSet
        }
    }
}
