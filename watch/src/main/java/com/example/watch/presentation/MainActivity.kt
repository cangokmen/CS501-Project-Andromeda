package com.example.watch.presentation

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.watch.presentation.theme.AndromedaTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Data Models and State ---

// Represents a single wellness category with its identifier and icon
data class WellnessCategory(
    val id: String,
    val icon: ImageVector,
    val label: String
)

// UI state now includes all categories and the set of selected ones
data class WatchUiState(
    val allCategories: List<WellnessCategory> = emptyList(),
    val selectedCategoryIds: Set<String> = setOf("DIET", "ACTIVITY", "SLEEP"), // Default selection
    val questionValues: Map<String, Int> = emptyMap(),
    val currentScreen: Screen = Screen.Input // To navigate between input and settings
)

enum class Screen {
    Input,
    CategorySelection
}

// --- ViewModel Layer ---

class WatchViewModel(application: Application) : AndroidViewModel(application) {

    // --- REMOVED: All DataClient.OnDataChangedListener logic ---
    // The ViewModel is now self-contained and doesn't listen to the phone for questions.

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState = _uiState.asStateFlow()

    private val dataClient by lazy { Wearable.getDataClient(application) }

    // Define the 5 static categories directly in the ViewModel
    private val allWellnessCategories = listOf(
        WellnessCategory("DIET", Icons.Default.Fastfood, "Diet"),
        WellnessCategory("ACTIVITY", Icons.Default.DirectionsRun, "Activity"),
        WellnessCategory("SLEEP", Icons.Default.Hotel, "Sleep"),
        WellnessCategory("WATER", Icons.Default.LocalDrink, "Water"),
        WellnessCategory("PROTEIN", Icons.Default.Restaurant, "Protein")
    )

    init {
        // Initialize the state with all categories and default values for the selected ones
        _uiState.value = WatchUiState(
            allCategories = allWellnessCategories,
            questionValues = _uiState.value.selectedCategoryIds.associateWith { 5 }
        )
    }

    // --- NEW: Functions to manage category selection ---

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleCategorySelection(categoryId: String) {
        _uiState.update { currentState ->
            val currentSelection = currentState.selectedCategoryIds.toMutableSet()
            if (categoryId in currentSelection) {
                // Prevent removing if it's the last one
                if (currentSelection.size > 1) {
                    currentSelection.remove(categoryId)
                }
            } else {
                // Allow adding only if less than 3 are selected
                if (currentSelection.size < 3) {
                    currentSelection.add(categoryId)
                }
            }
            // Update values map to reflect the new selection
            val newValues = currentSelection.associateWith { id ->
                currentState.questionValues.getOrElse(id) { 5 }
            }
            currentState.copy(selectedCategoryIds = currentSelection, questionValues = newValues)
        }
    }


    // --- UNCHANGED: Functions to manage rating values ---

    fun onValueChange(question: String, newValue: Int) {
        _uiState.update {
            it.copy(questionValues = it.questionValues + (question to newValue))
        }
    }

    fun getQuestionValue(question: String): Int {
        return _uiState.value.questionValues.getOrElse(question) { 5 }
    }

    // --- UNCHANGED: Factory for ViewModel creation ---
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WatchViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WatchViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


// --- UI Layer for Watch ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndromedaTheme {
                val viewModel: WatchViewModel = viewModel(factory = WatchViewModel.Factory(application))
                val uiState by viewModel.uiState.collectAsState()

                // Simple navigation based on the ViewModel's state
                when (uiState.currentScreen) {
                    Screen.Input -> WellnessInputScreen(viewModel)
                    Screen.CategorySelection -> CategorySelectionScreen(viewModel)
                }
            }
        }
    }
}

// --- NEW: Category Selection Screen ---

@Composable
fun CategorySelectionScreen(viewModel: WatchViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- THIS TEXT CAN NOW BE DYNAMIC ---
        val remaining = 3 - uiState.selectedCategoryIds.size
        Text(
            text = if (remaining > 0) "Select $remaining more" else "Categories",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(16.dp))

        // Display all 5 icons in a flow layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.allCategories.forEach { category ->
                val isSelected = category.id in uiState.selectedCategoryIds
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    modifier = Modifier
                        .size(39.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.toggleCategorySelection(category.id) }
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .padding(8.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- ⭐ THIS IS THE MODIFIED BUTTON ⭐ ---
        // Button is now disabled unless exactly 3 categories are selected.
        Button(
            onClick = { viewModel.navigateTo(Screen.Input) },
            enabled = uiState.selectedCategoryIds.size == 3
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
        }
    }
}

@Composable
fun WellnessInputScreen(viewModel: WatchViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dataClient = remember { Wearable.getDataClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var weight by remember { mutableIntStateOf(150) }

    // Get the full category objects for the selected IDs to display them
    val selectedCategories = remember(uiState.selectedCategoryIds, uiState.allCategories) {
        uiState.allCategories.filter { it.id in uiState.selectedCategoryIds }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("New Entry", style = MaterialTheme.typography.titleMedium)
        }

        // Weight selector remains unchanged
        item {
            ValueSelector(
                label = "Weight",
                value = weight,
                onValueChange = { weight = it },
                range = 50..400
            )
        }

        // This now iterates over the 3 selected categories
        items(selectedCategories, key = { it.id }) { category ->
            ValueSelector(
                label = category.label,
                value = viewModel.getQuestionValue(category.id),
                onValueChange = { newValue -> viewModel.onValueChange(category.id, newValue) },
                range = 1..10
            )
        }

        item {
            // Button to navigate to the category selection screen
            Button(onClick = { viewModel.navigateTo(Screen.CategorySelection) }) {
                Text("Edit Categories")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(


                onClick = {
                    coroutineScope.launch {
                        try {
                            val putDataMapRequest = PutDataMapRequest.create("/wellness_data").apply {
                                // These values are always present
                                dataMap.putDouble("KEY_WEIGHT", weight.toDouble())
                                dataMap.putLong("KEY_TIMESTAMP", System.currentTimeMillis())

                                // --- THIS IS THE MODIFIED LOGIC ---
                                // Get the map of user-rated values
                                val userRatings = uiState.questionValues

                                // For each possible category, send its rating if it exists, otherwise send 0.
                                dataMap.putInt("KEY_Q1", userRatings.getOrElse("DIET") { 0 })
                                dataMap.putInt("KEY_Q2", userRatings.getOrElse("ACTIVITY") { 0 })
                                dataMap.putInt("KEY_Q3", userRatings.getOrElse("SLEEP") { 0 })
                                dataMap.putInt("KEY_Q4", userRatings.getOrElse("WATER") { 0 })
                                dataMap.putInt("KEY_Q5", userRatings.getOrElse("PROTEIN") { 0 })
                            }

                            println("WATCH: Sending DataMap: ${putDataMapRequest.dataMap}")

                            val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
                            dataClient.putDataItem(putDataRequest).await()
                            println("WATCH: Data sent successfully!")

                        } catch (e: Exception) {
                            println("WATCH: Error sending wellness data: $e")
                        }
                    }
                },
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Submit")
            }
        }
    }
}


// This composable remains unchanged, as it's a generic UI component
@Composable
fun ValueSelector(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )
            Button(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                // ⭐ THIS IS THE CORRECTED LINE ⭐
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
