package com.example.andromeda.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.andromeda.data.WellnessData
import androidx.compose.runtime.getValue
import com.example.andromeda.data.WellnessDataRepository

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (allWellnessData.isEmpty()) {
            Text("No wellness data has been saved yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allWellnessData.reversed()) { data ->
                    WellnessDataCard(data = data)
                }
            }
        }
    }
}
@Composable
fun WellnessDataCard(data: WellnessData) {
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
                Text(
                    "Weight: ${data.weight} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // --- MODIFIED: Build the list of metrics based on non-null data fields ---
            val metricsToShow = mutableListOf<@Composable () -> Unit>()

            // Check if each data point exists (is not null) before adding it to the list
            if (data.dietRating != null) {
                metricsToShow.add { Text("Diet: ${data.dietRating}/10") }
            }
            if (data.activityLevel != null) {
                metricsToShow.add { Text("Activity: ${data.activityLevel}/10") }
            }
            if (data.sleepHours != null) {
                metricsToShow.add { Text("Sleep: ${data.sleepHours}/10") }
            }
            if (data.waterIntake != null) {
                metricsToShow.add { Text("Water: ${data.waterIntake}/10") }
            }
            if (data.proteinIntake != null) {
                metricsToShow.add { Text("Protein: ${data.proteinIntake}/10") }
            }

            // Display the metrics in a 2-column layout
            metricsToShow.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp) // Add spacing between items
                ) {
                    rowItems.forEach { item ->
                        // Use weight to make columns equal width
                        Box(modifier = Modifier.weight(1f)) {
                            item()
                        }
                    }
                    // If there's only one item in the chunk, add a spacer to fill the row
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}