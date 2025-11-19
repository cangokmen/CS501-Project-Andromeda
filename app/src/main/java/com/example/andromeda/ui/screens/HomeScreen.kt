package com.example.andromeda.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import com.example.andromeda.BuildConfig
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers // <-- Import Dispatchers
import kotlinx.coroutines.withContext // <-- Import withContext

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    // This parameter is no longer needed for the cards, but might be useful for other features.
    // For now, it's kept to maintain the signature from AppNavHost.
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())
    var geminiResponse by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) } // State to show a loading indicator

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
                                "Home Page",
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
            }
        }
        Button(onClick = {
            // Set loading to true and launch the coroutine
            isLoading = true
            coroutineScope.launch {
                try {
                    val responseText = withContext(Dispatchers.IO) { // <-- Perform network call on IO thread
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-2.5-pro",
                            apiKey = BuildConfig.GEMINI_API_KEY
                        )
                        val prompt = "Write a story about a magic backpack."
                        val response = generativeModel.generateContent(prompt)
                        response.text ?: "Error: No text in response"
                    }
                    geminiResponse = responseText
                } catch (e: Exception) {
                    // It's good practice to catch exceptions
                    geminiResponse = "Error: ${e.message}"
                } finally {
                    isLoading = false // Hide loading indicator
                }
            }
        }, enabled = !isLoading) { // Disable button while loading
            Text("Test Gemini")
        }

        // Show a loading indicator or the response
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        } else {
            Text(text = geminiResponse, modifier = Modifier.padding(top = 8.dp))
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
