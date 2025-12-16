package com.example.andromeda.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.UserProfile
import com.example.andromeda.data.WellnessDataRepository
// REMOVED: import com.example.andromeda.util.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// AI Suggested This: Unit tests for the HomeViewModel.
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // --- CHANGE 1: Define a test dispatcher directly in the class ---
    private val testDispatcher = StandardTestDispatcher()

    // REMOVED: The MainCoroutineRule is no longer needed
    // @get:Rule
    // val mainCoroutineRule = MainCoroutineRule()

    private lateinit var mockWellnessRepo: WellnessDataRepository
    private lateinit var mockAuthRepo: RegisterRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        // --- CHANGE 2: Manually set the main dispatcher for coroutines ---
        Dispatchers.setMain(testDispatcher)

        mockWellnessRepo = mockk()
        mockAuthRepo = mockk()

        // Mock the initial username loading in the init block
        coEvery { mockAuthRepo.getUserProfile() } returns UserProfile("Cag", "Okmen", 30, 80.0, "kg")

        viewModel = HomeViewModel(mockWellnessRepo, mockAuthRepo)
    }

    // --- CHANGE 3: Add a tearDown function to clean up the dispatcher ---
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generateSuggestions with empty data shows 'not enough data' message`() = runTest {
        // ARRANGE: Mock repository to return an empty list
        coEvery { mockWellnessRepo.allWellnessData } returns flowOf(emptyList())

        // ACT: Call the function
        viewModel.generateSuggestions()

        // ASSERT: Verify the UI state
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.suggestions.isNotEmpty())
        assertEquals("Not enough data to generate suggestions. Please add more entries.", state.suggestions.first())
        assertNull(state.error)
    }

    @Test
    fun `loadUserName populates userFirstName in uiState`() = runTest {
        // ARRANGE is done in setUp

        // ACT: The action happens in the ViewModel's init block, which runs during setup.
        // We need to advance the dispatcher to allow the coroutine in `init` to complete.
        testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Check if the user's first name was correctly loaded into the state.
        val state = viewModel.uiState.value
        assertEquals("Cag", state.userFirstName)
    }
}
