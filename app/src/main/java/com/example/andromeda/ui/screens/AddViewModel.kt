package com.example.andromeda.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// UI state for the AddScreen
data class AddScreenState(
    val weight: String = "",
    val dietRating: Float = 5f,
    val activityRating: Float = 5f,
    val sleepHours: Float = 5f,
    val showSaveConfirmation: Boolean = false // New state to control dialog visibility
)

class AddViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WellnessDataRepository(application)

    private val _uiState = MutableStateFlow(AddScreenState())
    val uiState: StateFlow<AddScreenState> = _uiState.asStateFlow()

    // --- Event Handlers to update the UI state ---
    fun onWeightChange(weight: String) {
        _uiState.update { it.copy(weight = weight) }
    }

    fun onDietRatingChange(rating: Float) {
        _uiState.update { it.copy(dietRating = rating) }
    }

    fun onActivityRatingChange(rating: Float) {
        _uiState.update { it.copy(activityRating = rating) }
    }

    fun onSleepHoursChange(hours: Float) {
        _uiState.update { it.copy(sleepHours = hours) }
    }

    // --- Function to save the entry ---
    fun saveEntry() {
        viewModelScope.launch {
            val currentState = _uiState.value

            val wellnessData = WellnessData(
                weight = currentState.weight.toDoubleOrNull() ?: 0.0,
                dietRating = currentState.dietRating.roundToInt(),
                activityLevel = currentState.activityRating.roundToInt(),
                sleepHours = currentState.sleepHours.roundToInt()
            )

            repository.addWellnessData(wellnessData)

            // Show the confirmation dialog
            _uiState.update { it.copy(showSaveConfirmation = true) }
        }
    }

    // --- Function to dismiss the dialog and reset fields ---
    fun dismissSaveConfirmation() {
        // Reset the entire state, which also hides the dialog
        _uiState.value = AddScreenState()
    }
}
