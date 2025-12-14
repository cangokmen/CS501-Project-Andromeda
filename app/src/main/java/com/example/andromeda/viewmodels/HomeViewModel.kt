package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.BuildConfig
import com.example.andromeda.data.AuthRepository
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

// UI State for the HomeScreen - suggestions is now a List
data class HomeUiState(
    val suggestions: List<String> = emptyList(), // Changed to List<String>
    val isLoading: Boolean = false,
    val error: String? = null,
    // Add user's first name to the state for a personalized greeting
    val userFirstName: String? = null
)

class HomeViewModel(
    private val wellnessRepo: WellnessDataRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Load the user's name when the ViewModel is created
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val profile = authRepo.getUserProfile()
            profile?.let {
                _uiState.update { currentState ->
                    currentState.copy(userFirstName = it.firstName)
                }
            }
        }
    }

    fun generateSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // --- FIX: Fetch the single user profile directly ---
                val userProfile = authRepo.getUserProfile()
                val wellnessData = wellnessRepo.allWellnessData.first().takeLast(7) // Get last 7 entries

                if (wellnessData.isEmpty()) {
                    _uiState.update { it.copy(suggestions = listOf("Not enough data to generate suggestions. Please add more entries."), isLoading = false) }
                    return@launch
                }

                val prompt = createPrompt(wellnessData, userProfile)

                val responseText = withContext(Dispatchers.IO) {
                    val generativeModel = GenerativeModel(
                        // --- FIX: Corrected model name and API key access ---
                        modelName = "gemini-1.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY
                    )
                    val response = generativeModel.generateContent(prompt)
                    response.text ?: "No response text found."
                }

                // Parse the response into a list of suggestions
                val suggestionList = responseText
                    .split("\n") // Split by new line
                    .filter { it.trim().startsWith("-") } // Keep only lines that are suggestions
                    .map { it.trim().removePrefix("-").trim() } // Clean up each suggestion

                _uiState.update { it.copy(suggestions = suggestionList, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun createPrompt(data: List<WellnessData>, profile: UserProfile?): String {
        // --- FIX: Use the profile's weight unit for the prompt ---
        val weightUnit = profile?.weightUnit ?: "kg"
        val dataSummary = data.joinToString(separator = "\n") { entry ->
            val displayWeight = if (weightUnit == "lbs") {
                (entry.weight * 2.20462).toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            } else {
                entry.weight.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            }
            "- Date: ${entry.timestamp}, Weight: $displayWeight $weightUnit, Diet: ${entry.dietRating ?: "N/A"}, " +
                    "Activity: ${entry.activityLevel ?: "N/A"}, Sleep: ${entry.sleepHours ?: "N/A"}"
        }

        val userContext = if (profile != null) {
            val targetWeight = (if (weightUnit == "lbs") {
                (profile.targetWeight * 2.20462)
            } else {
                profile.targetWeight
            }).toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            "The user's age is ${profile.age} and their target weight is $targetWeight $weightUnit."
        } else {
            ""
        }
        // --- END FIX ---

        return """
        Based on the following user information and recent wellness data:
        $userContext
        
        Recent Data:
        $dataSummary

        Please provide 3 very short, encouraging, and actionable suggestions or motivational tips to 
        help them improve their habits. The tone should be positive and supportive. Format the output 
        as a simple, un-numbered list, with each tip on a new line starting with a dash.
        """.trimIndent()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    WellnessDataRepository(application),
                    AuthRepository(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
