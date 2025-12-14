package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import java.math.BigDecimal
import java.util.TimeZone // Import TimeZone


// --- CONVERSION CONSTANTS ---
private const val LBS_TO_KG = 0.453592
private const val KG_TO_LBS = 2.20462

data class AddScreenUiState(
    val weight: String = "",
    val dietRating: Float = 5f,
    val activityRating: Float = 5f,
    val sleepHours: Float = 5f,
    val waterIntake: Float = 5f,
    val proteinIntake: Float = 5f,
    val showSaveConfirmation: Boolean = false,
    val isEditing: Boolean = false,
    val weightUnit: String = "kg"
)

class AddViewModel(
    private val repository: WellnessDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val selectedQuestions: Set<String>,
    private val wellnessDataId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddScreenUiState())
    val uiState: StateFlow<AddScreenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentUnit = userPreferencesRepository.weightUnit.first()
            _uiState.update { it.copy(weightUnit = currentUnit) }

            if (wellnessDataId != null) {
                loadWellnessData(wellnessDataId, currentUnit)
            }
        }
    }

    private fun loadWellnessData(idOrDate: String, currentUnit: String) {
        viewModelScope.launch {
            val entry = repository.getWellnessDataById(idOrDate)
            if (entry != null) {
                // The value is stored in KG. Convert it for display if the user's preference is lbs.
                val displayWeight: Double
                if (currentUnit == "lbs") {
                    // Perform the multiplication using BigDecimal to avoid floating point errors
                    val weightInKg = BigDecimal.valueOf(entry.weight)
                    val conversionFactor = BigDecimal.valueOf(KG_TO_LBS)
                    // Set the scale (number of decimal places) and rounding mode here
                    displayWeight = weightInKg.multiply(conversionFactor)
                        .setScale(1, RoundingMode.HALF_UP)
                        .toDouble()
                } else {
                    // For kg, just round the existing value
                    displayWeight = BigDecimal.valueOf(entry.weight)
                        .setScale(1, RoundingMode.HALF_UP)
                        .toDouble()
                }

                _uiState.update {
                    it.copy(
                        weight = displayWeight.toString(), // Use the correctly converted weight for display
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
                // The UI value is what the user sees (e.g., 154.3 lbs).
                // We ALWAYS convert it to KG for storage if the current unit is lbs.
                // This logic is now correct for both new entries and edits.
                val weightInKg = if (currentUiState.weightUnit == "lbs") {
                    weightValue * LBS_TO_KG
                } else {
                    weightValue
                }

                val finalWeight = weightInKg.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()

                val timestamp: String
                val entryId: String

                // Create a formatter that is explicitly in UTC to avoid timezone shifts
                val utcFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }

                if (currentUiState.isEditing) {
                    val existingEntry = repository.getWellnessDataById(wellnessDataId!!)!!
                    entryId = existingEntry.id
                    timestamp = existingEntry.timestamp
                } else {
                    entryId = java.util.UUID.randomUUID().toString()
                    timestamp = if (wellnessDataId != null) {
                        // If a date string is passed, use it directly
                        wellnessDataId
                    } else {
                        // For a new entry for today, format today's date in UTC
                        utcFormatter.format(Date())
                    }
                }

                val entryToSave = WellnessData(
                    id = entryId,
                    timestamp = timestamp,
                    weight = finalWeight,
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

    fun deleteEntry() {
        viewModelScope.launch {
            if (uiState.value.isEditing && wellnessDataId != null) {
                repository.deleteWellnessData(wellnessDataId)
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
                    userPreferencesRepository = UserPreferencesRepository(application),
                    selectedQuestions = selectedQuestions,
                    wellnessDataId = wellnessDataId
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}