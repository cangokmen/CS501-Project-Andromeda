package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Unauthenticated : RegisterState()
    object Loading : RegisterState()
    data class Authenticated(val userProfile: UserProfile?) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel(
    private val registerRepository: RegisterRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Loading)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    init {
        checkUserStatus()
    }

    fun checkUserStatus() {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            if (registerRepository.hasProfile()) {
                val profile = registerRepository.getUserProfile()
                _registerState.value = RegisterState.Authenticated(profile)
            } else {
                _registerState.value = RegisterState.Unauthenticated
            }
        }
    }

    /*
     * AI Suggested this: To handle a multi-part registration, this function coordinates
     * writes to two different repositories. It first saves the general weight unit
     * preference for the app, then creates the permanent user profile, ensuring all
     * necessary data is stored from a single user action.
     */
    fun createProfile(firstName: String, lastName: String, age: Int, targetWeight: Double, weightUnit: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            // Save the unit preference that the rest of the app will use
            userPrefsRepository.saveWeightUnitPreference(weightUnit)

            // This part correctly saves the unit to the user's permanent profile
            val result = registerRepository.createOrUpdateUserProfile(firstName, lastName, age, targetWeight, weightUnit)
            result.onSuccess {
                val profile = registerRepository.getUserProfile()
                _registerState.value = RegisterState.Authenticated(profile)
            }.onFailure {
                _registerState.value = RegisterState.Error(it.message ?: "An unknown error occurred.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            registerRepository.deleteProfileAndData()
            // It might also be a good idea to reset the preference to default on logout, but it's optional.
            // userPrefsRepository.saveWeightUnitPreference("kg")
            _registerState.value = RegisterState.Unauthenticated
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                        // Create both repositories here
                        val registerRepository = RegisterRepository(application)
                        val userPrefsRepository = UserPreferencesRepository(application)
                        return RegisterViewModel(registerRepository, userPrefsRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
