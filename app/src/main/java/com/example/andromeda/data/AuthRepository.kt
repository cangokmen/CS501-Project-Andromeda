package com.example.andromeda.data

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// --- FIXED: Import UserProfile from the correct 'data' package ---
import com.example.andromeda.data.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
private val Context.userStore: DataStore<Preferences> by preferencesDataStore(name = "user_profiles")

class AuthRepository(private val application: Application) {

    private val gson = Gson()
    private val context = application.applicationContext

    // Key for storing the list of users as a JSON string
    private val USER_LIST_KEY = stringPreferencesKey("user_list")
    // Key for storing the currently logged-in user's email
    private val LOGGED_IN_USER_KEY = stringPreferencesKey("logged_in_user_email")

    /**
     * Registers a new user by adding them to the local DataStore.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        age: Int,
        targetWeight: Double
    ): Result<Unit> {
        return try {
            val allUsers = getUsers()
            // Check if user already exists
            if (allUsers.any { it.email.equals(email, ignoreCase = true) }) {
                return Result.failure(Exception("An account with this email already exists."))
            }

            // In a real app, you would hash the password. Here we store it directly for simplicity.
            val newUser = UserProfile(
                email = email,
                passwordHash = password, // Storing plain text for this example
                firstName = firstName,
                lastName = lastName,
                age = age,
                targetWeight = targetWeight
            )

            context.userStore.edit { preferences ->
                val updatedUsers = allUsers + newUser
                preferences[USER_LIST_KEY] = gson.toJson(updatedUsers)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Logs in a user by checking credentials against the local DataStore.
     */
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            val user = getUsers().find { it.email.equals(email, ignoreCase = true) }

            if (user != null && user.passwordHash == password) {
                // Save the logged-in user's email
                context.userStore.edit { preferences ->
                    preferences[LOGGED_IN_USER_KEY] = email
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid email or password."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed", e)
            Result.failure(e)
        }
    }

    // --- ADDED: Get a single user profile ---
    suspend fun getUserProfile(email: String): UserProfile? {
        return getUsers().find { it.email.equals(email, ignoreCase = true) }
    }

    // --- ADDED: Update an existing user profile ---
    suspend fun updateUserProfile(updatedProfile: UserProfile) {
        try {
            val allUsers = getUsers().toMutableList()
            val userIndex = allUsers.indexOfFirst { it.email.equals(updatedProfile.email, ignoreCase = true) }
            if (userIndex != -1) {
                allUsers[userIndex] = updatedProfile
                context.userStore.edit { preferences ->
                    preferences[USER_LIST_KEY] = gson.toJson(allUsers)
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile", e)
        }
    }

    /**
     * Retrieves the list of all users from DataStore.
     */
    private suspend fun getUsers(): List<UserProfile> {
        return try {
            val jsonString = context.userStore.data.first()[USER_LIST_KEY] ?: "[]"
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(jsonString, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get users", e)
            emptyList()
        }
    }

    /**
     * Gets the email of the currently signed-in user.
     */
    suspend fun getCurrentUserEmail(): String? {
        return try {
            context.userStore.data.map { it[LOGGED_IN_USER_KEY] }.first()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get current user email", e)
            null
        }
    }

    /**
     * Signs out the current user.
     */
    suspend fun logout() {
        context.userStore.edit { preferences ->
            preferences.remove(LOGGED_IN_USER_KEY)
        }
    }

    /**
     * Checks if a user is currently logged in.
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentUserEmail() != null
    }
}
