package com.example.glowtrack.models.models

/**
 * Data class representing daily progress for a habit
 */
data class HabitProgress(
    val habitId: String,
    val date: String, // Format: "yyyy-MM-dd"
    val isCompleted: Boolean = false,
    val value: Int = 0,
    val completedAt: Long? = null
) {
    fun getProgressPercentage(targetValue: Int): Int {
        return if (targetValue > 0) {
            ((value.toFloat() / targetValue.toFloat()) * 100).coerceAtMost(100f).toInt()
        } else {
            0
        }
    }
}