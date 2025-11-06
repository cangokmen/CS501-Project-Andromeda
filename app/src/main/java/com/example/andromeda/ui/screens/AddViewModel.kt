package com.example.andromeda.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A data class to hold all the state for the AddScreen.
 */
data class AddScreenState(
    val weight: String = "",
    val dietRating: Float = 5f,
    val activityRating: Float = 5f,
    val moodRating: Float = 5f
)

/**
 * The ViewModel for the AddScreen.
 */
class AddViewModel : ViewModel() {

    // Expose the state as a read-only StateFlow
    private val _uiState = MutableStateFlow(AddScreenState())
    val uiState = _uiState.asStateFlow()

    // --- Event Handlers ---

    fun onWeightChange(newWeight: String) {
        // Only allow digits and a single decimal point
        if (newWeight.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _uiState.update { it.copy(weight = newWeight) }
        }
    }

    fun onDietRatingChange(newRating: Float) {
        _uiState.update { it.copy(dietRating = newRating) }
    }

    fun onActivityRatingChange(newRating: Float) {
        _uiState.update { it.copy(activityRating = newRating) }
    }

    fun onMoodRatingChange(newRating: Float) {
        _uiState.update { it.copy(moodRating = newRating) }
    }

    fun saveEntry() {
        // TODO: This is where you will later call the repository to save the data.
        // For now, it just prints the current state.
        println("Saving State: ${_uiState.value}")

        // Reset the state to default values after saving
        _uiState.value = AddScreenState()
    }
}
