package com.example.glowtrack.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glowtrack.MainActivity
import com.example.glowtrack.R
import com.example.glowtrack.models.models.HydrationIntake
import com.example.glowtrack.models.models.HydrationSettings
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.example.glowtrack.receivers.HydrationAlarmScheduler
import com.example.glowtrack.ui.adapters.HydrationHistoryAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for hydration tracking and reminders
 */
class HydrationFragment : Fragment() {
    
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvCurrentIntake: TextView
    private lateinit var tvCurrentIntakeCircle: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var tvProgressPercentageCircle: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var progressBarHydration: LinearProgressIndicator
    private lateinit var circularProgressHydration: CircularProgressIndicator
    private lateinit var btnAddWater: MaterialButton
    private lateinit var btnSetReminder: MaterialButton
    private lateinit var recyclerHydrationHistory: RecyclerView
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var hydrationHistoryAdapter: HydrationHistoryAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hydration, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize components
        initializeViews(view)
        setupHydrationHistory()
        setupClickListeners()
        updateHydrationDisplay()
    }
    
    override fun onResume() {
        super.onResume()
        updateHydrationDisplay()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal)
        tvCurrentIntake = view.findViewById(R.id.tv_current_intake)
        tvCurrentIntakeCircle = view.findViewById(R.id.tv_current_intake_circle)
        tvProgressText = view.findViewById(R.id.tv_progress_text)
        tvProgressPercentageCircle = view.findViewById(R.id.tv_progress_percentage_circle)
        tvStreakCount = view.findViewById(R.id.tv_streak_count)
        progressBarHydration = view.findViewById(R.id.progress_bar_hydration)
        circularProgressHydration = view.findViewById(R.id.circular_progress_hydration)
        btnAddWater = view.findViewById(R.id.btn_add_water)
        btnSetReminder = view.findViewById(R.id.btn_set_reminder)
        recyclerHydrationHistory = view.findViewById(R.id.recycler_hydration_history)
    }
    
    private fun setupHydrationHistory() {
        hydrationHistoryAdapter = HydrationHistoryAdapter(
            onDeleteClick = { intake ->
                // Show confirmation dialog before deleting
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Intake")
                    .setMessage("Are you sure you want to delete this water intake entry?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteHydrationIntake(intake)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        
        recyclerHydrationHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hydrationHistoryAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun deleteHydrationIntake(intake: HydrationIntake) {
        prefsManager.deleteHydrationIntake(intake)
        updateHydrationDisplay()
        
        // Show confirmation
        android.widget.Toast.makeText(
            requireContext(),
            "Intake entry deleted",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun setupClickListeners() {
        btnAddWater.setOnClickListener {
            showAddWaterDialog()
        }
        
        btnSetReminder.setOnClickListener {
            showReminderSettingsDialog()
        }
    }
    
    private fun updateHydrationDisplay() {
        val settings = prefsManager.getHydrationSettings()
        val todayIntake = prefsManager.getTodayTotalHydration()
        val todayIntakeList = prefsManager.getTodayHydrationIntake()
        
        // Update goal and current intake
        tvDailyGoal.text = "${settings.dailyGoalMl} ${getString(R.string.ml_unit)}"
        tvCurrentIntake.text = "$todayIntake ${getString(R.string.ml_unit)}"
        tvCurrentIntakeCircle.text = todayIntake.toString()
        
        // Update progress
        val progressPercentage = if (settings.dailyGoalMl > 0) {
            ((todayIntake.toFloat() / settings.dailyGoalMl.toFloat()) * 100).toInt().coerceAtMost(100)
        } else {
            0
        }
        
        progressBarHydration.progress = progressPercentage
        circularProgressHydration.progress = progressPercentage
        tvProgressText.text = "$progressPercentage% of daily goal"
        tvProgressPercentageCircle.text = "$progressPercentage% of goal"
        
        // Update streak count (simple implementation - in a real app, this would be more sophisticated)
        val streakCount = calculateHydrationStreak()
        tvStreakCount.text = "$streakCount days"
        
        // Update history
        val allIntakes = prefsManager.getHydrationIntake().take(10) // Show last 10 entries
        hydrationHistoryAdapter.updateIntakes(allIntakes)
        
        // Check if goal is reached
        if (progressPercentage >= 100) {
            // Show congratulations
            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.goal_reached),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun calculateHydrationStreak(): Int {
        // Simple streak calculation - in a real app, this would be more sophisticated
        var streak = 0
        val calendar = Calendar.getInstance()
        
        // Check for the last 7 days
        for (i in 0..6) {
            val date = dateFormat.format(calendar.time)
            val dailyIntake = prefsManager.getHydrationIntakeForDate(date)
            val settings = prefsManager.getHydrationSettings()
            
            // If daily intake meets or exceeds goal
            if (dailyIntake >= settings.dailyGoalMl) {
                streak++
            } else if (i > 0) { // Break streak if not today and goal not met
                break
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return streak
    }
    
    private fun showAddWaterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_water, null)
        
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_water_amount)
        val sliderCustomAmount = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.slider_custom_amount)
        val chipGroupRecentIntake = dialogView.findViewById<ChipGroup>(R.id.chip_group_recent_intake)
        
        // Setup preset buttons
        val cardPreset200 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_200)
        val cardPreset250 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_250)
        val cardPreset300 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_300)
        val cardPreset330 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_330)
        val cardPreset500 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_500)
        val cardPreset750 = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preset_750)
        
        // Set up click listeners for preset cards
        cardPreset200.setOnClickListener { etAmount.setText("200") }
        cardPreset250.setOnClickListener { etAmount.setText("250") }
        cardPreset300.setOnClickListener { etAmount.setText("300") }
        cardPreset330.setOnClickListener { etAmount.setText("330") }
        cardPreset500.setOnClickListener { etAmount.setText("500") }
        cardPreset750.setOnClickListener { etAmount.setText("750") }
        
        // Set up slider listener
        sliderCustomAmount.addOnChangeListener { _, value, _ ->
            etAmount.setText(value.toInt().toString())
        }
        
        // Populate recent intake chips
        populateRecentIntakeChips(chipGroupRecentIntake, etAmount)
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_water))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val amountText = etAmount.text?.toString()
                if (!amountText.isNullOrBlank()) {
                    try {
                        val amount = amountText.toInt()
                        if (amount > 0) {
                            addWaterIntake(amount)
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Please enter a valid amount",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please enter an amount",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun populateRecentIntakeChips(chipGroup: ChipGroup, amountEditText: TextInputEditText) {
        // Get recent intake amounts (last 5 unique amounts)
        val recentIntakes = prefsManager.getHydrationIntake()
            .takeLast(20) // Last 20 entries
            .map { it.amountMl }
            .distinct()
            .take(5)
        
        // Clear existing chips
        chipGroup.removeAllViews()
        
        // Add chips for recent intakes
        for (amount in recentIntakes) {
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = "${amount}ml"
            chip.isClickable = true
            chip.isCheckable = false
            
            // Set chip style
            chip.setChipBackgroundColorResource(R.color.surface_variant)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            chip.chipStrokeWidth = 1f
            chip.chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.divider_color)
            
            // Set click listener
            chip.setOnClickListener {
                amountEditText.setText(amount.toString())
            }
            
            chipGroup.addView(chip)
        }
    }
    
    private fun addWaterIntake(amountMl: Int) {
        val today = dateFormat.format(Date())
        val intake = HydrationIntake(
            date = today,
            amountMl = amountMl,
            timestamp = Date()
        )
        
        prefsManager.addHydrationIntake(intake)
        updateHydrationDisplay()
        
        // Check if goal is reached after adding this intake
        checkAndNotifyGoalReached()
        
        android.widget.Toast.makeText(
            requireContext(),
            "Added ${amountMl}ml of water",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun checkAndNotifyGoalReached() {
        val settings = prefsManager.getHydrationSettings()
        val todayIntake = prefsManager.getTodayTotalHydration()
        
        // Check if user just reached their goal
        if (todayIntake >= settings.dailyGoalMl) {
            sendGoalReachedNotification()
        }
    }
    
    private fun sendGoalReachedNotification() {
        // Create intent to open the app when notification is tapped
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration", true)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            requireContext(),
            2, // Different request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get default alarm sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
        // Create notification channel first
        createNotificationChannel()
        
        // Build the goal reached notification
        val notification = NotificationCompat.Builder(requireContext(), "hydration_reminders")
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Goal Reached! 🎉")
            .setContentText("Congratulations! You've reached your daily hydration goal.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great job staying hydrated! You've reached your daily water intake goal.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri) // Add alarm sound for goal reached
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibrate pattern
            .build()
        
        // Show the notification
        try {
            NotificationManagerCompat.from(requireContext()).notify(1002, notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
            android.widget.Toast.makeText(
                requireContext(),
                "Please enable notification permission to receive goal alerts",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_hydration_name)
            val descriptionText = getString(R.string.channel_hydration_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("hydration_reminders", name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showReminderSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder_settings, null)
        
        val etGoal = dialogView.findViewById<TextInputEditText>(R.id.et_daily_goal)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_start_time)
        val tvEndTime = dialogView.findViewById<TextView>(R.id.tv_end_time)
        val cardNotificationPermission = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_notification_permission)
        val btnGrantPermission = dialogView.findViewById<MaterialButton>(R.id.btn_grant_permission)
        val cardCustomFrequency = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_custom_frequency)
        val sliderCustomFrequency = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.slider_custom_frequency)
        val tvCustomFrequencyValue = dialogView.findViewById<TextView>(R.id.tv_custom_frequency_value)
        val switchVibration = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_vibration)
        val switchSnooze = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_snooze)
        
        // Frequency preset chips
        val chipFreq30 = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_30)
        val chipFreq1 = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_1)
        val chipFreq2 = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_2)
        val chipFreq3 = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_3)
        val chipFreq4 = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_4)
        val chipFreqCustom = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_freq_custom)
        
        val currentSettings = prefsManager.getHydrationSettings()
        
        // Set current values
        etGoal.setText(currentSettings.dailyGoalMl.toString())
        
        // Format reminder time (now with minutes)
        val reminderTimeText = String.format("%02d:%02d", currentSettings.startTime, currentSettings.startMinute)
        tvReminderTime.text = reminderTimeText
        
        // Format end time
        val endTimeText = String.format("%02d:%02d", currentSettings.endTime, 0)
        tvEndTime.text = endTimeText
        
        // Set switch states
        switchVibration.isChecked = currentSettings.vibrationEnabled
        switchSnooze.isChecked = currentSettings.snoozeEnabled
        
        // Variable to track selected frequency
        var selectedFrequency = currentSettings.reminderIntervalMinutes
        
        // Variable to track selected time (hour and minute)
        var selectedHour = currentSettings.startTime
        var selectedMinute = currentSettings.startMinute
        var selectedEndHour = currentSettings.endTime
        
        // Set up frequency chip listeners
        chipFreq30.setOnClickListener { 
            selectedFrequency = 30
            cardCustomFrequency.visibility = View.GONE
            // Update chip states
            chipFreq30.isChecked = true
            chipFreq1.isChecked = false
            chipFreq2.isChecked = false
            chipFreq3.isChecked = false
            chipFreq4.isChecked = false
            chipFreqCustom.isChecked = false
        }
        
        chipFreq1.setOnClickListener { 
            selectedFrequency = 60
            cardCustomFrequency.visibility = View.GONE
            // Update chip states
            chipFreq30.isChecked = false
            chipFreq1.isChecked = true
            chipFreq2.isChecked = false
            chipFreq3.isChecked = false
            chipFreq4.isChecked = false
            chipFreqCustom.isChecked = false
        }
        
        chipFreq2.setOnClickListener { 
            selectedFrequency = 120
            cardCustomFrequency.visibility = View.GONE
            // Update chip states
            chipFreq30.isChecked = false
            chipFreq1.isChecked = false
            chipFreq2.isChecked = true
            chipFreq3.isChecked = false
            chipFreq4.isChecked = false
            chipFreqCustom.isChecked = false
        }
        
        chipFreq3.setOnClickListener { 
            selectedFrequency = 180
            cardCustomFrequency.visibility = View.GONE
            // Update chip states
            chipFreq30.isChecked = false
            chipFreq1.isChecked = false
            chipFreq2.isChecked = false
            chipFreq3.isChecked = true
            chipFreq4.isChecked = false
            chipFreqCustom.isChecked = false
        }
        
        chipFreq4.setOnClickListener { 
            selectedFrequency = 240
            cardCustomFrequency.visibility = View.GONE
            // Update chip states
            chipFreq30.isChecked = false
            chipFreq1.isChecked = false
            chipFreq2.isChecked = false
            chipFreq3.isChecked = false
            chipFreq4.isChecked = true
            chipFreqCustom.isChecked = false
        }
        
        chipFreqCustom.setOnClickListener { 
            selectedFrequency = 60 // Default custom to 1 hour
            cardCustomFrequency.visibility = View.VISIBLE
            // Update chip states
            chipFreq30.isChecked = false
            chipFreq1.isChecked = false
            chipFreq2.isChecked = false
            chipFreq3.isChecked = false
            chipFreq4.isChecked = false
            chipFreqCustom.isChecked = true
        }
        
        // Set initial selected chip based on current settings
        when (currentSettings.reminderIntervalMinutes) {
            30 -> {
                selectedFrequency = 30
                chipFreq30.isChecked = true
            }
            60 -> {
                selectedFrequency = 60
                chipFreq1.isChecked = true
            }
            120 -> {
                selectedFrequency = 120
                chipFreq2.isChecked = true
            }
            180 -> {
                selectedFrequency = 180
                chipFreq3.isChecked = true
            }
            240 -> {
                selectedFrequency = 240
                chipFreq4.isChecked = true
            }
            else -> {
                selectedFrequency = 60
                chipFreqCustom.isChecked = true
                cardCustomFrequency.visibility = View.VISIBLE
            }
        }
        
        // Set up custom frequency slider
        sliderCustomFrequency.value = selectedFrequency.toFloat()
        tvCustomFrequencyValue.text = "Every $selectedFrequency minutes"
        
        sliderCustomFrequency.addOnChangeListener { _, value, _ ->
            selectedFrequency = value.toInt()
            tvCustomFrequencyValue.text = "Every $selectedFrequency minutes"
        }
        
        // Set up time pickers
        tvReminderTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    tvReminderTime.text = String.format("%02d:%02d", hour, minute)
                },
                selectedHour,
                selectedMinute,
                true
            )
            timePicker.show()
        }
        
        tvEndTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hour, _ ->
                    selectedEndHour = hour
                    tvEndTime.text = String.format("%02d:%02d", hour, 0)
                },
                selectedEndHour,
                0,
                true
            )
            timePicker.show()
        }
        
        // Check notification permission and show/hide permission card
        if (checkNotificationPermission()) {
            cardNotificationPermission.visibility = View.GONE
        } else {
            cardNotificationPermission.visibility = View.VISIBLE
            btnGrantPermission.setOnClickListener {
                requestNotificationPermission()
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_reminder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val goalText = etGoal.text?.toString()
                
                if (!goalText.isNullOrBlank()) {
                    try {
                        val newGoal = goalText.toInt()
                        
                        val newSettings = currentSettings.copy(
                            dailyGoalMl = newGoal,
                            reminderIntervalMinutes = selectedFrequency,
                            startTime = selectedHour,
                            startMinute = selectedMinute,
                            endTime = selectedEndHour,
                            vibrationEnabled = switchVibration.isChecked,
                            snoozeEnabled = switchSnooze.isChecked
                        )
                        
                        prefsManager.saveHydrationSettings(newSettings)
                        
                        // Check for exact alarm permission on Android 12+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (HydrationAlarmScheduler.canScheduleExactAlarms(requireContext())) {
                                // Use the enhanced alarm scheduler with minute precision
                                HydrationAlarmScheduler.scheduleRecurringAlarmWithMinutes(
                                    requireContext(), 
                                    newSettings, 
                                    selectedMinute
                                )
                            } else {
                                // Request exact alarm permission
                                HydrationAlarmScheduler.requestExactAlarmPermission(requireContext())
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Please grant exact alarm permission in settings",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setPositiveButton
                            }
                        } else {
                            // For older versions, directly schedule the alarm
                            HydrationAlarmScheduler.scheduleRecurringAlarmWithMinutes(
                                requireContext(), 
                                newSettings, 
                                selectedMinute
                            )
                        }
                        
                        updateHydrationDisplay()
                        
                        // Show next alarm time
                        val nextAlarmText = HydrationAlarmScheduler.getNextAlarmTimeFormatted(requireContext())
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Settings saved. $nextAlarmText",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        
                    } catch (e: NumberFormatException) {
                        // Invalid number
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid number for the goal",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions are automatically granted on older versions
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
    
    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Notification permission granted",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Notification permission denied",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun setupHydrationReminders(settings: HydrationSettings) {
        if (!settings.reminderEnabled) {
            HydrationAlarmScheduler.cancelAlarm(requireContext())
            return
        }
        
        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (HydrationAlarmScheduler.canScheduleExactAlarms(requireContext())) {
                // Use the new alarm scheduler
                HydrationAlarmScheduler.scheduleRecurringAlarm(requireContext(), settings)
            }
            // If we can't schedule exact alarms, the alarms will remain unscheduled
            // The user will need to manually enable them in the app
        } else {
            // For older versions, directly schedule the alarm
            HydrationAlarmScheduler.scheduleRecurringAlarm(requireContext(), settings)
        }
    }
}