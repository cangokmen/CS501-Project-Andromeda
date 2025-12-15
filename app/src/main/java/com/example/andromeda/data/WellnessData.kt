package com.example.andromeda.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/**
 * Represents a single day's wellness data entry.
 * It includes a unique ID for editing and deleting specific entries.
 */
data class WellnessData(
    // A unique identifier for each entry. Essential for editing/deleting.
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = formatter.format(Calendar.getInstance().time),
    val weight: Double,
    val sleepHours: Int?,
    val activityLevel: Int?,
    val dietRating: Int?,
    val waterIntake: Int?,
    val proteinIntake: Int?
)
