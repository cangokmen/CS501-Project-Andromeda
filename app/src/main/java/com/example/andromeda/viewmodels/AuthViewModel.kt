package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.AuthRepository
import com.example.andromeda.data.UserProfile
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

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

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

    fun createProfile(firstName: String, lastName: String, age: Int, targetWeight: Double, weightUnit: WeightUnit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
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
            _authState.value = AuthState.Unauthenticated
        }
    }

    companion object {
        // This factory now takes the Application context as a parameter
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST") // This is the correct syntax
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        // Create the repository here, passing the application context
                        val authRepository = AuthRepository(application)
                        return AuthViewModel(authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
