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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property to create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wellness_data_store")

class WellnessDataRepository(private val context: Context) {

    private val gson = Gson()
    private val wellnessDataListKey = stringPreferencesKey("wellness_data_list_v2")

    // A flow that emits all saved wellness entries, sorted with the newest first.
    val allWellnessData: Flow<List<WellnessData>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[wellnessDataListKey] ?: "[]"
            val type = object : TypeToken<List<WellnessData>>() {}.type
            val list: List<WellnessData> = gson.fromJson(jsonString, type)
            list.sortedByDescending { it.timestamp }
        }

    /**
     * Adds a new wellness data entry to the list.
     */
    suspend fun addWellnessData(wellnessData: WellnessData) {
        Log.d("WellnessDataRepository", "Adding new data for date: ${wellnessData.timestamp}")
        context.dataStore.edit { preferences ->
            val currentList = getMutableList(preferences)
            currentList.add(wellnessData)
            preferences[wellnessDataListKey] = gson.toJson(currentList)
        }
    }

    /**
     * Updates an existing wellness data entry in the list.
     */
    suspend fun updateWellnessData(wellnessData: WellnessData) {
        Log.d("WellnessDataRepository", "Updating data for entry ID: ${wellnessData.id}")
        context.dataStore.edit { preferences ->
            val currentList = getMutableList(preferences)
            val index = currentList.indexOfFirst { it.id == wellnessData.id }
            if (index != -1) {
                currentList[index] = wellnessData
                preferences[wellnessDataListKey] = gson.toJson(currentList)
            } else {
                Log.w("WellnessDataRepository", "Update failed: Entry with ID ${wellnessData.id} not found.")
            }
        }
    }

    /**
     * Retrieves a single wellness data entry by its unique ID.
     * This function resolves the "Unresolved reference" error.
     */
    suspend fun getWellnessDataById(id: String): WellnessData? {
        val currentList = allWellnessData.first()
        return currentList.find { it.id == id }
    }

    /**
     * Deletes all wellness data from the DataStore.
     */
    suspend fun clearAllData() {
        Log.w("WellnessDataRepository", "CLEARING ALL WELLNESS DATA")
        context.dataStore.edit { preferences ->
            preferences[wellnessDataListKey] = "[]"
        }
    }

    /**
     * Helper function to retrieve and deserialize the current list from preferences.
     */
    private fun getMutableList(preferences: Preferences): MutableList<WellnessData> {
        val currentJson = preferences[wellnessDataListKey] ?: "[]"
        val type = object : TypeToken<MutableList<WellnessData>>() {}.type
        return gson.fromJson(currentJson, type)
    }
}
