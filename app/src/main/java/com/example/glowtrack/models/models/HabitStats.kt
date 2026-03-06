package com.example.glowtrack.models.models

/**
 * Data class for habit statistics
 */
data class HabitStats(
    val habitId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val completionRate: Float = 0f // Percentage
)