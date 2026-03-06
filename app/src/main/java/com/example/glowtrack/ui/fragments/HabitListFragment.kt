package com.example.glowtrack.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.models.HabitProgress
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.example.glowtrack.ui.adapters.HabitsAdapter
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date

class HabitListFragment : Fragment(), HabitsFragment.Companion.OnHabitDataChangeListener {
    companion object {
        private const val ARG_POSITION = "position"
        
        fun newInstance(position: Int): HabitListFragment {
            val fragment = HabitListFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var prefsManager: SharedPreferencesManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_habit_list, container, false)
        recyclerView = view.findViewById(R.id.recycler_habit_list)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        
        // Initialize adapter with proper listeners
        habitsAdapter = HabitsAdapter(
            onHabitClick = { habit, view -> 
                // Handle habit click - open edit dialog
                getHabitsFragment()?.editHabit(habit)
            },
            onProgressClick = { habit, progress -> 
                // Handle progress toggle
                toggleHabitProgress(habit, progress)
            },
            onDeleteClick = { habit -> 
                // Handle delete
                deleteHabit(habit)
            },
            onShareClick = { habit -> 
                // Handle share
                shareHabitProgress(habit)
            }
        )
        
        // Setup RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = habitsAdapter
            setHasFixedSize(true)
        }
        
        // Register as a listener for habit data changes
        getHabitsFragment()?.addDataChangeListener(this)
        
        // Load habits
        loadHabits()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister as a listener when the fragment is destroyed
        getHabitsFragment()?.removeDataChangeListener(this)
    }
    
    override fun onHabitDataChanged() {
        // Refresh the habits list when data changes
        loadHabits()
    }
    
    private fun getHabitsFragment(): HabitsFragment? {
        // Try to get the target fragment first (set by ViewPager adapter)
        val target = targetFragment
        if (target is HabitsFragment) {
            return target
        }
        
        // Fallback to traversing the parent fragment hierarchy
        return HabitsFragment.findInstance(this)
    }
    
    private fun loadHabits() {
        val position = arguments?.getInt(ARG_POSITION) ?: 0
        val habits = prefsManager.getHabits()
        val today = dateFormat.format(Date())
        
        // Filter habits based on tab position
        val filteredHabits = when (position) {
            0 -> habits // All habits
            1 -> habits.filter { habit ->
                val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                progress != null && !progress.isCompleted
            } // In Progress
            2 -> habits.filter { habit ->
                val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                progress?.isCompleted ?: false
            } // Completed
            else -> habits
        }
        
        // Get progress for each habit
        val habitsWithProgress = filteredHabits.map { habit ->
            val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                ?: HabitProgress(habit.id, today)
            Pair(habit, progress)
        }
        
        habitsAdapter.updateHabits(habitsWithProgress)
    }
    
    // Methods to handle habit actions directly
    private fun toggleHabitProgress(habit: Habit, currentProgress: HabitProgress) {
        val today = dateFormat.format(Date())
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
        // Notify the parent fragment to refresh all components
        getHabitsFragment()?.notifyDataChange()
    }
    
    private fun deleteHabit(habit: Habit) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Habit")
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                prefsManager.deleteHabit(habit.id)
                // Notify the parent fragment to refresh all components
                getHabitsFragment()?.notifyDataChange()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun shareHabitProgress(habit: Habit) {
        val today = dateFormat.format(Date())
        val progress = prefsManager.getHabitProgressForDay(habit.id, today)
        val progressText = if (progress?.isCompleted == true) {
            "Completed: ${habit.name}"
        } else {
            "Working on: ${habit.name}"
        }
        
        val shareText = "Check out my habit progress: $progressText"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    
    // Refresh the habits list
    fun refreshHabits() {
        if (::habitsAdapter.isInitialized) {
            loadHabits()
        }
    }
    
    private fun showHabitDetail(habit: Habit, view: View) {
        // For now, we'll just show a toast
        // In a real implementation, you would navigate to the habit detail fragment
        android.widget.Toast.makeText(requireContext(), "Showing details for ${habit.name}", android.widget.Toast.LENGTH_SHORT).show()
    }
}