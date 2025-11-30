package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.BuildConfig
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

// UI State for the HomeScreen - suggestions is now a List
data class HomeUiState(
    val suggestions: List<String> = emptyList(), // Changed to List<String>
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(private val repository: WellnessDataRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun generateSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch the most recent data
                val wellnessData = repository.allWellnessData.first().takeLast(7) // Get last 7 entries
                if (wellnessData.isEmpty()) {
                    _uiState.update { it.copy(suggestions = listOf("Not enough data to generate suggestions. Please add more entries."), isLoading = false) }
                    return@launch
                }

                // Create the prompt
                val prompt = createPrompt(wellnessData)

                val responseText = withContext(Dispatchers.IO) {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-2.5-flash-lite",
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

    private fun createPrompt(data: List<WellnessData>): String {
        val dataSummary = data.joinToString(separator = "\n") { entry ->
            "- Date: ${entry.timestamp}, Weight: ${entry.weight}, Diet: ${entry.dietRating ?: "N/A"}, " +
                    "Activity: ${entry.activityLevel ?: "N/A"}, Sleep: ${entry.sleepHours ?: "N/A"}"
        }

        return """
        Based on the following recent wellness data for a user:
        $dataSummary

        Please provide 3 very short, encouraging, and actionable suggestions or motivational tips to 
        help them improve their habits. The tone should be positive and supportive. Format the output 
        as a simple, un-numbered list, with each tip on a new line starting with a dash.
        """.trimIndent()
    }

    // Factory to create the ViewModel with its dependency
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(WellnessDataRepository(application)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
