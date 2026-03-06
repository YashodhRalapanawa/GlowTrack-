package com.example.glowtrack.ui.charts

import android.content.Context
import com.example.glowtrack.R
import com.example.glowtrack.models.models.MoodEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for creating mood trend charts
 */
class MoodChartHelper(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    fun setupMoodTrendChart(chart: LineChart, moodEntries: List<MoodEntry>, period: Int = 7) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(android.graphics.Color.WHITE)
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = if (period <= 7) 7 else 6
                textColor = context.getColor(R.color.text_secondary)
            }
            
            // Configure Y-axis to have 0 in the middle
            axisLeft.apply {
                axisMinimum = -3f  // Changed to allow negative values
                axisMaximum = 3f   // Changed to allow positive values
                setDrawGridLines(true)
                textColor = context.getColor(R.color.text_secondary)
            }
            
            axisRight.isEnabled = false
            
            // Configure legend
            legend.apply {
                isEnabled = true
                textColor = context.getColor(R.color.text_primary)
            }
        }
        
        // Process mood data for the specified period
        val chartData = prepareMoodData(moodEntries, period)
        
        if (chartData.isNotEmpty()) {
            val dataSet = LineDataSet(chartData, "Daily Mood").apply {
                color = context.getColor(R.color.primary_green)
                setCircleColor(context.getColor(R.color.primary_green))
                lineWidth = 3f
                circleRadius = 6f
                setDrawCircleHole(false)
                valueTextSize = 12f
                valueTextColor = context.getColor(R.color.text_primary)
                setDrawFilled(true)
                fillColor = context.getColor(R.color.primary_green_light)
                fillAlpha = 50
            }
            
            val lineData = LineData(dataSet)
            chart.data = lineData
            
            // Set up X-axis labels
            val labels = getPeriodLabels(period)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            
            chart.invalidate() // Refresh chart
        } else {
            chart.clear()
            chart.invalidate()
        }
    }
    
    private fun prepareMoodData(moodEntries: List<MoodEntry>, period: Int): List<Entry> {
        val calendar = Calendar.getInstance()
        val entries = mutableListOf<Entry>()
        
        // Get mood data for the specified period
        for (i in (period - 1) downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = dateFormat.format(calendar.time)
            
            // Find moods for this day
            val dayMoods = moodEntries.filter { it.date == dateString }
            
            if (dayMoods.isNotEmpty()) {
                // Calculate average mood for the day and center around 0
                // Convert 1-5 scale to -2 to +2 scale (0 in middle)
                val averageMood = dayMoods.map { it.mood.value }.average().toFloat()
                val centeredMood = averageMood - 3f  // Shift from 1-5 to -2 to +2
                entries.add(Entry(((period - 1) - i).toFloat(), centeredMood))
            } else {
                // No mood entry for this day
                entries.add(Entry(((period - 1) - i).toFloat(), 0f)) // 0 for no data
            }
        }
        
        return entries
    }
    
    private fun getPeriodLabels(period: Int): List<String> {
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()
        
        if (period <= 7) {
            // Weekly view - show day names
            for (i in (period - 1) downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                labels.add(dayFormat.format(calendar.time))
            }
        } else {
            // Monthly view - show dates
            for (i in (period - 1) downTo 0 step (period / 6)) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                labels.add(monthFormat.format(calendar.time))
            }
            // Ensure we have exactly 6 labels
            while (labels.size < 6) {
                labels.add("")
            }
        }
        
        return labels
    }
    
    fun setupMoodDistributionChart(chart: com.github.mikephil.charting.charts.BarChart, moodEntries: List<MoodEntry>) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            setBackgroundColor(android.graphics.Color.WHITE)
            
            // Configure X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = context.getColor(R.color.text_secondary)
            }
            
            // Configure Y-axis
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = context.getColor(R.color.text_secondary)
            }
            
            axisRight.isEnabled = false
            
            // Configure legend
            legend.apply {
                isEnabled = false
            }
        }
        
        // Process mood data for distribution
        val chartData = prepareMoodDistributionData(moodEntries)
        
        if (chartData.isNotEmpty()) {
            val dataSet = com.github.mikephil.charting.data.BarDataSet(chartData, "Mood Distribution").apply {
                color = context.getColor(R.color.primary_green)
                valueTextSize = 12f
                valueTextColor = context.getColor(R.color.text_primary)
            }
            
            val barData = com.github.mikephil.charting.data.BarData(dataSet)
            chart.data = barData
            
            // Set up X-axis labels
            val labels = getMoodLabels()
            chart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
            
            chart.invalidate() // Refresh chart
        } else {
            chart.clear()
            chart.invalidate()
        }
    }
    
    private fun prepareMoodDistributionData(moodEntries: List<MoodEntry>): List<com.github.mikephil.charting.data.BarEntry> {
        val moodCounts = mutableMapOf<String, Int>()
        
        // Count occurrences of each mood
        moodEntries.forEach { entry ->
            val moodLabel = entry.mood.label
            moodCounts[moodLabel] = moodCounts.getOrDefault(moodLabel, 0) + 1
        }
        
        val entries = mutableListOf<com.github.mikephil.charting.data.BarEntry>()
        val moods = listOf("Very Sad", "Sad", "Neutral", "Happy", "Very Happy")
        
        // Create entries for each mood type
        moods.forEachIndexed { index, mood ->
            val count = moodCounts[mood] ?: 0
            entries.add(com.github.mikephil.charting.data.BarEntry(index.toFloat(), count.toFloat()))
        }
        
        return entries
    }
    
    private fun getMoodLabels(): List<String> {
        return listOf("Very Sad", "Sad", "Neutral", "Happy", "Very Happy")
    }
    
    /**
     * Setup mood distribution as a pie chart
     */
    fun setupMoodDistributionPieChart(chart: com.github.mikephil.charting.charts.PieChart, moodEntries: List<MoodEntry>) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            holeRadius = 60f
            transparentCircleRadius = 65f
            setHoleColor(android.graphics.Color.WHITE)
            setCenterTextSize(14f)
            setCenterTextColor(context.getColor(R.color.text_primary))
            setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setDrawCenterText(true)
            setCenterText("Mood\nDistribution")
            legend.apply {
                isEnabled = true
                textColor = context.getColor(R.color.text_secondary)
                textSize = 12f
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
        
        // Process mood data for distribution
        val chartData = prepareMoodDistributionPieData(moodEntries)
        
        if (chartData.isNotEmpty()) {
            val dataSet = com.github.mikephil.charting.data.PieDataSet(chartData, "").apply {
                colors = listOf(
                    context.getColor(R.color.mood_very_sad),
                    context.getColor(R.color.mood_sad),
                    context.getColor(R.color.mood_neutral),
                    context.getColor(R.color.mood_happy),
                    context.getColor(R.color.mood_very_happy)
                )
                valueTextSize = 12f
                valueTextColor = context.getColor(R.color.text_primary)
                sliceSpace = 2f
                selectionShift = 8f
            }
            
            val pieData = com.github.mikephil.charting.data.PieData(dataSet).apply {
                setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter(chart))
            }
            
            chart.data = pieData
            chart.invalidate() // Refresh chart
        } else {
            chart.clear()
            chart.invalidate()
        }
    }
    
    private fun prepareMoodDistributionPieData(moodEntries: List<MoodEntry>): List<com.github.mikephil.charting.data.PieEntry> {
        val moodCounts = mutableMapOf<String, Int>()
        var totalEntries = 0
        
        // Count occurrences of each mood
        moodEntries.forEach { entry ->
            val moodLabel = entry.mood.label
            moodCounts[moodLabel] = moodCounts.getOrDefault(moodLabel, 0) + 1
            totalEntries++
        }
        
        val entries = mutableListOf<com.github.mikephil.charting.data.PieEntry>()
        val moods = listOf("Very Sad", "Sad", "Neutral", "Happy", "Very Happy")
        val moodEmojis = listOf("😢", "😞", "😐", "😊", "😄")
        
        // Create entries for each mood type
        moods.forEachIndexed { index, mood ->
            val count = moodCounts[mood] ?: 0
            if (count > 0 && totalEntries > 0) {
                val percentage = (count.toFloat() / totalEntries.toFloat()) * 100
                entries.add(com.github.mikephil.charting.data.PieEntry(percentage, "${moodEmojis[index]} $mood"))
            }
        }
        
        return entries
    }
}