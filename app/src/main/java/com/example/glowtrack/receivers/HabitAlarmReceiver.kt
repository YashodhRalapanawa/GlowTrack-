package com.example.glowtrack.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.glowtrack.MainActivity
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.repository.SharedPreferencesManager

/**
 * BroadcastReceiver for handling habit reminder notifications
 */
class HabitAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "habit_reminders"
        private const val NOTIFICATION_ID_BASE = 2000
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val habitName = intent.getStringExtra("habit_name") ?: return
        
        // Create notification channel
        createNotificationChannel(context)
        
        // Send habit reminder notification
        sendHabitReminder(context, habitId, habitName)
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Habit Reminders"
            val descriptionText = "Notifications for habit reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun sendHabitReminder(context: Context, habitId: String, habitName: String) {
        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_habits", true)
            putExtra("habit_id", habitId)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get default notification sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_habits)
            .setContentTitle("Habit Reminder")
            .setContentText("Time to complete: $habitName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't forget to work on your habit: $habitName")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .build()
        
        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(habitId.hashCode(), notification)
    }
}