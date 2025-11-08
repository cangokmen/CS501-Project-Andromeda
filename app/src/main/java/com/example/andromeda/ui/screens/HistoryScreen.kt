package com.example.andromeda.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository

@Composable
fun HistoryScreen(
    // Accept the selectedQuestions set
    selectedQuestions: Set<String>
) {
    // 1. Get the context and create an instance of the repository
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }

    // 2. Collect the flow of wellness data as state
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Wellness History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 3. Use LazyColumn to display the list of data efficiently
        if (allWellnessData.isEmpty()) {
            Text("No wellness data has been saved yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Reverse the list to show the most recent entries first
                items(allWellnessData.reversed()) { data ->
                    // Pass the selectedQuestions down to the card
                    WellnessDataCard(data = data, selectedQuestions = selectedQuestions)
                }
            }
        }
    }
}

@Composable
fun WellnessDataCard(data: WellnessData, selectedQuestions: Set<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date: ${data.timestamp}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Always display weight
                Text(
                    "Weight: ${data.weight} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Dynamically display selected metrics
            // Using a chunked approach to keep two items per row
            val metricsToShow = mutableListOf<@Composable () -> Unit>()

            if ("DIET" in selectedQuestions && data.dietRating != null) {
                metricsToShow.add { Text("Diet: ${data.dietRating}/10") }
            }
            if ("ACTIVITY" in selectedQuestions && data.activityLevel != null) {
                metricsToShow.add { Text("Activity: ${data.activityLevel}/10") }
            }
            if ("SLEEP" in selectedQuestions && data.sleepHours != null) {
                metricsToShow.add { Text("Sleep: ${data.sleepHours} hrs") }
            }
            if ("WATER" in selectedQuestions && data.waterIntake != null) {
                metricsToShow.add { Text("Water: ${data.waterIntake} glasses") }
            }
            if ("PROTEIN" in selectedQuestions && data.proteinIntake != null) {
                metricsToShow.add { Text("Protein: ${data.proteinIntake}g") }
            }

            metricsToShow.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Ensure the row has two items to avoid layout issues
                    // The first item will always exist if the row exists.
                    rowItems[0]()
                    // If a second item exists, display it. Otherwise, leave the space empty.
                    if (rowItems.size > 1) {
                        rowItems[1]()
                    } else {
                        Spacer(modifier = Modifier) // Placeholder
                    }
                }
            }
        }
    }
}
