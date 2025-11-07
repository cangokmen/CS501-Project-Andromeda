package com.example.andromeda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

// Create a separate DataStore for user preferences
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val USE_BIGGER_TEXT = booleanPreferencesKey("use_bigger_text") // New key
    }

    // Flow to get the dark theme preference
    val isDarkTheme: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            // Default to false (light theme) if not set
            preferences[PreferencesKeys.IS_DARK_THEME] ?: false
        }

    // Flow to get the text size preference
    val useBiggerText: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_BIGGER_TEXT] ?: false // Default to normal text
        }

    // Function to save the dark theme preference
    suspend fun saveThemePreference(isDarkTheme: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDarkTheme
        }
    }

    // Function to save the text size preference
    suspend fun saveTextSizePreference(useBigger: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BIGGER_TEXT] = useBigger
        }
    }
}
