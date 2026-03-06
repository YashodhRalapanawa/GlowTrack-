package com.example.glowtrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.example.glowtrack.receivers.HabitAlarmScheduler
import com.example.glowtrack.receivers.HydrationAlarmScheduler
import com.example.glowtrack.ui.auth.LoginActivity
import com.example.glowtrack.ui.fragments.HabitsFragment
import com.example.glowtrack.ui.fragments.HydrationFragment
import com.example.glowtrack.ui.fragments.MoodFragmentEnhanced
import com.example.glowtrack.ui.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var prefsManager: SharedPreferencesManager
    
    // Permission request code
    private val PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        prefsManager = SharedPreferencesManager.getInstance(this)
        if (!prefsManager.isUserLoggedIn()) {
            // Redirect to login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_main)
        
        // Initialize bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation)
        setupBottomNavigation()
        
        // Set up window insets for proper layout handling
        setupWindowInsets()
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HabitsFragment())
        }
        
        // Check and request notification permissions on app start
        checkAndRequestNotificationPermission()
        
        // Check and request exact alarm permissions on app start
        checkAndRequestExactAlarmPermission()
        
        // Schedule habit reminders on app start
        HabitAlarmScheduler.scheduleAllHabitReminders(this)
        
        // Handle intent extras
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_habits -> HabitsFragment()
                R.id.nav_mood -> MoodFragmentEnhanced()
                R.id.nav_hydration -> HydrationFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HabitsFragment()
            }
            loadFragment(fragment)
            true
        }
        
        // Set the default selected item
        bottomNavigation.selectedItemId = R.id.nav_habits
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
    
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Show explanation dialog before requesting permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Notification Permission Required")
                    .setMessage("WellnesMate needs notification permission to send you hydration reminders. Please grant this permission to receive timely reminders.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun checkAndRequestExactAlarmPermission() {
        // Only check on Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!HydrationAlarmScheduler.canScheduleExactAlarms(this)) {
                // Show explanation dialog before requesting permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Exact Alarm Permission Required")
                    .setMessage("WellnesMate needs permission to schedule exact alarms for precise hydration reminders. Please grant this permission to receive timely reminders.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        HydrationAlarmScheduler.requestExactAlarmPermission(this)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(
                        this,
                        "Notification permission granted. You'll now receive hydration reminders!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Show explanation why permission is needed
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("Without notification permission, you won't receive hydration reminders. You can enable this permission later in Settings > Apps > WellnesMate > Permissions.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            )
                            val uri = android.net.Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }
    
    // Method to update the toolbar title from fragments (no longer needed)
    /*
    fun updateToolbarTitle(title: String) {
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.title = title
    }
    */
    
    private fun handleIntent(intent: Intent) {
        when {
            intent.getBooleanExtra("open_hydration", false) -> {
                bottomNavigation.selectedItemId = R.id.nav_hydration
                loadFragment(HydrationFragment())
                
                // Handle quick add water from notification
                if (intent.getBooleanExtra("quick_add_water", false)) {
                    // Add default amount of water (250ml)
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val intake = com.example.glowtrack.models.models.HydrationIntake(
                        date = today,
                        amountMl = 250,
                        timestamp = java.util.Date()
                    )
                    prefsManager.addHydrationIntake(intake)
                    
                    android.widget.Toast.makeText(
                        this,
                        "Added 250ml of water",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                // Default to habits fragment
                loadFragment(HabitsFragment())
            }
        }
    }
}