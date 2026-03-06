package com.example.glowtrack.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class HabitDetailFragment : Fragment() {
    
    companion object {
        private const val ARG_HABIT = "habit"
        
        fun newInstance(habit: Habit): HabitDetailFragment {
            val fragment = HabitDetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_HABIT, habit)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var habit: Habit
    private lateinit var prefsManager: SharedPreferencesManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private lateinit var tvHabitName: TextView
    private lateinit var tvHabitDescription: TextView
    private lateinit var tvHabitTarget: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgressText: TextView
    private lateinit var tvStreak: TextView
    private lateinit var btnToggleCompletion: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habit = arguments?.getParcelable(ARG_HABIT) ?: throw IllegalArgumentException("Habit is required")
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        tvHabitName = view.findViewById(R.id.tv_habit_name)
        tvHabitDescription = view.findViewById(R.id.tv_habit_description)
        tvHabitTarget = view.findViewById(R.id.tv_habit_target)
        progressBar = view.findViewById(R.id.progress_bar_habit)
        tvProgressText = view.findViewById(R.id.tv_progress_text)
        tvStreak = view.findViewById(R.id.tv_streak)
        btnToggleCompletion = view.findViewById(R.id.btn_toggle_completion)
        
        // Load habit data
        loadHabitData()
        
        // Setup click listeners
        btnToggleCompletion.setOnClickListener {
            toggleHabitCompletion()
        }
    }
    
    private fun loadHabitData() {
        val today = dateFormat.format(Date())
        val progress = prefsManager.getHabitProgressForDay(habit.id, today)
            ?: com.example.glowtrack.models.models.HabitProgress(
                habitId = habit.id,
                date = today,
                isCompleted = false,
                value = 0,
                completedAt = null
            )
        
        // Update UI
        tvHabitName.text = habit.name
        tvHabitDescription.text = habit.description
        tvHabitTarget.text = getString(R.string.habit_target_format, habit.targetValue, habit.unit)
        
        // Progress info
        val progressPercentage = progress.getProgressPercentage(habit.targetValue)
        tvProgressText.text = getString(R.string.habit_progress_format, 
            progress.value, habit.targetValue, habit.unit)
        progressBar.setProgress(progressPercentage, true)
        
        // Streak info
        val streak = prefsManager.calculateHabitStreak(habit.id)
        tvStreak.text = getString(R.string.habit_streak, streak)
        
        // Completion button
        val (buttonText, buttonColor) = if (progress.isCompleted) {
            getString(R.string.mark_incomplete) to R.color.success
        } else {
            getString(R.string.mark_complete) to R.color.primary_green_light
        }
        btnToggleCompletion.text = buttonText
        btnToggleCompletion.setBackgroundColor(requireContext().getColor(buttonColor))
    }
    
    private fun toggleHabitCompletion() {
        val today = dateFormat.format(Date())
        val currentProgress = prefsManager.getHabitProgressForDay(habit.id, today)
            ?: com.example.glowtrack.models.models.HabitProgress(
                habitId = habit.id,
                date = today,
                isCompleted = false,
                value = 0,
                completedAt = null
            )
        
        val newProgress = if (currentProgress.isCompleted) {
            // Mark as incomplete
            currentProgress.copy(
                isCompleted = false,
                value = 0,
                completedAt = null
            )
        } else {
            // Mark as complete
            currentProgress.copy(
                isCompleted = true,
                value = habit.targetValue,
                completedAt = Date().time
            )
        }
        
        prefsManager.saveHabitProgressForDay(habit.id, today, newProgress)
        loadHabitData() // Refresh the UI
    }
}