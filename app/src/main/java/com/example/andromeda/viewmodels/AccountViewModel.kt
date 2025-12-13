package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.AuthRepository
import com.example.andromeda.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false
)

class AccountViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = repository.getCurrentUserEmail()
            if (email != null) {
                val profile = repository.getUserProfile(email)
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onProfileValueChange(
        firstName: String? = null,
        lastName: String? = null,
        age: String? = null,
        targetWeight: String? = null
    ) {
        _uiState.value.profile?.let { currentProfile ->
            _uiState.update {
                it.copy(
                    profile = currentProfile.copy(
                        firstName = firstName ?: currentProfile.firstName,
                        lastName = lastName ?: currentProfile.lastName,
                        age = age?.toIntOrNull() ?: currentProfile.age,
                        targetWeight = targetWeight?.toDoubleOrNull() ?: currentProfile.targetWeight
                    )
                )
            }
        }
    }

    fun toggleEditMode() {
        val isCurrentlyEditing = _uiState.value.isEditing
        if (isCurrentlyEditing) {
            // If we were editing, now we save
            saveProfile()
        }
        _uiState.update { it.copy(isEditing = !isCurrentlyEditing) }
    }

    private fun saveProfile() {
        viewModelScope.launch {
            _uiState.value.profile?.let {
                repository.updateUserProfile(it)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(AuthRepository(application)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
