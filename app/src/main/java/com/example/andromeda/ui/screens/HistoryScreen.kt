package com.example.andromeda.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.andromeda.R
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    selectedQuestions: Set<String>,
    // Callback to navigate to the Add/Edit screen
    onEditEntry: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())

    // State to hold the date for the add/edit overlay
    var editingDate by remember { mutableStateOf<String?>(null) }
    // State for managing the DatePickerDialog
    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // The HistoryScreen content is the base layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Add Past Entry")
                }
            }

            // **FIX 1**: The `visibleData` variable is no longer needed, we can use `allWellnessData` directly.

            // Collapse multiple entries on the same date -> keep latest one per day
            val perDayLatest: List<WellnessData> = remember(allWellnessData) {
                allWellnessData
                    .groupBy { it.timestamp.take(10) }   // group by yyyy-MM-dd
                    .map { (_, list) -> list.last() }    // keep last entry for that date
                    .sortedByDescending { it.timestamp } // newest day first
            }

            if (perDayLatest.isEmpty()) {
                Text("No wellness data has been saved yet.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(perDayLatest) { data ->
                        // The onEdit lambda from WellnessDataCard now calls the onEditEntry from the NavHost
                        WellnessDataCard(data = data, onEdit = { onEditEntry(data.id.toString()) })
                    }
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { millis ->
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val selectedDate = formatter.format(Date(millis))
                                editingDate = selectedDate
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AnimatedVisibility(
            visible = editingDate != null,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
        ) {
            AddScreen(
                selectedQuestions = selectedQuestions,
                wellnessDataId = editingDate,
                onSaveComplete = { editingDate = null }
            )
        }
    }
}


@SuppressLint("SimpleDateFormat")
@Composable
fun WellnessDataCard(data: WellnessData, onEdit: () -> Unit) {
    val date = remember(data.timestamp) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(data.timestamp.take(10))
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                data.timestamp.take(10)
            }
        } catch (e: Exception) {
            data.timestamp.take(10)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.weight),
                        contentDescription = "Weight",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        // **FIX 3**: Changed "lbs" to "kg" to match the rest of the app.
                        "${data.weight} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (data.dietRating != null || data.activityLevel != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            data.dietRating?.let {
                                MetricItem(painterRes = R.drawable.diet, label = "Diet", value = "$it/10")
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            data.activityLevel?.let {
                                MetricItem(painterRes = R.drawable.activity, label = "Activity", value = "$it/10")
                            }
                        }
                    }
                }
                if (data.sleepHours != null || data.waterIntake != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            data.sleepHours?.let {
                                MetricItem(painterRes = R.drawable.sleep, label = "Sleep", value = "$it/10")
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            data.waterIntake?.let {
                                MetricItem(painterRes = R.drawable.water, label = "Water", value = "$it/10")
                            }
                        }
                    }
                }
                if (data.proteinIntake != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            MetricItem(
                                painterRes = R.drawable.protein,
                                label = "Protein",
                                value = "${data.proteinIntake}/10"
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(painterRes: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = painterRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
