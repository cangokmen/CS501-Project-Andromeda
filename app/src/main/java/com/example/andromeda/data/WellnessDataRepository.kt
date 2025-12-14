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
import java.math.RoundingMode

// Extension property to create a DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wellness_data_store")

// --- CONVERSION CONSTANT ---
private const val KG_TO_LBS = 2.20462

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
     * Weight is assumed to be in the app's standard unit (kg).
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
     * Weight is assumed to be in the app's standard unit (kg).
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

    // --- NEW CONVERSION FUNCTION ---
    /**
     * Converts all historical weight data and the user's target weight to a new unit.
     */
    suspend fun convertAllWeightData(newUnit: String, authRepository: AuthRepository) {
        Log.d("WellnessDataRepo", "Converting all data to $newUnit")
        val userProfile = authRepository.getUserProfile() ?: return
        val oldUnit = userProfile.weightUnit

        if (oldUnit == newUnit) return // No conversion needed

        val conversionFactor = if (newUnit == "lbs") KG_TO_LBS else 1 / KG_TO_LBS

        // Convert historical data
        context.dataStore.edit { preferences ->
            val currentList = getMutableList(preferences)
            val convertedList = currentList.map { entry ->
                entry.copy(weight = (entry.weight * conversionFactor)
                    .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble())
            }
            preferences[wellnessDataListKey] = gson.toJson(convertedList)
        }

        // Convert user's target weight and update their profile
        val newTargetWeight = (userProfile.targetWeight * conversionFactor)
            .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()

        authRepository.createOrUpdateUserProfile(
            firstName = userProfile.firstName,
            lastName = userProfile.lastName,
            age = userProfile.age,
            targetWeight = newTargetWeight,
            weightUnit = newUnit
        )
        Log.d("WellnessDataRepo", "Data conversion complete.")
    }
    // --- END NEW FUNCTION ---


    /**
     * Deletes a single wellness data entry by its unique ID.
     */
    suspend fun deleteWellnessData(id: String) {
        Log.d("WellnessDataRepository", "Deleting entry with ID: $id")
        context.dataStore.edit { preferences ->
            val currentList = getMutableList(preferences)
            currentList.removeIf { it.id == id }
            preferences[wellnessDataListKey] = gson.toJson(currentList)
        }
    }

    /**
     * Retrieves a single wellness data entry by its unique ID.
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
