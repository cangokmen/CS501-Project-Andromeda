package com.example.andromeda.data

/**
 * Represents the data model for a user's profile, stored locally.
 */
data class UserProfile(
    val email: String,
    val passwordHash: String, // In a real app, this should be a secure hash, not plain text
    val firstName: String,
    val lastName: String,
    val age: Int,
    val targetWeight: Double
)
