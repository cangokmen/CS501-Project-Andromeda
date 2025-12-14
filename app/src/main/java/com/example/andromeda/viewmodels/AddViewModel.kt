package com.example.andromeda.viewmodels

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class AddScreenUiState(
    val weight: String = "",
    val dietRating: Float = 5f,
    val activityRating: Float = 5f,
    val sleepHours: Float = 5f,
    val waterIntake: Float = 5f,
    val proteinIntake: Float = 5f,
    val showSaveConfirmation: Boolean = false,
    val isEditing: Boolean = false
)

class AddViewModel(
    private val repository: WellnessDataRepository,
    private val selectedQuestions: Set<String>,
    private val wellnessDataId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddScreenUiState())
    val uiState: StateFlow<AddScreenUiState> = _uiState.asStateFlow()

    init {
        if (wellnessDataId != null) {
            loadWellnessData(wellnessDataId)
        }
    }

    private fun loadWellnessData(idOrDate: String) {
        viewModelScope.launch {
            val entry = repository.getWellnessDataById(idOrDate)
            if (entry != null) {
                _uiState.update {
                    it.copy(
                        weight = entry.weight.toString(),
                        dietRating = entry.dietRating?.toFloat() ?: 5f,
                        activityRating = entry.activityLevel?.toFloat() ?: 5f,
                        sleepHours = entry.sleepHours?.toFloat() ?: 5f,
                        waterIntake = entry.waterIntake?.toFloat() ?: 5f,
                        proteinIntake = entry.proteinIntake?.toFloat() ?: 5f,
                        isEditing = true
                    )
                }
            }
        }
    }


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
                val timestamp: String
                val entryId: String

                if (currentUiState.isEditing) {
                    val existingEntry = repository.getWellnessDataById(wellnessDataId!!)!!
                    entryId = existingEntry.id
                    timestamp = existingEntry.timestamp
                } else {
                    entryId = java.util.UUID.randomUUID().toString()
                    timestamp = if (wellnessDataId != null) {
                        wellnessDataId
                    } else {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    }
                }

                val entryToSave = WellnessData(
                    id = entryId,
                    timestamp = timestamp,
                    weight = weightValue,
                    dietRating = if ("DIET" in selectedQuestions) currentUiState.dietRating.roundToInt() else null,
                    activityLevel = if ("ACTIVITY" in selectedQuestions) currentUiState.activityRating.roundToInt() else null,
                    sleepHours = if ("SLEEP" in selectedQuestions) currentUiState.sleepHours.roundToInt() else null,
                    waterIntake = if ("WATER" in selectedQuestions) currentUiState.waterIntake.roundToInt() else null,
                    proteinIntake = if ("PROTEIN" in selectedQuestions) currentUiState.proteinIntake.roundToInt() else null
                )

                if (currentUiState.isEditing) {
                    repository.updateWellnessData(entryToSave)
                } else {
                    repository.addWellnessData(entryToSave)
                }

                _uiState.update { it.copy(showSaveConfirmation = true) }
            }
        }
    }

    // --- NEW FUNCTION ---
    /**
     * Deletes the current entry if it's in edit mode.
     * It triggers the onSaveComplete callback which closes the dialog.
     */
    fun deleteEntry() {
        viewModelScope.launch {
            if (uiState.value.isEditing && wellnessDataId != null) {
                repository.deleteWellnessData(wellnessDataId)
                // We re-use the save confirmation logic to dismiss the dialog
                _uiState.update { it.copy(showSaveConfirmation = true) }
            }
        }
    }
    // --- END NEW FUNCTION ---

    fun dismissSaveConfirmation() {
        _uiState.update { it.copy(showSaveConfirmation = false) }
    }

    class Factory(
        private val application: Application,
        private val selectedQuestions: Set<String>,
        private val wellnessDataId: String?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddViewModel(
                    repository = WellnessDataRepository(application),
                    selectedQuestions = selectedQuestions,
                    wellnessDataId = wellnessDataId
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
