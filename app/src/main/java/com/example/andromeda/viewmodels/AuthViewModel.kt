package com.example.andromeda.viewmodels

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthScreenUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val age: String = "",
    val targetWeight: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isLoginMode: Boolean = true
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onNameChange(name: String) {
        val parts = name.trim().split(" ", limit = 2)
        val firstName = parts.getOrNull(0) ?: ""
        val lastName = parts.getOrNull(1) ?: ""
        _uiState.update { it.copy(name = name, firstName = firstName, lastName = lastName) }
    }

    fun onAgeChange(age: String) {
        if (age.all { it.isDigit() }) {
            _uiState.update { it.copy(age = age) }
        }
    }

    fun onTargetWeightChange(weight: String) {
        if (weight.isEmpty() || weight.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _uiState.update { it.copy(targetWeight = weight) }
        }
    }

    fun switchMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, errorMessage = null) }
    }

    private fun validate(isLogin: Boolean): Boolean {
        val state = _uiState.value

        if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return false
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return false
        }

        if (!isLogin) {
            if (state.firstName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter your first name.") }
                return false
            }
            if (state.age.isBlank() || state.targetWeight.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please fill in all fields.") }
                return false
            }
        }

        _uiState.update { it.copy(errorMessage = null) }
        return true
    }

    fun login(onSuccess: () -> Unit) {
        if (!validate(isLogin = true)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.loginUser(_uiState.value.email, _uiState.value.password)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun register(onSuccess: () -> Unit) {
        if (!validate(isLogin = false)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = _uiState.value
            val result = repository.registerUser(
                email = state.email,
                password = state.password,
                firstName = state.firstName,
                lastName = state.lastName,
                age = state.age.toIntOrNull() ?: 0,
                targetWeight = state.targetWeight.toDoubleOrNull() ?: 0.0
            )

            if (result.isSuccess) {
                login(onSuccess)
            } else {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message, isLoading = false) }
            }
        }
    }


    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(AuthRepository(application)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
