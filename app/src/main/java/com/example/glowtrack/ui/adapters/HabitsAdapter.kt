package com.example.glowtrack.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.models.HabitProgress
import com.google.android.material.button.MaterialButton

/**
 * Adapter for displaying habits in RecyclerView
 */
class HabitsAdapter(
    private val onHabitClick: (Habit, View) -> Unit,
    private val onProgressClick: (Habit, HabitProgress) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onShareClick: (Habit) -> Unit
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    private var habitsWithProgress: List<Pair<Habit, HabitProgress>> = emptyList()
    private lateinit var prefsManager: com.example.glowtrack.models.repository.SharedPreferencesManager

    fun updateHabits(newHabitsWithProgress: List<Pair<Habit, HabitProgress>>) {
        habitsWithProgress = newHabitsWithProgress
        // Initialize SharedPreferences manager if not already done
        if (!::prefsManager.isInitialized && habitsWithProgress.isNotEmpty()) {
            // Get context from the first view holder when it's created
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val (habit, progress) = habitsWithProgress[position]
        holder.bind(habit, progress)
    }

    override fun getItemCount(): Int = habitsWithProgress.size
    
    fun getHabitAtPosition(position: Int): Pair<Habit, HabitProgress> {
        return habitsWithProgress[position]
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHabitName: TextView = itemView.findViewById(R.id.tv_habit_name)
        private val tvHabitTarget: TextView = itemView.findViewById(R.id.tv_habit_target)
        private val tvProgressText: TextView = itemView.findViewById(R.id.tv_progress_text)
        private val tvStreak: TextView = itemView.findViewById(R.id.tv_streak)
        private val cardLongestStreak: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.card_longest_streak)
        private val tvLongestStreak: TextView = itemView.findViewById(R.id.tv_longest_streak)
        private val progressBar: com.google.android.material.progressindicator.LinearProgressIndicator = itemView.findViewById(R.id.progress_bar_habit)
        private val btnToggleCompletion: MaterialButton = itemView.findViewById(R.id.btn_toggle_completion)
        private val btnMenu: MaterialButton = itemView.findViewById(R.id.btn_menu)

        fun bind(habit: Habit, progress: HabitProgress) {
            // Initialize SharedPreferences manager if needed
            if (!::prefsManager.isInitialized) {
                prefsManager = com.example.glowtrack.models.repository.SharedPreferencesManager.getInstance(itemView.context)
            }
            
            // Basic habit info
            tvHabitName.text = habit.name
            tvHabitTarget.text = itemView.context.getString(R.string.habit_target_format, habit.targetValue, habit.unit)

            // Progress info
            val progressPercentage = progress.getProgressPercentage(habit.targetValue)
            tvProgressText.text = itemView.context.getString(R.string.habit_progress_format, 
                progress.value, habit.targetValue, habit.unit)
            progressBar.setProgress(progressPercentage, true)

            // Streak info
            val streak = prefsManager.calculateHabitStreak(habit.id)
            tvStreak.text = itemView.context.getString(R.string.habit_streak, streak)
            
            // Longest streak info
            val longestStreak = prefsManager.calculateLongestHabitStreak(habit.id)
            tvLongestStreak.text = longestStreak.toString()
            // Show the longest streak badge only if it's greater than the current streak
            cardLongestStreak.visibility = if (longestStreak > streak) View.VISIBLE else View.GONE

            // Completion button
            val (buttonText, buttonColor) = if (progress.isCompleted) {
                itemView.context.getString(R.string.mark_incomplete) to R.color.success
            } else {
                itemView.context.getString(R.string.mark_complete) to R.color.primary_green_light
            }
            btnToggleCompletion.text = buttonText
            btnToggleCompletion.setBackgroundColor(itemView.context.getColor(buttonColor))

            // Click listeners
            itemView.setOnClickListener { onHabitClick(habit, itemView) }
            btnToggleCompletion.setOnClickListener { onProgressClick(habit, progress) }
            
            // Menu button setup
            btnMenu.setOnClickListener { view ->
                val popupMenu = androidx.appcompat.widget.PopupMenu(view.context, view)
                popupMenu.menuInflater.inflate(R.menu.habit_item_menu, popupMenu.menu)
                
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit -> {
                            onHabitClick(habit, itemView) // Reuse onHabitClick for edit action
                            true
                        }
                        R.id.action_share -> {
                            onShareClick(habit)
                            true
                        }
                        R.id.action_delete -> {
                            onDeleteClick(habit)
                            true
                        }
                        else -> false
                    }
                }
                
                popupMenu.show()
            }
            
            // Update progress indicator color based on completion
            val progressColor = if (progress.isCompleted) {
                R.color.success
            } else {
                R.color.secondary
            }
            progressBar.setIndicatorColor(itemView.context.getColor(progressColor))
        }
    }
}