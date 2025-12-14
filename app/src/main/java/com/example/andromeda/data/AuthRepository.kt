package com.example.andromeda.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_store")

class AuthRepository(private val application: Application) {

    private val gson = Gson()
    private val context = application.applicationContext

    // --- ADD THIS ---
    // Create an instance of WellnessDataRepository to manage wellness data
    private val wellnessDataRepository = WellnessDataRepository(context)
    // --- END ---


    // Key for storing the single user profile as a JSON string
    private val USER_PROFILE_KEY = stringPreferencesKey("user_profile")

    /**
     * Creates or updates the user profile.
     * Since there's only one user, this function serves as the "registration".
     */
    suspend fun createOrUpdateUserProfile(
        firstName: String,
        lastName: String,
        age: Int,
        targetWeight: Double,
        weightUnit: String // <-- ADDED
    ): Result<Unit> {
        return try {
            val userProfile = UserProfile(
                firstName = firstName,
                lastName = lastName,
                age = age,
                targetWeight = targetWeight,
                weightUnit = weightUnit // <-- ADDED
            )
            context.userStore.edit { preferences ->
                preferences[USER_PROFILE_KEY] = gson.toJson(userProfile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves the single user profile from DataStore.
     */
    suspend fun getUserProfile(): UserProfile? {
        return try {
            val jsonString = context.userStore.data.map { it[USER_PROFILE_KEY] }.first()
            if (jsonString != null) {
                gson.fromJson(jsonString, UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get user profile", e)
            null
        }
    }

    /**
     * Checks if a profile has been created.
     * This replaces the isLoggedIn() check.
     */
    suspend fun hasProfile(): Boolean {
        return getUserProfile() != null
    }

    /**
     * Deletes the user profile AND all associated wellness data.
     * This can be used for a "reset" or "logout" functionality.
     */
    suspend fun deleteProfileAndData() { // <-- RENAMED for clarity
        // Delete the user profile
        context.userStore.edit { preferences ->
            preferences.remove(USER_PROFILE_KEY)
        }
        // --- ADD THIS ---
        // Also delete all the wellness history data
        wellnessDataRepository.clearAllData()
        // --- END ---
    }
}
