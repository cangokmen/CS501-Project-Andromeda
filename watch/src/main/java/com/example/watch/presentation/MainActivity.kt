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
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.watch.presentation.data.WatchDataRepository
import com.example.watch.presentation.theme.AndromedaTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Data Models and State (Unchanged) ---
data class WellnessCategory(val id: String, val icon: ImageVector, val label: String)
data class WatchUiState(
    val allCategories: List<WellnessCategory> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val questionValues: Map<String, Int> = emptyMap(),
    val currentScreen: Screen = Screen.Input
)
enum class Screen { Input, CategorySelection }

// --- ViewModel Layer ---
class WatchViewModel(
    application: Application,
    private val watchDataRepository: WatchDataRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    private val allWellnessCategories = listOf(
        WellnessCategory("DIET", Icons.Default.Fastfood, "Diet"),
        WellnessCategory("ACTIVITY", Icons.Default.DirectionsRun, "Activity"),
        WellnessCategory("SLEEP", Icons.Default.Hotel, "Sleep"),
        WellnessCategory("WATER", Icons.Default.LocalDrink, "Water"),
        WellnessCategory("PROTEIN", Icons.Default.Restaurant, "Protein")
    )

    init {
        viewModelScope.launch {
            watchDataRepository.selectedQuestions.collect { savedQuestionIds ->
                _uiState.update { currentState ->
                    currentState.copy(
                        allCategories = allWellnessCategories,
                        selectedCategoryIds = savedQuestionIds,
                        questionValues = savedQuestionIds.associateWith { id ->
                            currentState.questionValues.getOrElse(id) { 5 }
                        }
                    )
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun confirmCategorySelection() {
        viewModelScope.launch {
            watchDataRepository.saveSelectedQuestions(_uiState.value.selectedCategoryIds)
            navigateTo(Screen.Input)
        }
    }

    fun toggleCategorySelection(categoryId: String) {
        _uiState.update { currentState ->
            val currentSelection = currentState.selectedCategoryIds.toMutableSet()
            if (categoryId in currentSelection) {
                if (currentSelection.size > 1) currentSelection.remove(categoryId)
            } else {
                if (currentSelection.size < 3) currentSelection.add(categoryId)
            }
            val newValues = currentSelection.associateWith { id ->
                currentState.questionValues.getOrElse(id) { 5 }
            }
            currentState.copy(selectedCategoryIds = currentSelection, questionValues = newValues)
        }
    }

    fun onValueChange(question: String, newValue: Int) {
        _uiState.update {
            it.copy(questionValues = it.questionValues + (question to newValue))
        }
    }

    class Factory(
        private val application: Application,
        private val repository: WatchDataRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WatchViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WatchViewModel(application, repository) as T
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
                val repository = remember { WatchDataRepository(application) }
                val viewModel: WatchViewModel = viewModel(factory = WatchViewModel.Factory(application, repository))
                val uiState by viewModel.uiState.collectAsState()

                when (uiState.currentScreen) {
                    Screen.Input -> WellnessInputScreen(viewModel)
                    Screen.CategorySelection -> CategorySelectionScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CategorySelectionScreen(viewModel: WatchViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val remaining = 3 - uiState.selectedCategoryIds.size
        Text(
            text = if (remaining > 0) "Select $remaining more" else "Categories",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
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
                            // CORRECTED: Use theme color for border
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .padding(8.dp),
                    // CORRECTED: Use theme color for icon tint
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.confirmCategorySelection() },
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
    val activity = (context as? ComponentActivity)
    val dataClient = remember { Wearable.getDataClient(context) }
    val coroutineScope = rememberCoroutineScope()
    var weight by remember { mutableIntStateOf(150) }
    val selectedCategories = remember(uiState.selectedCategoryIds, uiState.allCategories) {
        uiState.allCategories.filter { it.id in uiState.selectedCategoryIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("New Entry", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.Black) }

            // Hardcoded Black for the "Weight" selector
            item {
                ValueSelector(
                    label = "Weight",
                    value = weight,
                    onValueChange = { weight = it },
                    range = 50..400,
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }

            // Hardcoded Black for the dynamic category selectors
            items(selectedCategories, key = { it.id }) { category ->
                var rating by remember(uiState.questionValues[category.id]) {
                    mutableStateOf(uiState.questionValues[category.id] ?: 5)
                }
                ValueSelector(
                    label = category.label,
                    value = rating,
                    onValueChange = { newValue ->
                        rating = newValue
                        viewModel.onValueChange(category.id, newValue)
                    },
                    range = 1..10,
                    color = androidx.compose.ui.graphics.Color.Black // Add this line
                )
            }

            item {
                Button(onClick = { viewModel.navigateTo(Screen.CategorySelection) }) {
                    Text("Edit Categories")
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        try {
                            val putDataMapRequest = PutDataMapRequest.create("/wellness_data").apply {
                                dataMap.putDouble("KEY_WEIGHT", weight.toDouble())
                                dataMap.putLong("KEY_TIMESTAMP", System.currentTimeMillis())
                                val userRatings = uiState.questionValues
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
                            activity?.finish()
                        } catch (e: Exception) {
                            println("WATCH: Error sending wellness data: $e")
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Submit")
                }
            }
        }
    }
}

@Composable
fun ValueSelector(label: String, value: Int, onValueChange: (Int) -> Unit, range: IntRange, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // FIX 1: Apply the color here.
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
            }
            // FIX 2: Apply the color here as well.
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center,
                color = color
            )
            Button(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}

