package com.example.andromeda.ui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    currentUserEmail: String?,   // gets passed from AppNavHost
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()

    // 1) Filter to current user
    val visibleData = remember(allWellnessData, currentUserEmail) {
        if (currentUserEmail.isNullOrBlank()) {
            allWellnessData
        } else {
            allWellnessData.filter { it.userEmail == currentUserEmail }
        }
    }

    // 2) Collapse multiple entries on the same date -> keep latest per day
    val dailyData = remember(visibleData) {
        visibleData
            .groupBy { it.timestamp.take(10) }   // yyyy-MM-dd
            .map { (_, list) ->
                // same as HistoryScreen: use the last item in insertion order
                list.last()
            }
            .sortedBy { it.timestamp }           // oldest → newest
    }

    // Suggestions
    LaunchedEffect(Unit) {
        viewModel.generateSuggestions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Home",
            style = MaterialTheme.typography.headlineMedium,
        )

        if (dailyData.isEmpty()) {
            Text("No wellness data has been saved yet.")
        } else {
            Text(
                "Last 10-Day Weight Trend",
                style = MaterialTheme.typography.titleMedium,
            )
            WeightLineChart(
                data = dailyData,      //use per-day-latest data
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        // --- Suggestions Section ---
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        uiState.suggestions.forEach { suggestion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                // Set the background of the card to the theme's surface color (LightGreen)
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    // Ensure text color is readable on the new background
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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

    val lastDayCount = 10;
    // Build rolling 10-day series, sorted oldest → newest
    val recentSorted = remember(data) {
        val today = java.time.LocalDate.now()
        val cutoff = today.minusDays(lastDayCount.toLong())
        data.mapNotNull { wd ->
            val d = runCatching { java.time.LocalDate.parse(wd.timestamp.take(lastDayCount)) }.getOrNull()
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

    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val textSizeY = with(density) { 12.sp.toPx() }
    val textSizeX = with(density) { 13.sp.toPx() }

    Canvas(modifier = modifier) {
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
        drawLine(
            axisColor,
            Offset(paddingLeft, paddingTop),
            Offset(paddingLeft, originY),
            strokeWidth = 2f
        )

        drawLine(
            axisColor,
            Offset(paddingLeft, originY),
            Offset(size.width - paddingRight, originY),
            strokeWidth = 2f
        )

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

        // Y ticks
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

        // X labels
        val maxLabels = 6
        val every = (n / maxLabels).coerceAtLeast(1)
        for (i in 0 until n step every) {
            val x = xy(i).x
            val label = datesFmt[i]
            drawLine(axisColor, Offset(x, originY), Offset(x, originY + 6f), strokeWidth = 2f)
            val tw = paintX.measureText(label)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - tw / 2f,
                originY + 42f,
                paintX
            )
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
            paintX
        )

        drawContext.canvas.nativeCanvas.drawText(
            "Weight (lb)",
            paddingLeft - 100f,
            paddingTop - 50f,
            paintY
        )
    }
}
