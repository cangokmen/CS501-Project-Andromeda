package com.example.andromeda.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.WellnessDataRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatbotViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Manually set up the test dispatcher, consistent with other test files.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockWellnessRepo: WellnessDataRepository
    private lateinit var mockAuthRepo: RegisterRepository
    private lateinit var viewModel: ChatbotViewModel

    @Before
    fun setUp() {
        // Set the main dispatcher for coroutines.
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks with a relaxed configuration.
        mockWellnessRepo = mockk(relaxed = true)
        mockAuthRepo = mockk(relaxed = true)

        // Initialize the ViewModel with both mocked repositories.
        viewModel = ChatbotViewModel(mockWellnessRepo, mockAuthRepo)
    }

    @After
    fun tearDown() {
        // Clean up the dispatcher after each test.
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage adds user message and processing indicator immediately to UI state`() = runTest {
        // ARRANGE
        // The initial state has one greeting message.
        val initialMessageCount = viewModel.uiState.value.messages.size
        assertEquals(1, initialMessageCount)

        val userInput = "Hello, this is a test!"
        viewModel.onUserInputChange(userInput)

        // ACT
        // Call the function we want to test.
        viewModel.sendMessage()

        // ASSERT
        // We don't advance the dispatcher here because we want to test the *immediate* UI update
        // that happens before the AI response is fetched.

        val messages = viewModel.uiState.value.messages
        // The message list should now contain the initial greeting, the user's message, and a processing bubble.
        assertEquals(initialMessageCount + 2, messages.size)

        // Check the user's message (second to last).
        val userMessage = messages[initialMessageCount]
        assertEquals(userInput, userMessage.text)
        assertTrue(userMessage.isFromUser)

        // Check the processing indicator (last message).
        val processingMessage = messages[initialMessageCount + 1]
        assertTrue(processingMessage.isProcessing)
    }
}
