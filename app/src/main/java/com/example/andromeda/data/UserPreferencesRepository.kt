package com.example.andromeda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a separate DataStore for user preferences
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        // Key to store the dark theme preference. A null value means use system default.
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    // Flow to get the dark theme preference
    val isDarkTheme: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            // Default to false (light theme) if not set
            preferences[PreferencesKeys.IS_DARK_THEME] ?: false
        }

    // Function to save the dark theme preference
    suspend fun saveThemePreference(isDarkTheme: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDarkTheme
        }
    }
}
