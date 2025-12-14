package com.example.andromeda.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.andromeda.BuildConfig
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode

// Represents a single message in the chat.
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isProcessing: Boolean = false // To show a loading indicator
)

// UI state for the Chatbot screen.
data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = ""
)

class ChatbotViewModel(
    private val wellnessRepo: WellnessDataRepository,
    private val authRepo: RegisterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite", // Updated model name
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Store the chat history for context
    private val chatHistory = mutableListOf<Content>()

    init {
        // Start the conversation with a greeting from the model
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        "Hello! I'm your wellness assistant. How can I help you today? Feel free to ask about your progress, habits, or for any recommendations.",
                        isFromUser = false
                    )
                )
            )
        }
    }

    fun onUserInputChange(text: String) {
        _uiState.update { it.copy(userInput = text) }
    }

    fun sendMessage() {
        val userText = _uiState.value.userInput.trim()
        if (userText.isBlank()) return

        // Add user message to UI and history
        val userMessage = ChatMessage(userText, isFromUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                userInput = "" // Clear the input field
            )
        }
        chatHistory.add(content("user") { text(userText) })


        /* AI Suggested this: This pattern improves the user experience by providing
         * immediate feedback. A temporary "processing" message is added to the UI
         * while the actual model response is being generated in the background.
         * Add a temporary processing message
         */
        val processingMessage = ChatMessage("...", isFromUser = false, isProcessing = true)
        _uiState.update { it.copy(messages = it.messages + processingMessage) }

        // Launch a coroutine to get the response from the model
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userProfile = authRepo.getUserProfile()
                val wellnessData = wellnessRepo.allWellnessData.first()

                // Create the context prompt with the fetched profile
                val contextPrompt = createWellnessContextPrompt(wellnessData, userProfile)

                // The chat object allows for multi-turn conversations
                val chat = generativeModel.startChat(
                    history = listOf(
                        content("user") { text(contextPrompt) },
                        content("model") { text("Okay, I have the user's wellness data. I will use it to provide personalized advice. Let's begin.") }
                    ) + chatHistory
                )

                val response = chat.sendMessage(userText)
                val modelResponseText = response.text ?: "Sorry, I couldn't process that. Please try again."

                // Add model response to history
                chatHistory.add(content("model") { text(modelResponseText) })

                // Update UI with the final model response
                val modelMessage = ChatMessage(modelResponseText, isFromUser = false)
                _uiState.update {
                    it.copy(
                        messages = it.messages.dropLast(1) + modelMessage // Replace "..." with the actual message
                    )
                }

            } catch (e: Exception) {
                // Handle errors (e.g., network issues, API errors)
                val errorMessage = ChatMessage("Error: ${e.message}", isFromUser = false)
                _uiState.update {
                    it.copy(messages = it.messages.dropLast(1) + errorMessage)
                }
            }
        }
    }

    /* AI Suggested this: To achieve personalized responses, this function was created
     * to dynamically build a detailed context prompt. It aggregates the user's
     * profile and their recent wellness data into a single block of text for the LLM.
     * Creates a detailed prompt with the user's profile and historical data
     */
    private fun createWellnessContextPrompt(data: List<WellnessData>, profile: UserProfile?): String {

        val weightUnit = profile?.weightUnit ?: "kg"

        val userContext = if (profile != null) {
            val targetWeight = (if (weightUnit == "lbs") {
                (profile.targetWeight * 2.20462)
            } else {
                profile.targetWeight
            }).toBigDecimal().setScale(1, RoundingMode.HALF_UP)
            "The user's name is ${profile.firstName}, age is ${profile.age}, and their target weight is $targetWeight $weightUnit."
        } else {
            "The user's profile information (name, age, target weight) is not available."
        }

        val dataSummary = if (data.isEmpty()) {
            "The user has no wellness data logged yet."
        } else {
            data.takeLast(30).joinToString(separator = "\n") { entry ->
                val displayWeight = if (weightUnit == "lbs") {
                    (entry.weight * 2.20462).toBigDecimal().setScale(1, RoundingMode.HALF_UP)
                } else {
                    entry.weight.toBigDecimal().setScale(1, RoundingMode.HALF_UP)
                }
                "- Date: ${entry.timestamp}, Weight: $displayWeight $weightUnit, Diet: ${entry.dietRating ?: "N/A"}, Activity: ${entry.activityLevel ?: "N/A"}, Sleep: ${entry.sleepHours ?: "N/A"}"
            }
        }

        return """
        You are a friendly and encouraging wellness assistant named 'Andy'. The user will ask you questions about their health and progress.
        Use the following user profile and historical wellness data to provide personalized, insightful, and supportive responses.
        Your goal is to help the user understand their trends and motivate them to achieve their goals.
        Keep your answers concise and easy to understand.
        
        User Profile:
        $userContext

        Here is the user's data from the last 30 days:
        $dataSummary
        """.trimIndent()
    }


    // Factory now provides both repositories
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatbotViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatbotViewModel(
                    WellnessDataRepository(application),
                    RegisterRepository(application) // Provide AuthRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
