package com.example.andromeda.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.andromeda.data.RegisterRepository
import com.example.andromeda.data.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * AI Suggested This: Unit tests for the RegisterViewModel.
 * This class focuses on testing the business logic for user registration and profile creation.
 */
@ExperimentalCoroutinesApi
class RegisterViewModelTest {

    // A JUnit rule that swaps the background executor used by Architecture Components with one that executes each task synchronously.
    // This is crucial for testing StateFlow and LiveData updates.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines, allowing us to control the execution of coroutines in tests.
    private val testDispatcher = StandardTestDispatcher()

    // We need a mock for EACH dependency of the ViewModel.
    private lateinit var mockRegisterRepository: RegisterRepository
    private lateinit var mockUserPrefsRepository: UserPreferencesRepository

    // The ViewModel instance we are testing.
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        // Sets the main coroutine dispatcher to our test dispatcher for the duration of the test.
        Dispatchers.setMain(testDispatcher)

        // Create mock instances for BOTH repositories.
        mockRegisterRepository = mockk(relaxed = true) // 'relaxed' allows the mock to return default values for non-stubbed functions.
        mockUserPrefsRepository = mockk(relaxed = true)

        // Initialize the ViewModel with BOTH mocked repositories.
        viewModel = RegisterViewModel(
            mockRegisterRepository,
            mockUserPrefsRepository
        )
    }

    @After
    fun tearDown() {
        // Resets the main dispatcher to the original one after the test is complete.
        Dispatchers.resetMain()
    }

    @Test
    fun `createProfile with valid data calls repository and updates state to Authenticated`() = runTest {
        // ARRANGE: Define the inputs and expected outcomes.
        val firstName = "Cag"
        val lastName = "Okmen"
        val age = 30
        val weight = 80.0
        val unit = "kg"

        // No specific setup for the mocks is needed here since we just want to verify they are called.

        // ACT: Call the function that we are testing.
        viewModel.createProfile(firstName, lastName, age, weight, unit)

        // This is important! The test dispatcher needs to run the coroutines launched in createProfile.
        testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verify that the expected actions occurred.
        // 1. Verify that the repository's 'createOrUpdateUserProfile' function was called exactly once with the correct parameters.
        coVerify(exactly = 1) {
            mockRegisterRepository.createOrUpdateUserProfile(firstName, lastName, age, weight, unit)
        }

        // 2. Assert that the final state of the ViewModel is Authenticated.
        assert(viewModel.registerState.value is RegisterState.Authenticated)
    }

    @Test
    fun `createProfile with blank first name does not call repository`() = runTest {
        // ARRANGE: Set up invalid inputs.
        val firstName = ""
        val lastName = "Okmen"
        val age = 30
        val weight = 80.0
        val unit = "kg"

        // ACT: Call the function.
        viewModel.createProfile(firstName, lastName, age, weight, unit)
        testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verify that the repository was NEVER called.
        coVerify(exactly = 0) {
            mockRegisterRepository.createOrUpdateUserProfile(any(), any(), any(), any(), any())
        }

        // Note: The validation logic is currently inside your Composable, not the ViewModel.
        // For this test to be more effective, that logic should be moved to the ViewModel.
        // As it stands, this test demonstrates what SHOULD happen if the ViewModel handled validation.
    }
}
