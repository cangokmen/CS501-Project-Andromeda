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
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.example.andromeda.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                                Text("No wellness data has been saved yet.")
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WeightLineChart(
                                        data = dailyData,
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
                                    modifier = Modifier.fillMaxSize()//bounded
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
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeightLineChart(
    data: List<WellnessData>,
    modifier: Modifier = Modifier
) {
    val recentSorted = remember(data) {
        val today = java.time.LocalDate.now()
        val cutoff = today.minusDays(14)
        data.mapNotNull { wd ->
            val d = runCatching { java.time.LocalDate.parse(wd.timestamp.take(10)) }.getOrNull()
            d?.let { Triple(wd.weight, it, wd.timestamp) }
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

        drawContext.canvas.nativeCanvas.drawText("Date", size.width / 2 - 30f, size.height + 20f, paintX)
        drawContext.canvas.nativeCanvas.drawText("Weight (lb)", paddingLeft - 120f, paddingTop - 50f, paintY)
    }
}

/* ----------------------- CALENDAR (STICKY HEADER) ----------------------- */

private data class HomeCalendarCell(
    val year: Int,
    val month: Int, // 0-based
    val day: Int,
    val inCurrentMonth: Boolean
)

@Composable
private fun WeightCalendar(
    weightData: List<WellnessData>,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    val weightByDate = remember(weightData) {
        weightData.associate { it.timestamp.take(10) to it.weight }
    }

    val today = remember { Calendar.getInstance() }
    var displayMonth by rememberSaveable { mutableStateOf(today.get(Calendar.MONTH)) }
    var displayYear by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR)) }

    val firstOfMonth = remember(displayYear, displayMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val monthLabel = remember(displayYear, displayMonth) {
        monthFormatter.format(firstOfMonth.time)
    }

    val cells: List<HomeCalendarCell> = remember(displayYear, displayMonth) {
        val firstDow = firstOfMonth.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        val leading = firstDow - Calendar.SUNDAY // 0..6

        val start = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leading)
        }

        val endOfMonth = (firstOfMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        val lastDayIf35 = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 34) }
        val totalCells = if (lastDayIf35.before(endOfMonth)) 42 else 35

        buildList {
            repeat(totalCells) { i ->
                val c = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
                add(
                    HomeCalendarCell(
                        year = c.get(Calendar.YEAR),
                        month = c.get(Calendar.MONTH),
                        day = c.get(Calendar.DAY_OF_MONTH),
                        inCurrentMonth = (c.get(Calendar.YEAR) == displayYear && c.get(Calendar.MONTH) == displayMonth)
                    )
                )
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {

        // ---- Sticky header ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (displayMonth == 0) {
                        displayMonth = 11
                        displayYear -= 1
                    } else displayMonth -= 1
                }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Previous month") }

                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                IconButton(onClick = {
                    if (displayMonth == 11) {
                        displayMonth = 0
                        displayYear += 1
                    } else displayMonth += 1
                }) { Icon(Icons.Filled.ArrowForward, contentDescription = "Next month") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // ---- Scrollable grid (bounded height) ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)                 // IMPORTANT: gives finite height -> no crash
                .verticalScroll(scrollState)
        ) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        val alphaText = if (cell.inCurrentMonth) 1f else 0.25f
                        val circleAlpha = if (cell.inCurrentMonth) 0.10f else 0.05f

                        val key = "%04d-%02d-%02d".format(cell.year, cell.month + 1, cell.day)
                        val weight = weightByDate[key]

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = circleAlpha),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell.day.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaText)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (cell.inCurrentMonth && weight != null) {
                                Text(
                                    text = "${weight.toInt()} lb",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}