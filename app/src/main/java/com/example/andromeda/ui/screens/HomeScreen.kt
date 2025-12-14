package com.example.andromeda.ui.screens

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.math.RoundingMode


// Conversion constant
private const val KG_TO_LBS = 2.20462

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val repository = remember { WellnessDataRepository(context) }
    val allWellnessData by repository.allWellnessData.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()

    val userPrefsRepo = remember { UserPreferencesRepository(context) }
    val weightUnit by userPrefsRepo.weightUnit.collectAsState(initial = "kg")

    /*
     * AI Suggested this: To prevent visual clutter from multiple entries on the same day,
     * this logic processes the raw data. It groups entries by date, takes only the
     * most recent one from each day, and then sorts them for chronological display
     * in the graph and calendar.
     */
    // 2) Collapse multiple entries on the same date -> keep latest per day
    val dailyData = remember(allWellnessData) {
        allWellnessData
            .groupBy { it.timestamp.take(10) } // yyyy-MM-dd
            .map { (_, list) -> list.last() }
            .sortedBy { it.timestamp }
    }

    // Suggestions
    LaunchedEffect(Unit) { viewModel.generateSuggestions() }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Calendar", "Graph")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Home", style = MaterialTheme.typography.headlineMedium)

        // --- Combined Graph / Calendar card (swipeable) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        1 -> {
                            if (dailyData.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No wellness data has been saved yet.")
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WeightLineChart(
                                        data = dailyData,
                                        weightUnit = weightUnit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                }
                            }
                        }

                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WeightCalendar(
                                    weightData = dailyData,
                                    weightUnit = weightUnit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                }
            }
        }

        // --- Suggestions Section ---
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                else -> {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.suggestions.forEach { suggestion ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
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

/*
 * AI Suggested this: When making the line chart, 
 * collaboration with Gemini has been used. 
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeightLineChart(
    data: List<WellnessData>,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val recentSorted = remember(data, weightUnit) { // <-- Re-trigger on unit change
        val today = java.time.LocalDate.now()
        val cutoff = today.minusDays(14)
        data.mapNotNull { wd ->
            val d = runCatching { java.time.LocalDate.parse(wd.timestamp.take(10)) }.getOrNull()
            val displayWeight = if (weightUnit == "lbs") {
                wd.weight * KG_TO_LBS
            } else {
                wd.weight
            }
            d?.let { Triple(displayWeight, it, wd.timestamp) }
        }
            .filter { it.second >= cutoff }
            .sortedBy { it.second }
    }
    if (recentSorted.isEmpty()) return

    val weights = recentSorted.map { it.first }
    val datesFmt = recentSorted.map {
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
        val paddingLeft = 120f
        val paddingBottom = 72f
        val paddingTop = 70f
        val paddingRight = 80f

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
            drawLine(gridColor, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), strokeWidth = 1f)
        }

        drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, originY), strokeWidth = 2f)
        drawLine(axisColor, Offset(paddingLeft, originY), Offset(size.width - paddingRight, originY), strokeWidth = 2f)

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

        for (i in 0..ySteps) {
            val value = min + (i / ySteps.toDouble()) * range
            val y = paddingTop + (1 - (i / ySteps.toFloat())) * h
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", value),
                paddingLeft - 120f,
                y + 0f,
                paintY
            )
        }

        val maxLabels = 6
        val every = (n / maxLabels).coerceAtLeast(1)
        for (i in 0 until n step every) {
            val x = xy(i).x
            val label = datesFmt[i]
            drawLine(axisColor, Offset(x, originY), Offset(x, originY + 6f), strokeWidth = 2f)
            val tw = paintX.measureText(label)
            drawContext.canvas.nativeCanvas.drawText(label, x - tw / 2f, originY + 42f, paintX)
        }

        val path = Path().apply {
            moveTo(xy(0).x, xy(0).y)
            for (i in 1 until n) lineTo(xy(i).x, xy(i).y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))
        for (i in 0 until n) drawCircle(pointColor, radius = 6f, center = xy(i))

        drawContext.canvas.nativeCanvas.drawText("Date", size.width / 2 - 30f, size.height - (paddingBottom/4) + 20, paintX)
        drawContext.canvas.nativeCanvas.drawText("Weight ($weightUnit)", paddingLeft - 120f, paddingTop - 50f, paintY)
    }
}

/*
 * AI Suggested this: When making the calendar, 
 * collaboration with Gemini has been used. 
 */
// Weight Calendar
@Composable
private fun WeightCalendar(
    weightData: List<WellnessData>,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    val weightByDate = remember(weightData, weightUnit) {
        weightData.associate {
            val displayWeight = if (weightUnit == "lbs") {
                it.weight * KG_TO_LBS
            } else {
                it.weight
            }
            it.timestamp.take(10) to displayWeight
        }
    }

    val today = remember { Calendar.getInstance() }
    var displayMonth by rememberSaveable { mutableStateOf(today.get(Calendar.MONTH)) }
    var displayYear by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR)) }

    val calendar = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val emptyCellsBefore = (firstDayOfWeek - Calendar.SUNDAY + 7) % 7

    Column(modifier = modifier) {
        // --- Month Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (displayMonth == 0) {
                    displayMonth = 11
                    displayYear--
                } else {
                    displayMonth--
                }
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
            }
            Text(
                monthFormatter.format(calendar.time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                if (displayMonth == 11) {
                    displayMonth = 0
                    displayYear++
                } else {
                    displayMonth++
                }
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
            }
        }

        // --- Days of Week Header ---
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // --- Calendar Grid ---
        val totalCells = 42 // 6 weeks
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 0 until 7) {
                    val cellIndex = week * 7 + day
                    val dayOfMonth = cellIndex - emptyCellsBefore + 1
                    if (cellIndex < emptyCellsBefore || dayOfMonth > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) // Empty cell
                    } else {
                        val dateKey = String.format(
                            "%04d-%02d-%02d",
                            displayYear,
                            displayMonth + 1,
                            dayOfMonth
                        )
                        val weight = weightByDate[dateKey]
                        CalendarDay(day = dayOfMonth.toString(), weight = weight)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.CalendarDay(day: String, weight: Double?) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        if (weight != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Text(day, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = weight.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(day, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
