package com.example.andromeda.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Define a formatter using the older SimpleDateFormat class
private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

data class WellnessData(
    // Get the current date and format it as a string
    val timestamp: String = formatter.format(Calendar.getInstance().time),
    val sleepHours: Int,
    val activityLevel: Int,
    val dietRating: Int,
    val weight: Double
)