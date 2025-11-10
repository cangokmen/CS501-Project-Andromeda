package com.example.andromeda.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HistoryScreen(
    // This parameter is no longer needed for the cards, but might be useful for other features.
    // For now, it's kept to maintain the signature from AppNavHost.
    selectedQuestions: Set<String>
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (allWellnessData.isEmpty()) {
            Text("No wellness data has been saved yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Pinned header: title + chart
                stickyHeader {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                "Wellness History",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            WeightLineChart(
                                data = allWellnessData, // chronological spacing
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }

                // The cards list
                items(allWellnessData.reversed()) { data ->
                    WellnessDataCard(data = data)
                }
            }
        }
    }
}

// --- MODIFIED: The card is now self-contained and doesn't need selectedQuestions ---
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
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeightLineChart(
    data: List<WellnessData>,
    modifier: Modifier = Modifier
) {
    // Build rolling 30-day series, sorted oldest → newest
    val recentSorted = remember(data) {
        val today = java.time.LocalDate.now()
        val cutoff = today.minusDays(30)
        data.mapNotNull { wd ->
            val d = runCatching { java.time.LocalDate.parse(wd.timestamp.take(10)) }.getOrNull()
            d?.let { Triple(wd.weight, it, wd.timestamp) } // (weight, dateOnly, rawTs)
        }
            .filter { it.second >= cutoff }
            .sortedBy { it.second }
    }
    if (recentSorted.isEmpty()) return

    val weights: List<Double> = recentSorted.map { it.first }
    val datesFmt: List<String> = recentSorted.map {
        it.second.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
    }
    val n = weights.size

    // Theme-aware colors and text sizes
    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val textSizeY = with(density) { 12.sp.toPx() }
    val textSizeX = with(density) { 13.sp.toPx() }

    Canvas(modifier = modifier) {
        // generous padding
        val paddingLeft = 110f
        val paddingBottom = 72f
        val paddingTop = 44f
        val paddingRight = 44f

        val w = size.width - paddingLeft - paddingRight
        val h = size.height - paddingTop - paddingBottom

        val min = weights.minOrNull() ?: 0.0
        val max = weights.maxOrNull() ?: 1.0
        val range = (max - min).let { if (it <= 1e-4) 1.0 else it }

        fun xy(i: Int): Offset {
            val x = paddingLeft + (if (n == 1) 0f else (i.toFloat() / (n - 1)) * w)
            val yRatio = ((weights[i] - min) / range).toFloat()
            val y = paddingTop + (1 - yRatio) * h
            return Offset(x, y)
        }

        val originY = size.height - paddingBottom

        // optional horizontal grid lines (match Y ticks)
        val ySteps = 3
        for (i in 0..ySteps) {
            val y = paddingTop + (1 - (i / ySteps.toFloat())) * h
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        // axes
        drawLine(axisColor,
            Offset(paddingLeft, paddingTop),
            Offset(paddingLeft, originY),
            strokeWidth = 2f)

        drawLine(axisColor,
            Offset(paddingLeft, originY),
            Offset(size.width - paddingRight, originY),
            strokeWidth = 2f)

        // paints for text
        val paintY = android.graphics.Paint().apply {
            color = axisColor.toArgb()
            textSize = textSizeY
            isAntiAlias = true
        }
        val paintX = android.graphics.Paint().apply {
            color = axisColor.toArgb()
            textSize = textSizeX
            isAntiAlias = true
        }

        // Y ticks/labels
        for (i in 0..ySteps) {
            val value = min + (i / ySteps.toDouble()) * range
            val y = paddingTop + (1 - (i / ySteps.toFloat())) * h
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", value),
                paddingLeft - 88f,
                y + 10f,
                paintY
            )
        }

        // X labels aligned to points; avoid overlap
        val maxLabels = 6
        val every = (n / maxLabels).coerceAtLeast(1)
        for (i in 0 until n step every) {
            val x = xy(i).x
            val label = datesFmt[i]
            drawLine(axisColor, Offset(x, originY), Offset(x, originY + 6f), strokeWidth = 2f)
            val tw = paintX.measureText(label)
            drawContext.canvas.nativeCanvas.drawText(label, x - tw / 2f, originY + 42f, paintX)
        }

        // line + points
        val path = Path().apply {
            moveTo(xy(0).x, xy(0).y)
            for (i in 1 until n) lineTo(xy(i).x, xy(i).y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))
        for (i in 0 until n) drawCircle(pointColor, radius = 6f, center = xy(i))

        // titles
        drawContext.canvas.nativeCanvas.drawText(
            "Date",
            size.width / 2 - 30f,
            size.height + 20f,
            paintX)

        drawContext.canvas.nativeCanvas.drawText(
            "Weight (lb)",
            paddingLeft - 100f,
            paddingTop - 50f,
            paintY )
    }
}


