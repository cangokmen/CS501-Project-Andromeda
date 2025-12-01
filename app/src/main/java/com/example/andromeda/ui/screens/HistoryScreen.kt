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
    selectedQuestions: Set<String>,
    currentUserEmail: String?
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())

    // Filter to this user's data (if we know the email)
    val visibleData = remember(allWellnessData, currentUserEmail) {
        if (currentUserEmail.isNullOrBlank()) {
            allWellnessData
        } else {
            allWellnessData.filter { it.userEmail == currentUserEmail }
        }
    }

    // Collapse multiple entries on the same date -> keep latest one per day
    val perDayLatest: List<WellnessData> = remember(visibleData) {
        visibleData
            .groupBy { it.timestamp.take(10) }   // group by yyyy-MM-dd
            .map { (_, list) -> list.last() }    // keep last entry for that date
            .sortedByDescending { it.timestamp } // newest day first
    }

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

        if (perDayLatest.isEmpty()) {
            Text("No wellness data has been saved yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(perDayLatest) { data ->
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
                    "Weight: ${data.weight} lbs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val metricsToShow = mutableListOf<@Composable () -> Unit>()

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

            // 2-column layout
            metricsToShow.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            item()
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
