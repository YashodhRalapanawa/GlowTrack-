package com.example.glowtrack.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import com.example.glowtrack.models.models.Habit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Scheduler for habit reminder notifications
 */
object HabitAlarmScheduler {
    
    /**
     * Schedule a reminder for a specific habit
     */
    fun scheduleHabitReminder(context: Context, habit: Habit) {
        // Only schedule if reminder is enabled
        if (!habit.reminderEnabled) {
            cancelHabitReminder(context, habit.id)
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
            putExtra("habit_id", habit.id)
            putExtra("habit_name", habit.name)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Parse the reminder time
        val reminderTime = parseReminderTime(habit.reminderTime)
        
        // Calculate next alarm time
        val nextAlarmTime = calculateNextAlarmTime(reminderTime)
        
        // Set the alarm
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            nextAlarmTime.timeInMillis,
            pendingIntent
        )
    }
    
    /**
     * Cancel a habit reminder
     */
    fun cancelHabitReminder(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HabitAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Schedule reminders for all habits with reminders enabled
     */
    fun scheduleAllHabitReminders(context: Context) {
        // Cancel all existing habit alarms first
        cancelAllHabitReminders(context)
        
        // Get all habits with reminders enabled
        val prefsManager = com.example.glowtrack.models.repository.SharedPreferencesManager.getInstance(context)
        val habits = prefsManager.getHabits()
        
        // Schedule reminders for each habit with reminders enabled
        habits.filter { it.reminderEnabled }.forEach { habit ->
            scheduleHabitReminder(context, habit)
        }
    }
    
    /**
     * Cancel all habit reminders
     */
    fun cancelAllHabitReminders(context: Context) {
        val prefsManager = com.example.glowtrack.models.repository.SharedPreferencesManager.getInstance(context)
        val habits = prefsManager.getHabits()
        
        habits.forEach { habit ->
            cancelHabitReminder(context, habit.id)
        }
    }
    
    /**
     * Parse reminder time string (e.g., "09:00 AM") into hour and minute
     */
    private fun parseReminderTime(timeString: String): Pair<Int, Int> {
        try {
            // Split the time string
            val parts = timeString.split(" ")
            if (parts.size != 2) return Pair(9, 0)
            
            val timeParts = parts[0].split(":")
            if (timeParts.size != 2) return Pair(9, 0)
            
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = parts[1]
            
            // Convert to 24-hour format
            if (amPm.equals("PM", ignoreCase = true) && hour != 12) {
                hour += 12
            } else if (amPm.equals("AM", ignoreCase = true) && hour == 12) {
                hour = 0
            }
            
            return Pair(hour, minute)
        } catch (e: Exception) {
            // Return default time (9:00 AM) if parsing fails
            return Pair(9, 0)
        }
    }
    
    /**
     * Calculate the next alarm time based on the reminder time
     */
    private fun calculateNextAlarmTime(reminderTime: Pair<Int, Int>): Calendar {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance()
        
        // Set the alarm time to today at the specified hour and minute
        alarmTime.set(Calendar.HOUR_OF_DAY, reminderTime.first)
        alarmTime.set(Calendar.MINUTE, reminderTime.second)
        alarmTime.set(Calendar.SECOND, 0)
        alarmTime.set(Calendar.MILLISECOND, 0)
        
        // If the alarm time has already passed today, set it for tomorrow
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return alarmTime
    }
}