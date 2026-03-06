package com.example.glowtrack.models.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * Data class representing a daily habit
 */
@Parcelize
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val targetValue: Int = 1,
    val unit: String = "times",
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "09:00 AM"
) : Parcelable {
    companion object {
        const val UNIT_TIMES = "times"
        const val UNIT_MINUTES = "minutes"
        const val UNIT_HOURS = "hours"
        const val UNIT_GLASSES = "glasses"
        const val UNIT_STEPS = "steps"
        const val UNIT_PAGES = "pages"
        const val UNIT_KILOMETERS = "km"
    }
}