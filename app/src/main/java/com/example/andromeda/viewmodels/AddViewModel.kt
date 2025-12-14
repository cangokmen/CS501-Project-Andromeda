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
    // This is a String that can be an ID (for editing) or a date (for a new past entry)
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
            // **FIX**: We now directly call getWellnessDataById with the String.
            // If it returns an entry, we are in edit mode.
            // If it returns null, we assume idOrDate was a date for a new entry.
            val entry = repository.getWellnessDataById(idOrDate)

            if (entry != null) {
                // We found an entry, so we are in "Edit" mode.
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
            // No 'else' block needed. If entry is null, we stay in the default "Add" mode.
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
                // Determine the correct timestamp.
                val timestamp: String
                val entryId: String

                if (currentUiState.isEditing) {
                    // Editing an existing entry. Use its ID and original timestamp.
                    val existingEntry = repository.getWellnessDataById(wellnessDataId!!)!!
                    entryId = existingEntry.id
                    timestamp = existingEntry.timestamp
                } else {
                    // Creating a new entry. Generate a new ID.
                    entryId = java.util.UUID.randomUUID().toString()
                    // Timestamp is either the passed-in date or today's date.
                    timestamp = if (wellnessDataId != null) {
                        wellnessDataId // Use the date from HistoryScreen
                    } else {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Use today
                    }
                }

                val entryToSave = WellnessData(
                    id = entryId, // **FIX**: Now correctly uses a String ID
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
