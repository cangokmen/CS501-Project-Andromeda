package com.example.watch.presentation.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore instance for the watch app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "watch_settings")

class WatchDataRepository(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        // Key for storing the set of selected question IDs
        val SELECTED_QUESTIONS_KEY = stringSetPreferencesKey("selected_questions")
        // Default questions to use if none are saved
        private val DEFAULT_QUESTIONS = setOf("DIET", "ACTIVITY", "SLEEP")
    }

    // A Flow that emits the set of selected questions whenever they change
    val selectedQuestions: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[SELECTED_QUESTIONS_KEY] ?: DEFAULT_QUESTIONS
    }

    // A suspend function to save a new set of questions
    suspend fun saveSelectedQuestions(questions: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SELECTED_QUESTIONS_KEY] = questions
        }
    }
}
