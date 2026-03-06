package com.example.glowtrack.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glowtrack.R
import com.example.glowtrack.models.models.MoodEntry
import com.example.glowtrack.models.models.MoodType
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.example.glowtrack.ui.adapters.MoodSelectorAdapterEnhanced
import com.example.glowtrack.ui.adapters.MoodHistoryAdapter
import com.example.glowtrack.ui.charts.MoodChartHelper
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced fragment for mood journaling with improved UI and visualization
 */
class MoodFragmentEnhanced : Fragment() {
    
    private lateinit var recyclerMoodSelector: RecyclerView
    private lateinit var recyclerMoodHistory: RecyclerView
    private lateinit var btnSaveMood: MaterialButton
    private lateinit var chartMoodDistribution: PieChart
    private lateinit var tvTotalEntries: TextView
    private lateinit var tvAvgMood: TextView
    private lateinit var tvMoodStreak: TextView
    private lateinit var tvMostFrequentMood: TextView
    private lateinit var tvMostFrequentMoodLabel: TextView
    private lateinit var tvMoodConsistency: TextView
    private lateinit var tvBestDay: TextView
    private lateinit var tvHistoryCount: TextView
    private lateinit var layoutEmptyMoodHistory: View
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var moodSelectorAdapter: MoodSelectorAdapterEnhanced
    private lateinit var moodHistoryAdapter: MoodHistoryAdapter
    private lateinit var moodChartHelper: MoodChartHelper
    
