package com.example.andromeda.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The DataStore is now only used for UI settings like theme and question choices.
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    // Keys are simplified, as they are no longer user-specific.
    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val USE_BIGGER_TEXT = booleanPreferencesKey("use_bigger_text")
        val SELECTED_QUESTIONS = stringSetPreferencesKey("selected_questions")
    }

    // --- UI Preference Flows ---

    val isDarkTheme: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.IS_DARK_THEME] ?: false }

    val useBiggerText: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.USE_BIGGER_TEXT] ?: false }

    val selectedQuestions: Flow<Set<String>> = context.userPreferencesDataStore.data
        .map { prefs ->
            // Provide a default set of questions if none are stored.
            val questions = prefs[PreferencesKeys.SELECTED_QUESTIONS]
            if (questions.isNullOrEmpty()) {
                setOf("DIET", "ACTIVITY", "SLEEP")
            } else {
                questions
            }
        }

    // --- UI Preference Save Functions ---

    suspend fun saveThemePreference(isDarkTheme: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_DARK_THEME] = isDarkTheme
        }
    }

    suspend fun saveTextSizePreference(useBigger: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.USE_BIGGER_TEXT] = useBigger
        }
    }

    suspend fun saveSelectedQuestions(questions: Set<String>) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_QUESTIONS] = questions
        }
    }

    // All previous auth-related functions (saveUserCredentials, checkCredentials, logoutUser, etc.)
    // have been completely removed.
}
