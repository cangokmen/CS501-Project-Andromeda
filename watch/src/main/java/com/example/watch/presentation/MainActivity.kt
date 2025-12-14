package com.example.watch.presentation

import android.app.Application
import android.os.Bundle
import android.util.Log
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
import com.example.watch.presentation.data.WatchDataRepository
import com.example.watch.presentation.theme.AndromedaTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// REMOVED: import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

data class WellnessCategory(val id: String, val icon: ImageVector, val label: String)
data class WatchUiState(
    val allCategories: List<WellnessCategory> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val questionValues: Map<String, Int> = emptyMap(),
    val currentScreen: Screen = Screen.Input,
    val averageWeight: Int = 150 // Default initial value
)
enum class Screen { Input, CategorySelection }

// ViewModelLayer
class WatchViewModel(
    application: Application,
    private val watchDataRepository: WatchDataRepository
) : AndroidViewModel(application), MessageClient.OnMessageReceivedListener {

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    private val messageClient by lazy { Wearable.getMessageClient(application) }
    private val nodeClient by lazy { Wearable.getNodeClient(application) }


    private val allWellnessCategories = listOf(
        WellnessCategory("DIET", Icons.Default.Fastfood, "Diet"),
        WellnessCategory("ACTIVITY", Icons.Default.DirectionsRun, "Activity"),
        WellnessCategory("SLEEP", Icons.Default.Hotel, "Sleep"),
        WellnessCategory("WATER", Icons.Default.LocalDrink, "Water"),
        WellnessCategory("PROTEIN", Icons.Default.Restaurant, "Protein")
    )

    init {
        messageClient.addListener(this)
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

    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(this)
    }


    fun requestAverageWeight() {
        Log.d("WatchViewModel", "Requesting average weight from phone.")
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.firstOrNull()?.id?.let { nodeId ->
                messageClient.sendMessage(nodeId, "/request_average_weight", byteArrayOf())
                    .addOnSuccessListener { Log.d("WatchViewModel", "Request message sent successfully.") }
                    .addOnFailureListener { e -> Log.e("WatchViewModel", "Failed to send request message.", e) }
            }
        }
            .addOnFailureListener { e -> Log.e("WatchViewModel", "Failed to get connected nodes.", e) }
    }

    /*
     * AI Suggested this: To safely handle incoming data from the Wearable Data Layer,
     * this override function was implemented. It checks the message path, safely parses
     * the byte array to a string and then to a Double, and finally updates the
     * ViewModel's state, ensuring robust communication between devices.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/average_weight_response") {
            val averageWeightStr = String(messageEvent.data)
            Log.d("WatchViewModel", "Received average weight response: $averageWeightStr")
            averageWeightStr.toDoubleOrNull()?.let {
                _uiState.update { currentState ->
                    currentState.copy(averageWeight = it.roundToInt())
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
            val newQuestionValues = it.questionValues.toMutableMap()
            newQuestionValues[question] = newValue
            it.copy(questionValues = newQuestionValues)
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


// UI Layer
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .padding(8.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.confirmCategorySelection() },
            enabled = uiState.selectedCategoryIds.size == 3
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
            }
        }
    }
}

@Composable
fun WellnessInputScreen(viewModel: WatchViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)
    val dataClient = remember { Wearable.getDataClient(context) }
    var weight by remember(uiState.averageWeight) { mutableIntStateOf(uiState.averageWeight) }

    LaunchedEffect(Unit) {
        viewModel.requestAverageWeight()
    }

    val selectedCategories = remember(uiState.selectedCategoryIds, uiState.allCategories) {
        uiState.allCategories.filter { it.id in uiState.selectedCategoryIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("New Entry", style = MaterialTheme.typography.titleMedium, color = Color.Black) }

            item {
                ValueSelector(
                    label = "Weight",
                    value = weight,
                    onValueChange = { weight = it },
                    range = 50..400,
                    color = Color.Black
                )
            }

            /*
             * AI Suggested this: To create a dynamic list of inputs based on user
             * preferences, this `items` block iterates through only the selected
             * categories. For each one, it creates a `ValueSelector` and manages its
             * state, ensuring the UI always matches the user's chosen settings.
             */
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
                    color = Color.Black
                )
            }

            item {
                Button(onClick = { viewModel.navigateTo(Screen.CategorySelection) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Edit Categories")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
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
                    val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
                    dataClient.putDataItem(putDataRequest).apply {
                        addOnSuccessListener {
                            Log.d("WellnessInput", "Data sent successfully.")
                            activity?.finish()
                        }
                        addOnFailureListener { e ->
                            Log.e("WellnessInput", "Error sending wellness data", e)
                        }
                    }
                }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun ValueSelector(label: String, value: Int, onValueChange: (Int) -> Unit, range: IntRange, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease")
                }
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center,
                color = color
            )
            Button(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase")
                }
            }
        }
    }
}