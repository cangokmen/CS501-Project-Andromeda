package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.AuthRepository
import com.example.andromeda.data.UserProfile
// --- 1. ADD THIS IMPORT ---
import com.example.andromeda.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userProfile: UserProfile?) : AuthState()
    data class Error(val message: String) : AuthState()
}

// --- 2. MODIFY CONSTRUCTOR ---
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkUserStatus()
    }

    fun checkUserStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            if (authRepository.hasProfile()) {
                val profile = authRepository.getUserProfile()
                _authState.value = AuthState.Authenticated(profile)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun createProfile(firstName: String, lastName: String, age: Int, targetWeight: Double, weightUnit: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // --- 3. ADD THIS LINE ---
            // Save the unit preference that the rest of the app will use
            userPrefsRepository.saveWeightUnitPreference(weightUnit)

            // This part correctly saves the unit to the user's permanent profile
            val result = authRepository.createOrUpdateUserProfile(firstName, lastName, age, targetWeight, weightUnit)
            result.onSuccess {
                val profile = authRepository.getUserProfile()
                _authState.value = AuthState.Authenticated(profile)
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "An unknown error occurred.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.deleteProfileAndData()
            // It might also be a good idea to reset the preference to default on logout, but it's optional.
            // userPrefsRepository.saveWeightUnitPreference("kg")
            _authState.value = AuthState.Unauthenticated
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        // --- 4. MODIFY FACTORY ---
                        // Create both repositories here
                        val authRepository = AuthRepository(application)
                        val userPrefsRepository = UserPreferencesRepository(application)
                        return AuthViewModel(authRepository, userPrefsRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
