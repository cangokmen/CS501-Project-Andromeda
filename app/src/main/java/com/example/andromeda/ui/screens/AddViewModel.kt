package com.example.andromeda.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AddScreenUiState(
    val weight: String = "",
    val dietRating: Float = 5f,
    val activityRating: Float = 5f,
    val sleepHours: Float = 5f, // Default to 5
    val waterIntake: Float = 5f, // Default to 5
    val proteinIntake: Float = 5f, // Default to 5
    val showSaveConfirmation: Boolean = false
)

class AddViewModel(
    private val repository: WellnessDataRepository,
    private val selectedQuestions: Set<String>
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddScreenUiState())
    val uiState: StateFlow<AddScreenUiState> = _uiState.asStateFlow()

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

    fun onWaterIntakeChange(intake: Float) {
        _uiState.update { it.copy(waterIntake = intake) }
    }

    fun onProteinIntakeChange(intake: Float) {
        _uiState.update { it.copy(proteinIntake = intake) }
    }

    fun saveEntry() {
        val currentUiState = _uiState.value
        val weightValue = currentUiState.weight.toDoubleOrNull()
        if (weightValue != null) {
            viewModelScope.launch {
                val entryToSave = WellnessData(
                    weight = weightValue,
                    dietRating = if ("DIET" in selectedQuestions) currentUiState.dietRating.roundToInt() else null,
                    activityLevel = if ("ACTIVITY" in selectedQuestions) currentUiState.activityRating.roundToInt() else null,
                    sleepHours = if ("SLEEP" in selectedQuestions) currentUiState.sleepHours.roundToInt() else null,
                    waterIntake = if ("WATER" in selectedQuestions) currentUiState.waterIntake.roundToInt() else null,
                    proteinIntake = if ("PROTEIN" in selectedQuestions) currentUiState.proteinIntake.roundToInt() else null
                )
                repository.addWellnessData(entryToSave)
                // --- MODIFIED: Reset all ratings to 5f after saving ---
                _uiState.update {
                    it.copy(
                        weight = "",
                        dietRating = 5f,
                        activityRating = 5f,
                        sleepHours = 5f,
                        waterIntake = 5f,
                        proteinIntake = 5f,
                        showSaveConfirmation = true
                    )
                }
            }
        }
    }

    fun dismissSaveConfirmation() {
        _uiState.update { it.copy(showSaveConfirmation = false) }
    }

    class Factory(
        private val application: Application,
        private val selectedQuestions: Set<String>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddViewModel(
                    repository = WellnessDataRepository(application),
                    selectedQuestions = selectedQuestions
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
