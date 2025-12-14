package com.example.andromeda.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.ui.theme.AndromedaTheme
import com.example.andromeda.viewmodels.AddViewModel
import kotlin.math.roundToInt
import kotlin.text.isNotBlank


@Composable
fun AddScreen(
    selectedQuestions: Set<String>,
    // The ID or date of the entry to be edited/added, if any
    wellnessDataId: String? = null,
    onSaveComplete: () -> Unit,
    viewModel: AddViewModel = viewModel(
        // **FIX**: A simpler, unique key ensures the correct ViewModel instance is created.
        key = wellnessDataId ?: "new_entry",
        factory = AddViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            selectedQuestions,
            wellnessDataId // Pass the ID/date to the factory
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWeightEntered = uiState.weight.isNotBlank()

    if (uiState.showSaveConfirmation) {
        SaveConfirmationDialog(
            onConfirm = {
                // The ViewModel no longer handles navigation.
                // onSaveComplete is called directly to pop the back stack.
                onSaveComplete()
            }
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                if (uiState.isEditing) "Edit Entry" else "Add Entry",
                style = MaterialTheme.typography.headlineMedium
            )

            // **FIX**: Updated label to "kg"
            WeightInput(
                weight = uiState.weight,
                onWeightChange = viewModel::onWeightChange
            )

            val questionOrder = listOf("DIET", "ACTIVITY", "SLEEP", "WATER", "PROTEIN")
            val visibleQuestions = questionOrder.filter { it in selectedQuestions }

            visibleQuestions.forEachIndexed { index, questionKey ->
                val questionNumber = index + 1
                when (questionKey) {
                    "DIET" -> WellnessRatingSlider(
                        label = "Q$questionNumber: How would you rate your diet?",
                        value = uiState.dietRating,
                        onValueChange = viewModel::onDietRatingChange
                    )
                    "ACTIVITY" -> WellnessRatingSlider(
                        label = "Q$questionNumber: How would you rate your activity level?",
                        value = uiState.activityRating,
                        onValueChange = viewModel::onActivityRatingChange
                    )
                    "SLEEP" -> WellnessRatingSlider(
                        label = "Q$questionNumber: How would you rate your sleep?",
                        value = uiState.sleepHours,
                        onValueChange = viewModel::onSleepHoursChange
                    )
                    "WATER" -> WellnessRatingSlider(
                        label = "Q$questionNumber: How would you rate your water intake?",
                        value = uiState.waterIntake,
                        onValueChange = viewModel::onWaterIntakeChange
                    )
                    "PROTEIN" -> WellnessRatingSlider(
                        label = "Q$questionNumber: How would you rate your protein intake?",
                        value = uiState.proteinIntake,
                        onValueChange = viewModel::onProteinIntakeChange
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            FloatingActionButton(
                onClick = { if (isWeightEntered) viewModel.saveEntry() },
                containerColor = if (isWeightEntered) Color(0xFF4CAF50) else Color(0xFFA5D6A7),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Entry")
            }
        }
    }
}

@Composable
fun SaveConfirmationDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("Success!") },
        text = { Text("Your entry has been saved.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}

@Composable
fun WeightInput(weight: String, onWeightChange: (String) -> Unit) {
    OutlinedTextField(
        value = weight,
        onValueChange = onWeightChange,
        label = { Text("Weight (kg)") }, // **FIX**: Changed label to kg
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WellnessRatingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..10f,
    steps: Int = 8
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value.roundToInt().toString(), style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Good", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    AndromedaTheme(darkTheme = false){
        AddScreen(
            selectedQuestions = setOf("DIET", "SLEEP", "WATER"),
            onSaveComplete = {}
        )
    }
}
