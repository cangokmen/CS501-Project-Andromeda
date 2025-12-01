package com.example.andromeda.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    private fun safeKeyFromEmail(email: String): String =
        email.replace("@", "_at_").replace(".", "_dot_")
    private object PreferencesKeys {
        // UI / theme
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val USE_BIGGER_TEXT = booleanPreferencesKey("use_bigger_text")
        val SELECTED_QUESTIONS = stringSetPreferencesKey("selected_questions")

        // Auth – current session
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PASSWORD = stringPreferencesKey("user_password")
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")

        // Auth – all registered users, encoded as "email|password|name"
        val USERS_SET = stringSetPreferencesKey("users_set")
    }

    // --------- UI flows ---------

    val isDarkTheme: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.IS_DARK_THEME] ?: false }

    val useBiggerText: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.USE_BIGGER_TEXT] ?: false }

    val selectedQuestions: Flow<Set<String>> = context.userPreferencesDataStore.data
        .map { prefs ->
            prefs[PreferencesKeys.SELECTED_QUESTIONS] ?: setOf("DIET", "ACTIVITY", "SLEEP")
        }
    data class AccountProfile(
        val lastName: String = "",
        val age: String = "",
        val targetWeight: String = ""
    )

    suspend fun saveAccountProfile(
        email: String,
        lastName: String,
        age: String,
        targetWeight: String
    ) {
        val safe = safeKeyFromEmail(email)
        context.userPreferencesDataStore.edit { prefs ->
            prefs[stringPreferencesKey("profile_last_name_$safe")] = lastName
            prefs[stringPreferencesKey("profile_age_$safe")] = age
            prefs[stringPreferencesKey("profile_target_weight_$safe")] = targetWeight
        }
    }

    suspend fun getAccountProfile(email: String): AccountProfile {
        val safe = safeKeyFromEmail(email)
        val prefs = context.userPreferencesDataStore.data.first()
        val lastName = prefs[stringPreferencesKey("profile_last_name_$safe")] ?: ""
        val age = prefs[stringPreferencesKey("profile_age_$safe")] ?: ""
        val targetWeight = prefs[stringPreferencesKey("profile_target_weight_$safe")] ?: ""
        return AccountProfile(lastName, age, targetWeight)
    }

    // --------- Auth flows ---------

    val userEmail: Flow<String?> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.USER_EMAIL] }

    val userName: Flow<String?> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.USER_NAME] }

    val isLoggedIn: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { prefs -> prefs[PreferencesKeys.IS_LOGGED_IN] ?: false }

    // --------- UI save helpers ---------

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

    // --------- Auth helpers ---------

    private fun encodeUser(email: String, password: String, name: String): String =
        "$email|$password|$name"

    private fun decodeUser(encoded: String): Triple<String, String, String>? {
        val parts = encoded.split("|")
        return if (parts.size == 3) {
            Triple(parts[0], parts[1], parts[2])
        } else null
    }

    /**
     * Register or update an account.
     *
     * - Keeps all previously registered accounts.
     * - If the email already exists, its password/name are updated.
     * - Marks this account as the currently logged-in user.
     */
    suspend fun saveUserCredentials(name: String, email: String, password: String) {
        context.userPreferencesDataStore.edit { prefs ->
            val existingSet = prefs[PreferencesKeys.USERS_SET] ?: emptySet()
            val mutable = existingSet.toMutableSet()

            // Remove old record(s) with same email
            mutable.removeAll { encoded ->
                decodeUser(encoded)?.first == email
            }

            // Add/replace this email
            mutable.add(encodeUser(email, password, name))

            // Save back full set
            prefs[PreferencesKeys.USERS_SET] = mutable

            // Also set current session info
            prefs[PreferencesKeys.USER_EMAIL] = email
            prefs[PreferencesKeys.USER_PASSWORD] = password
            prefs[PreferencesKeys.USER_NAME] = name
            prefs[PreferencesKeys.IS_LOGGED_IN] = true
        }
    }

    /**
     * Validate login credentials against all registered accounts.
     * If successful, updates the "current session" info and sets IS_LOGGED_IN = true.
     */
    suspend fun checkCredentials(email: String, password: String): Boolean {
        val prefsSnapshot = context.userPreferencesDataStore.data.first()
        val usersSet = prefsSnapshot[PreferencesKeys.USERS_SET] ?: emptySet()

        var matched: Triple<String, String, String>? = null
        for (encoded in usersSet) {
            val decoded = decodeUser(encoded)
            if (decoded != null) {
                val (e, p, _) = decoded
                if (e == email && p == password) {
                    matched = decoded
                    break
                }
            }
        }

        val ok = matched != null
        Log.d("UserPrefsRepo", "checkCredentials($email) -> $ok")

        if (ok) {
            val (e, p, n) = matched!!
            context.userPreferencesDataStore.edit { prefs ->
                prefs[PreferencesKeys.USER_EMAIL] = e
                prefs[PreferencesKeys.USER_PASSWORD] = p
                prefs[PreferencesKeys.USER_NAME] = n
                prefs[PreferencesKeys.IS_LOGGED_IN] = true
            }
        }

        return ok
    }

    /**
     * Explicitly set login state. Kept for compatibility with existing ViewModel code.
     */
    suspend fun setLoggedIn(value: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_LOGGED_IN] = value
        }
    }

    /**
     * Logout: do not delete any saved accounts, only mark the session as logged out.
     */
    suspend fun logoutUser() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_LOGGED_IN] = false
        }
    }

    /**
     * Clear current session info (but keep registered accounts).
     * Only needed if some older code still calls this.
     */
    suspend fun clearUser() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.USER_EMAIL)
            prefs.remove(PreferencesKeys.USER_PASSWORD)
            prefs.remove(PreferencesKeys.USER_NAME)
            prefs[PreferencesKeys.IS_LOGGED_IN] = false
        }
    }

}
