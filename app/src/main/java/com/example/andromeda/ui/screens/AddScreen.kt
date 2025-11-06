package com.example.andromeda.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.ui.theme.AndromedaTheme
import kotlin.math.roundToInt

@Composable
fun AddScreen(
    viewModel: AddViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show the dialog when the state becomes true
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ADD", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            WeightInput(
                weight = uiState.weight,
                onWeightChange = viewModel::onWeightChange
            )

            Spacer(modifier = Modifier.height(32.dp))

            WellnessRatingSlider(
                label = "Q1: How healthy was your diet?",
                value = uiState.dietRating,
                onValueChange = viewModel::onDietRatingChange
            )
            WellnessRatingSlider(
                label = "Q2: How active were you?",
                value = uiState.activityRating,
                onValueChange = viewModel::onActivityRatingChange
            )
            WellnessRatingSlider(
                label = "Q3: How many hours did you sleep?",
                value = uiState.sleepHours,
                onValueChange = viewModel::onSleepHoursChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            FloatingActionButton(
                onClick = viewModel::saveEntry,
                containerColor = Color(0xFF4CAF50),
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
        title = { Text("Saved!") },
        text = { Text("Your input has been saved.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}


@Composable
private fun WeightInput(weight: String, onWeightChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("How much did you weigh?", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                modifier = Modifier.width(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("lbs", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun WellnessRatingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("0")
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.weight(1f)
            )
            Text("10")
        }
        Text(
            text = value.roundToInt().toString(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.labelMedium
        )
    }
}


@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    AndromedaTheme {
        AddScreen()
    }
}
