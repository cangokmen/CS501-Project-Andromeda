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


@Composable
fun AddScreen(
    selectedQuestions: Set<String>,
    currentUserEmail: String?,
    viewModel: AddViewModel = viewModel(
        key = selectedQuestions.toString() + (currentUserEmail ?: ""),
        factory = AddViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            selectedQuestions,
            currentUserEmail
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val isWeightEntered = uiState.weight.isNotBlank()

    if (uiState.showSaveConfirmation) {
        SaveConfirmationDialog(
            onConfirm = viewModel::dismissSaveConfirmation
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // --- MODIFIED: Increased spacing from 16.dp to 24.dp ---
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("ADD", style = MaterialTheme.typography.headlineMedium)

            WeightInput(
                weight = uiState.weight,
                onWeightChange = viewModel::onWeightChange
            )

            // Define the order of questions to ensure consistency
            val questionOrder = listOf("DIET", "ACTIVITY", "SLEEP", "WATER", "PROTEIN")
            // Filter the ordered list by the user's selection
            val visibleQuestions = questionOrder.filter { it in selectedQuestions }

            // Loop through the visible questions and assign numbers dynamically
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
        label = { Text("Weight (lbs)") },
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
    }
}


@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    AndromedaTheme(darkTheme = false){
        // Example: If only these 3 are selected, they will be numbered Q1, Q2, Q3
        AddScreen(selectedQuestions = setOf("DIET", "SLEEP", "WATER"),
            currentUserEmail = null)

    }
}
