package com.example.andromeda.data

// An enum to represent the weight units for type safety
enum class WeightUnit {
    KG, LB
}

/**
 * Represents the single user's profile data.
 * All login and multi-user fields have been removed.
 */
data class UserProfile(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val targetWeight: Double,
    val weightUnit: WeightUnit
)
