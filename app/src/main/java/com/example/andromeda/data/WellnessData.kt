package com.example.andromeda.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

data class WellnessData(
    val timestamp: String = formatter.format(Calendar.getInstance().time),
    val sleepHours: Int?, // Make nullable
    val activityLevel: Int?, // Make nullable
    val dietRating: Int?, // Make nullable
    val weight: Double,
    val waterIntake: Int?, // New and nullable
    val proteinIntake: Int?  // New and nullable
)
