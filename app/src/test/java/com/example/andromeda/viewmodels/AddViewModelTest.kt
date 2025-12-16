package com.example.andromeda.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

// Conversion constant for testing, mirrors the one in the ViewModel.
private const val LBS_TO_KG = 0.453592

@ExperimentalCoroutinesApi
class AddViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Manually set up the test dispatcher, consistent with other test files.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockWellnessRepo: WellnessDataRepository
    private lateinit var mockUserPrefsRepo: UserPreferencesRepository
    private lateinit var viewModel: AddViewModel

    @Before
    fun setUp() {
        // Set the main dispatcher for coroutines.
        Dispatchers.setMain(testDispatcher)

        // Initialize mocks with a relaxed configuration.
        mockWellnessRepo = mockk(relaxed = true)
        mockUserPrefsRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        // Clean up the dispatcher after each test.
        Dispatchers.resetMain()
    }

    @Test
    fun `saveEntry converts weight from lbs to kg before saving`() = runTest {
        // ARRANGE
        // 1. Mock the user's preference to be "lbs".
        coEvery { mockUserPrefsRepo.weightUnit } returns flowOf("lbs")

        // 2. Initialize the ViewModel for creating a new entry (ID is null).
        viewModel = AddViewModel(mockWellnessRepo, mockUserPrefsRepo, emptySet(), null)

        // 3. Simulate the user entering a weight in lbs.
        val weightInLbs = "176.4" // This is approximately 80kg.
        viewModel.onWeightChange(weightInLbs)

        // 4. Create a "slot" to capture the WellnessData object that will be passed to the repository.
        val wellnessDataSlot = slot<WellnessData>()
        // When addWellnessData is called, capture its argument in our slot.
        coEvery { mockWellnessRepo.addWellnessData(capture(wellnessDataSlot)) } returns Unit

        // ACT
        // Call the function we want to test.
        viewModel.saveEntry()

        // Allow the coroutine launched in saveEntry to complete.
        testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        // 1. Verify that addWellnessData was called exactly once.
        coVerify(exactly = 1) { mockWellnessRepo.addWellnessData(any()) }

        // 2. Check the captured object's weight. It must be in kg.
        val capturedWeightInKg = wellnessDataSlot.captured.weight
        val expectedWeightInKg = weightInLbs.toDouble() * LBS_TO_KG

        // 3. Use a tolerance for comparing floating-point numbers to avoid precision errors.
        val tolerance = 0.01
        assertTrue(
            "Weight should be converted to kg for storage. Expected around $expectedWeightInKg, but got $capturedWeightInKg",
            abs(expectedWeightInKg - capturedWeightInKg) < tolerance
        )
    }
}
