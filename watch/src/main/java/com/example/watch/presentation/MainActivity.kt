package com.example.watch.presentation

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.watch.presentation.theme.AndromedaTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Data Layer for Watch ---

data class WatchUiState(
    val questions: List<String> = listOf("DIET", "ACTIVITY", "SLEEP"),
    val questionValues: Map<String, Int> = questions.associateWith { 5 }
)

class WatchViewModel(application: Application) : AndroidViewModel(application),
    DataClient.OnDataChangedListener {
    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState = _uiState.asStateFlow()

    private val dataClient by lazy { Wearable.getDataClient(application) }

    init {
        dataClient.addListener(this)
        fetchInitialConfig()
    }

    private fun fetchInitialConfig() {
        viewModelScope.launch {
            try {
                val dataItems = dataClient.getDataItems(
                    PutDataMapRequest.create("/config_questions").asPutDataRequest().uri
                ).await()
                dataItems.firstOrNull()?.let { item ->
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    processQuestionConfig(dataMap)
                }
                dataItems.release()
            } catch (e: Exception) {
                println("Error fetching initial watch config: $e")
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/config_questions") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                processQuestionConfig(dataMap)
            }
        }
        dataEvents.release()
    }

    private fun processQuestionConfig(dataMap: DataMap) {
        val questionsJson = dataMap.getString("KEY_QUESTIONS_LIST") ?: return
        try {
            val type = object : TypeToken<Set<String>>() {}.type
            val questions: Set<String> = Gson().fromJson(questionsJson, type)

            _uiState.update { currentState ->
                val newValues = questions.associateWith { question ->
                    currentState.questionValues.getOrElse(question) { 5 }
                }
                currentState.copy(questions = questions.toList(), questionValues = newValues)
            }
        } catch (e: Exception) {
            println("Error parsing questions JSON on watch: $e")
        }
    }

    fun onValueChange(question: String, newValue: Int) {
        _uiState.update {
            it.copy(questionValues = it.questionValues + (question to newValue))
        }
    }

    fun getQuestionValue(question: String): Int {
        return _uiState.value.questionValues.getOrElse(question) { 5 }
    }

    override fun onCleared() {
        super.onCleared()
        dataClient.removeListener(this)
    }

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
                WellnessInputScreen(viewModel)
            }
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

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "New Wellness Entry",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }

        item {
            ValueSelector(
                label = "Weight",
                value = weight,
                onValueChange = { weight = it },
                range = 50..400
            )
        }

        items(uiState.questions, key = { it }) { question ->
            ValueSelector(
                label = question.replaceFirstChar { it.titlecase() }, // "DIET" -> "Diet"
                value = viewModel.getQuestionValue(question),
                onValueChange = { newValue -> viewModel.onValueChange(question, newValue) },
                range = 0..10
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Main Submit Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val putDataMapRequest = PutDataMapRequest.create("/wellness_data").apply {
                                dataMap.putInt("KEY_WEIGHT", weight)
                                uiState.questionValues.forEach { (key, value) ->
                                    when (key) {
                                        "DIET" -> dataMap.putInt("KEY_Q1", value)
                                        "ACTIVITY" -> dataMap.putInt("KEY_Q2", value)
                                        "SLEEP" -> dataMap.putInt("KEY_Q3", value)
                                        "WATER" -> dataMap.putInt("KEY_Q4", value)
                                        "PROTEIN" -> dataMap.putInt("KEY_Q5", value)
                                    }
                                }
                                dataMap.putLong("KEY_TIMESTAMP", System.currentTimeMillis())
                            }
                            val putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent()
                            dataClient.putDataItem(putDataRequest).await()
                            println("Wellness data sent successfully!")
                        } catch (e: Exception) {
                            println("Error sending wellness data: $e")
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Submit Entry",
                    modifier = Modifier.size(ButtonDefaults.LargeIconSize)
                )
            }
        }
    }
}

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
            // Decrease Button
            Button(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                enabled = value > range.first,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                // THE FIX: Remove `buttonType` and use a modifier with an explicit size.
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }

            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center
            )

            // Increase Button
            Button(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                enabled = value < range.last,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                // THE FIX: Remove `buttonType` and use a modifier with an explicit size.
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize)
                )
            }
        }
    }
}
