package com.example.andromeda.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wellness_data")

class WellnessDataRepository(private val context: Context) {

    private val gson = Gson()

    init {
        // This log will appear when the repository is first created.
        Log.d("WellnessDataRepository", "Repository has been initialized.")
    }

    // Define a key for storing the list of wellness data as a JSON string
    private val WELLNESS_DATA_LIST_KEY = stringPreferencesKey("wellness_data_list")

    // Flow to get all saved wellness entries
    val allWellnessData: Flow<List<WellnessData>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[WELLNESS_DATA_LIST_KEY] ?: "[]"
            // Define the type for deserialization
            val type = object : TypeToken<List<WellnessData>>() {}.type
            gson.fromJson(jsonString, type)
        }

    // Function to add a new wellness entry
    suspend fun addWellnessData(wellnessData: WellnessData) {
        // This is a high-visibility error log. If this function is called, you will see this.
        Log.d("WellnessDataRepository", "ADDING DATA: $wellnessData")
        context.dataStore.edit { preferences ->
            // Retrieve the current list
            val currentJson = preferences[WELLNESS_DATA_LIST_KEY] ?: "[]"
            val type = object : TypeToken<List<WellnessData>>() {}.type
            val currentList: MutableList<WellnessData> = gson.fromJson(currentJson, type)

            // Add the new entry
            currentList.add(wellnessData)

            // Save the updated list back to DataStore
            preferences[WELLNESS_DATA_LIST_KEY] = gson.toJson(currentList)
        }
    }

    /**
     * Deletes all wellness data from the DataStore.
     */
    suspend fun clearAllData() {
        Log.w("WellnessDataRepository", "CLEARING ALL WELLNESS DATA")
        context.dataStore.edit { preferences ->
            // Overwrite the existing data with an empty JSON array
            preferences[WELLNESS_DATA_LIST_KEY] = "[]"
        }
    }
}