    private var selectedMood: MoodType? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mood_enhanced, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize components
        initializeViews(view)
        setupMoodSelector()
        setupMoodHistory()
        setupClickListeners()
        loadMoodData()
    }
    
    override fun onResume() {
        super.onResume()
        loadMoodData()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        moodChartHelper = MoodChartHelper(requireContext())
        
        recyclerMoodSelector = view.findViewById(R.id.recycler_mood_selector)
        recyclerMoodHistory = view.findViewById(R.id.recycler_mood_history)
        btnSaveMood = view.findViewById(R.id.btn_save_mood)
        chartMoodDistribution = view.findViewById(R.id.chart_mood_distribution)
        tvTotalEntries = view.findViewById(R.id.tv_total_entries)
        tvAvgMood = view.findViewById(R.id.tv_avg_mood)
        tvMoodStreak = view.findViewById(R.id.tv_mood_streak)
        tvMostFrequentMood = view.findViewById(R.id.tv_most_frequent_mood)
        tvMostFrequentMoodLabel = view.findViewById(R.id.tv_most_frequent_mood_label)
        tvMoodConsistency = view.findViewById(R.id.tv_mood_consistency)
        tvBestDay = view.findViewById(R.id.tv_best_day)
        tvHistoryCount = view.findViewById(R.id.tv_history_count)
        layoutEmptyMoodHistory = view.findViewById(R.id.layout_empty_mood_history)
    }
    
    private fun setupMoodSelector() {
        moodSelectorAdapter = MoodSelectorAdapterEnhanced { mood ->
            selectedMood = mood
            updateSaveButtonState()
        }
        
        recyclerMoodSelector.apply {
            layoutManager = GridLayoutManager(context, 3) // 3 columns for better emoji visibility
            adapter = moodSelectorAdapter
            setHasFixedSize(true)
        }
        
        // Load all available moods
        moodSelectorAdapter.updateMoods(MoodType.getAllMoods())
    }
    
    private fun setupMoodHistory() {
        moodHistoryAdapter = MoodHistoryAdapter(
            onDeleteClick = { entry -> deleteMoodEntry(entry) },
            onShareClick = { entry -> shareMoodEntry(entry) }
        )
        
        recyclerMoodHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = moodHistoryAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupClickListeners() {
        btnSaveMood.setOnClickListener {
            saveMoodEntry()
        }
    }
    
    private fun updateSaveButtonState() {
        btnSaveMood.isEnabled = selectedMood != null
    }
    
    private fun saveMoodEntry() {
        val mood = selectedMood ?: return
        
        val moodEntry = MoodEntry(
            mood = mood,
            emoji = mood.emoji,
            notes = "", // Removed notes field
            timestamp = Date()
        )
        
        prefsManager.saveMoodEntry(moodEntry)
        loadMoodData()
        
        // Show confirmation
        android.widget.Toast.makeText(
            requireContext(),
            "Mood entry saved successfully!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // Reset selection
        selectedMood = null
        updateSaveButtonState()
        moodSelectorAdapter.clearSelection()
    }
    
    private fun loadMoodData() {
        val moodEntries = prefsManager.getMoodEntries()
        
        // Update mood history list
        moodHistoryAdapter.updateMoodEntries(moodEntries)
        
        // Update mood distribution chart (pie chart only)
        moodChartHelper.setupMoodDistributionPieChart(chartMoodDistribution, moodEntries)
        
        // Update mood summary
        updateMoodSummary(moodEntries)
        
        // Update mood insights
        updateMoodInsights(moodEntries)
        
        // Update empty state visibility
        val isEmpty = moodEntries.isEmpty()
        recyclerMoodHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        layoutEmptyMoodHistory.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tvHistoryCount.text = "${moodEntries.size} ${if (moodEntries.size == 1) "entry" else "entries"}"
    }
    
    private fun updateMoodSummary(moodEntries: List<MoodEntry>) {
        // Update total entries
        tvTotalEntries.text = moodEntries.size.toString()
        
        // Update average mood
        if (moodEntries.isNotEmpty()) {
            val avgMood = moodEntries.map { it.mood.value }.average()
            tvAvgMood.text = String.format("%.1f", avgMood)
        } else {
            tvAvgMood.text = "0.0"
        }
        
        // Update mood streak
        val streak = calculateMoodStreak()
        tvMoodStreak.text = "$streak days"
    }
    
    private fun updateMoodInsights(moodEntries: List<MoodEntry>) {
        if (moodEntries.isEmpty()) {
            // Reset all insights to default values
            tvMostFrequentMood.text = "😐"
            tvMostFrequentMoodLabel.text = "Neutral"
            tvMoodConsistency.text = "0%"
            tvBestDay.text = "N/A"
            return
        }
        
        // Calculate most frequent mood
        val moodCounts = mutableMapOf<MoodType, Int>()
        moodEntries.forEach { entry ->
            moodCounts[entry.mood] = moodCounts.getOrDefault(entry.mood, 0) + 1
        }
        
        val mostFrequentMood = moodCounts.maxByOrNull { it.value }?.key ?: MoodType.NEUTRAL
        tvMostFrequentMood.text = mostFrequentMood.emoji
        tvMostFrequentMoodLabel.text = mostFrequentMood.label
        
        // Calculate consistency (percentage of days with mood entries in the last 30 days)
        val totalDays = 30
        val daysWithEntries = moodEntries
            .map { it.date }
            .distinct()
            .size
        
        val consistency = if (totalDays > 0) (daysWithEntries * 100) / totalDays else 0
        tvMoodConsistency.text = "$consistency%"
        
        // Find the best day (day with highest average mood)
        val entriesByDate = moodEntries.groupBy { it.date }
        var bestDayAvg = 0.0
        var bestDay = ""
        
        entriesByDate.forEach { (date, entries) ->
            val avg = entries.map { it.mood.value }.average()
            if (avg > bestDayAvg) {
                bestDayAvg = avg
                bestDay = date
            }
        }
        
        if (bestDay.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                val date = dateFormat.parse(bestDay)
                tvBestDay.text = displayFormat.format(date ?: Date())
            } catch (e: Exception) {
                tvBestDay.text = "N/A"
            }
        } else {
            tvBestDay.text = "N/A"
        }
    }
    
    private fun calculateMoodStreak(): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Check consecutive days with mood entries
        for (i in 0..6) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = dateFormat.format(calendar.time)
            
            val dayEntries = prefsManager.getMoodEntriesForDate(dateString)
            if (dayEntries.isNotEmpty()) {
                streak++
            } else if (i > 0) { // Break streak if not today and no entries
                break
            }
        }
        
        return streak
    }
    
    private fun deleteMoodEntry(entry: MoodEntry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                prefsManager.deleteMoodEntry(entry.id)
                loadMoodData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun shareMoodEntry(entry: MoodEntry) {
        val shareText = "My mood: ${entry.mood.label} ${entry.emoji}"
        val formattedText = getString(R.string.share_mood_summary, shareText)
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, formattedText)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
}