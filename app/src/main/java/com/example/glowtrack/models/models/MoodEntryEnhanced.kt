package com.example.glowtrack.models.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced data class representing a mood journal entry without notes
 */
data class MoodEntryEnhanced(
    val id: String = UUID.randomUUID().toString(),
    val mood: MoodType,
    val emoji: String,
    val timestamp: Date = Date(),
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
)

/**
 * Enum representing different mood types with updated values
 */
enum class MoodTypeEnhanced(val value: Int, val label: String, val emoji: String) {
    VERY_HAPPY(5, "Very Happy", "😄"),
    HAPPY(4, "Happy", "😊"),
    CALM(4, "Calm", "😌"),
    EXCITED(5, "Excited", "🤩"),
    NEUTRAL(3, "Neutral", "😐"),
    TIRED(2, "Tired", "😴"),
    ANXIOUS(2, "Anxious", "😰"),
    SAD(2, "Sad", "😢"),
    ANGRY(2, "Angry", "😠"),
    VERY_SAD(1, "Very Sad", "😭");

    companion object {
        fun fromValue(value: Int): MoodTypeEnhanced {
            return values().find { it.value == value } ?: NEUTRAL
        }
        
        fun fromLabel(label: String): MoodTypeEnhanced {
            return values().find { it.label == label } ?: NEUTRAL
        }
        
        fun getAllMoods(): List<MoodTypeEnhanced> {
            return listOf(VERY_HAPPY, HAPPY, EXCITED, CALM, NEUTRAL, TIRED, ANXIOUS, SAD, ANGRY, VERY_SAD)
        }
    }
}