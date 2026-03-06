package com.example.glowtrack.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.models.HabitProgress
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.example.glowtrack.ui.adapters.HabitsAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.SimpleDateFormat
import java.util.*
/**
 * Fragment for displaying and managing daily habits
 */
class HabitsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddHabit: ExtendedFloatingActionButton
    private lateinit var tvProgressSummary: TextView
    private lateinit var layoutEmptyState: View
    private lateinit var tvHabitCount: TextView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var tvTotalHabits: TextView
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var habitsAdapter: HabitsAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pieChart: PieChart
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipHealth: Chip
    private lateinit var chipFitness: Chip
    private lateinit var chipLearning: Chip
    private lateinit var chipProductivity: Chip
    private lateinit var chipWellness: Chip
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize components
        initializeViews(view)
        setupTabLayoutIfNeeded() // Only setup tab layout if components exist
        setupHabitsAdapter() // Initialize adapter before setting up RecyclerView
        setupRecyclerView()
        setupClickListeners()
        setupSwipeActions()
        setupFilterChips()
        loadHabits()
        
        // Handle edge-to-edge design
        handleEdgeToEdge()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadHabits()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        recyclerView = view.findViewById(R.id.recycler_habits)
        fabAddHabit = view.findViewById(R.id.fab_add_habit)
        tvProgressSummary = view.findViewById(R.id.tv_progress_summary)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tvProgressPercentage = view.findViewById(R.id.tv_progress_percentage)
        tvStreakCount = view.findViewById(R.id.tv_streak_count)
        tvTotalHabits = view.findViewById(R.id.tv_total_habits)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        pieChart = view.findViewById(R.id.chart_habit_progress)
        chipGroupCategories = view.findViewById(R.id.chip_group_categories)
        chipAll = view.findViewById(R.id.chip_all)
        chipHealth = view.findViewById(R.id.chip_health)
        chipFitness = view.findViewById(R.id.chip_fitness)
        chipLearning = view.findViewById(R.id.chip_learning)
        chipProductivity = view.findViewById(R.id.chip_productivity)
        chipWellness = view.findViewById(R.id.chip_wellness)
    }
    
    private fun setupHabitsAdapter() {
        habitsAdapter = HabitsAdapter(
            onHabitClick = { habit, view -> showHabitDetail(habit, view) },
            onProgressClick = { habit, progress -> toggleHabitProgress(habit, progress) },
            onDeleteClick = { habit -> deleteHabit(habit) },
            onShareClick = { habit -> shareHabitProgress(habit) }
        )
    }
    
    private fun setupRecyclerView() {
        // Check if RecyclerView exists in current layout (only exists in tablet layout)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_habits)
        if (recyclerView != null) {
            // Use different layout managers based on screen size
            val layoutManager = if (isTablet()) {
                androidx.recyclerview.widget.GridLayoutManager(context, 2)
            } else {
                LinearLayoutManager(context)
            }
            
            recyclerView.apply {
                this.layoutManager = layoutManager
                adapter = habitsAdapter
                setHasFixedSize(true)
            }
        }
        // For phone layout, habits will be displayed in ViewPager2 fragments
    }
    
    private fun setupClickListeners() {
        fabAddHabit.setOnClickListener {
            addNewHabit()
        }
        
        fabAddHabit.setOnLongClickListener {
            // Show quick habit entry bottom sheet
            val bottomSheet = QuickHabitBottomSheetFragment()
            bottomSheet.show(parentFragmentManager, "QuickHabitBottomSheetFragment")
            true
        }
    }
    
    private fun setupFilterChips() {
        chipAll.setOnClickListener {
            // Load all habits
            loadHabits()
        }
        
        chipHealth.setOnClickListener {
            // Filter by health category
            filterHabitsByCategory("Health")
        }
        
        chipFitness.setOnClickListener {
            // Filter by fitness category
            filterHabitsByCategory("Fitness")
        }
        
        chipLearning.setOnClickListener {
            // Filter by learning category
            filterHabitsByCategory("Learning")
        }
        
        chipProductivity.setOnClickListener {
            // Filter by productivity category
            filterHabitsByCategory("Productivity")
        }
        
        chipWellness.setOnClickListener {
            // Filter by wellness category
            filterHabitsByCategory("Wellness")
        }
    }
    
    private fun setupSwipeActions() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // We don't want drag & drop
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val (habit, progress) = habitsAdapter.getHabitAtPosition(position)
                
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Delete habit
                        deleteHabit(habit)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Mark complete/incomplete
                        toggleHabitProgress(habit, progress)
                    }
                }
                
                // Notify adapter about the change
                habitsAdapter.notifyItemChanged(position)
            }
            
            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Customize swipe background and icons
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        
        // Apply to RecyclerView if it exists
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_habits)
        if (recyclerView != null) {
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }
    }
    
    fun loadHabits() {
        // Show loading state
        showLoadingState()
        
        try {
            val habits = prefsManager.getHabits()
            val today = dateFormat.format(Date())
            
            // Update total habits count
            tvTotalHabits.text = habits.size.toString()
            
            // Check if RecyclerView exists (tablet layout) or use ViewPager approach (phone layout)
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_habits)
            if (recyclerView != null) {
                // Tablet layout with RecyclerView
                if (habits.isEmpty()) {
                    // Show empty state
                    recyclerView.visibility = View.GONE
                    layoutEmptyState?.visibility = View.VISIBLE
                    tvProgressSummary.text = "0 of 0 habits completed (0%)"
                    tvProgressPercentage.text = "0%"
                    tvStreakCount.text = "0"
                    // Update progress chart
                    updateProgressChart(0, 0)
                    showContentState()
                    return
                }
                
                // Hide empty state
                recyclerView.visibility = View.VISIBLE
                layoutEmptyState?.visibility = View.GONE
                
                // Get today's progress for each habit
                val habitsWithProgress = habits.map { habit ->
                    val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                        ?: HabitProgress(
                            habitId = habit.id,
                            date = today,
                            isCompleted = false,
                            value = 0,
                            completedAt = null
                        )
                    Pair(habit, progress)
                }
                
                // Update progress summary
                val completedCount = habitsWithProgress.count { it.second.isCompleted }
                val totalCount = habits.size
                val progressPercentage = if (totalCount > 0) {
                    ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
                } else {
                    0
                }
                
                tvProgressSummary.text = "$completedCount of $totalCount habits completed ($progressPercentage%)"
                tvProgressPercentage.text = "$progressPercentage%"
                tvStreakCount.text = calculateStreak(habits).toString()
                
                // Update progress chart
                updateProgressChart(completedCount, totalCount)
                
                // Make sure the adapter is not null and update habits
                if (::habitsAdapter.isInitialized) {
                    habitsAdapter.updateHabits(habitsWithProgress)
                }
            } else {
                // Phone layout - update summary information only for now
                if (habits.isEmpty()) {
                    layoutEmptyState?.visibility = View.VISIBLE
                    tvProgressSummary.text = "0 of 0 habits completed (0%)"
                    tvProgressPercentage.text = "0%"
                    tvStreakCount.text = "0"
                    // Update progress chart
                    updateProgressChart(0, 0)
                    showContentState()
                    return
                }
                
                layoutEmptyState?.visibility = View.GONE
                
                // Update progress summary
                val completedCount = habits.count { habit ->
                    val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                    progress?.isCompleted ?: false
                }
                val totalCount = habits.size
                val progressPercentage = if (totalCount > 0) {
                    ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
                } else {
                    0
                }
                
                tvProgressSummary.text = "$completedCount of $totalCount habits completed ($progressPercentage%)"
                tvProgressPercentage.text = "$progressPercentage%"
                tvStreakCount.text = calculateStreak(habits).toString()
                
                // Update progress chart
                updateProgressChart(completedCount, totalCount)
            }
            
            // Show content state
            showContentState()
        } catch (e: Exception) {
            // Show error state
            showErrorState(e.message ?: "An error occurred")
        }
    }
    
    private fun showLoadingState() {
        // In a real implementation, you would show a loading indicator
        // For now, we'll just log
        android.util.Log.d("HabitsFragment", "Loading habits...")
    }
    
    private fun showContentState() {
        // In a real implementation, you would hide loading indicators
        // For now, we'll just log
        android.util.Log.d("HabitsFragment", "Habits loaded successfully")
    }
    
    private fun showErrorState(errorMessage: String) {
        // Show error message to user
        android.widget.Toast.makeText(requireContext(), "Error: $errorMessage", android.widget.Toast.LENGTH_LONG).show()
        android.util.Log.e("HabitsFragment", "Error loading habits: $errorMessage")
        
        // Show error state UI if we have one
        layoutEmptyState?.visibility = View.VISIBLE
    }
    
    private fun calculateStreak(habits: List<Habit>): Int {
        // Simple streak calculation - in a real app, this would be more sophisticated
        var streak = 0
        val calendar = Calendar.getInstance()
        
        // Check for the last 7 days
        for (i in 0..6) {
            val date = dateFormat.format(calendar.time)
            val completedHabits = habits.count { habit ->
                val progress = prefsManager.getHabitProgressForDay(habit.id, date)
                progress?.isCompleted ?: false
            }
            
            // If all habits were completed on this day
            if (completedHabits > 0 && completedHabits == habits.size) {
                streak++
            } else if (i > 0) { // Break streak if not today and not all habits completed
                break
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return streak
    }
    
    private fun updateProgressChart(completed: Int, total: Int) {
        // Clear any existing data
        pieChart.clear()
        
        // Create entries for the pie chart
        val entries = ArrayList<PieEntry>()
        
        if (total > 0) {
            entries.add(PieEntry(completed.toFloat(), "Completed"))
            entries.add(PieEntry((total - completed).toFloat(), "Remaining"))
        } else {
            // If no habits, show 100% as "No habits"
            entries.add(PieEntry(100f, "No habits"))
        }
        
        // Create dataset
        val dataSet = PieDataSet(entries, "Habit Progress")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 12f
        
        // Create pie data
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        
        // Configure pie chart
        pieChart.apply {
            this.data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            setUsePercentValues(true)
            holeRadius = 60f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setCenterTextSize(14f)
            setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            setDrawCenterText(true)
            setCenterText("$completed/$total\nCompleted")
            
            legend.apply {
                isEnabled = true
                textSize = 12f
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            }
            
            invalidate() // Refresh chart
        }
    }
    
    private fun filterHabitsByCategory(category: String) {
        // For now, we'll just reload all habits since we don't have category data in the Habit model
        // In a real implementation, you would filter the habits by category
        loadHabits()
    }
    
    private fun showHabitDetail(habit: Habit, view: View) {
        // For now, we'll just show a toast
        // In a real implementation, you would navigate to the habit detail fragment
        android.widget.Toast.makeText(requireContext(), "Showing details for ${habit.name}", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun handleEdgeToEdge() {
        val rootView = view ?: return
        
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply insets to the main content
            view.findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_view)?.let { scrollView ->
                scrollView.updatePadding(top = systemBars.top)
            }
            
            // Apply insets to the FAB
            val fabLayoutParams = fabAddHabit.layoutParams as android.view.ViewGroup.MarginLayoutParams
            fabLayoutParams.bottomMargin = systemBars.bottom + 32.dpToPx(requireContext())
            fabLayoutParams.rightMargin = systemBars.right + 32.dpToPx(requireContext())
            fabAddHabit.layoutParams = fabLayoutParams
            
            insets
        }
    }
    
    // Extension function to convert dp to pixels
    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
    
    private fun addNewHabit() {
        showAddHabitDialog()
    }
    
    fun editHabit(habit: Habit) {
        showEditHabitDialog(habit)
    }
    
    private fun toggleHabitProgress(habit: Habit, currentProgress: HabitProgress) {
        val today = dateFormat.format(Date())
        val newProgress = if (currentProgress.isCompleted) {
            // Mark as incomplete
            HabitProgress(
                habitId = habit.id,
                date = today,
                isCompleted = false,
                value = 0,
                completedAt = null
            )
        } else {
            // Mark as complete
            HabitProgress(
                habitId = habit.id,
                date = today,
                isCompleted = true,
                value = habit.targetValue,
                completedAt = System.currentTimeMillis()
            )
        }
        
        prefsManager.saveHabitProgressForDay(habit.id, today, newProgress)
        notifyDataChange() // Use notifyDataChange instead of loadHabits
    }
    
    private fun deleteHabit(habit: Habit) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_habit))
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                prefsManager.deleteHabit(habit.id)
                notifyDataChange() // Use notifyDataChange instead of loadHabits
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
        
        val shareText = getString(R.string.share_habit_progress, progressText)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
    
    private fun setupUnitSelector(dialogView: View, spinnerUnit: AutoCompleteTextView, chipGroup: ChipGroup) {
        // Set up chip group click listeners
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                spinnerUnit.setText(chip.text.toString(), false)
                chipGroup.visibility = View.GONE
            }
        }
        
        // Set up spinner click to show chips
        spinnerUnit.setOnClickListener {
            chipGroup.visibility = if (chipGroup.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }
    
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)

        // Initialize views with proper null safety
        val etHabitName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_habit_name)
        val etHabitDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_habit_description)
        val etTargetValue = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_target_value)
        val spinnerUnit = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.spinner_unit)
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_units)
        val chipGroupCategories = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_categories)
        val chipGroupFrequency = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_frequency)
        val switchReminder = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_reminder)
        val tvReminderTime = dialogView.findViewById<android.widget.TextView>(R.id.tv_reminder_time)
        val layoutReminderTime = dialogView.findViewById<android.widget.LinearLayout>(R.id.layout_reminder_time)
        
        // Setup unit selector
        val units = arrayOf("times", "minutes", "hours", "glasses", "pages", "km")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        spinnerUnit.setAdapter(adapter)
        spinnerUnit.setText("times", false)
        
        // Setup unit selector with chips
        setupUnitSelector(dialogView, spinnerUnit, chipGroup)
        
        // Setup reminder toggle
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Setup reminder time click listener
        tvReminderTime.setOnClickListener {
            showTimePickerDialog(tvReminderTime)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_habit))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etHabitName.text?.toString()?.trim()
                val description = etHabitDescription.text?.toString()?.trim() ?: ""
                val targetText = etTargetValue.text?.toString()?.trim()
                val unit = spinnerUnit.text?.toString() ?: "times"
                val categoryId = getSelectedChipId(chipGroupCategories)
                val frequencyId = getSelectedChipId(chipGroupFrequency)
                val reminderEnabled = switchReminder.isChecked
                val reminderTime = tvReminderTime.text.toString()
                
                if (!name.isNullOrBlank() && !targetText.isNullOrBlank()) {
                    try {
                        val target = targetText.toInt()
                        if (target > 0) {
                            val habit = Habit(
                                name = name,
                                description = description,
                                targetValue = target,
                                unit = unit,
                                reminderEnabled = reminderEnabled,
                                reminderTime = reminderTime
                            )
                            prefsManager.saveHabit(habit)
                            
                            // Schedule habit reminder if enabled
                            if (reminderEnabled) {
                                com.example.glowtrack.receivers.HabitAlarmScheduler.scheduleHabitReminder(requireContext(), habit)
                            }
                            
                            notifyDataChange() // Use notifyDataChange instead of loadHabits
                            
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Habit added successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Target value must be greater than 0",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid target number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please fill in all required fields",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showEditHabitDialog(habit: Habit) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)
        
        val etHabitName = dialogView.findViewById<TextInputEditText>(R.id.et_habit_name)
        val etHabitDescription = dialogView.findViewById<TextInputEditText>(R.id.et_habit_description)
        val etTargetValue = dialogView.findViewById<TextInputEditText>(R.id.et_target_value)
        val spinnerUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_unit)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_units)
        val chipGroupCategories = dialogView.findViewById<ChipGroup>(R.id.chip_group_categories)
        val chipGroupFrequency = dialogView.findViewById<ChipGroup>(R.id.chip_group_frequency)
        val switchReminder = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_reminder)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_reminder_time)
        val layoutReminderTime = dialogView.findViewById<android.widget.LinearLayout>(R.id.layout_reminder_time)
        
        // Pre-fill with current values
        etHabitName.setText(habit.name)
        etHabitDescription.setText(habit.description)
        etTargetValue.setText(habit.targetValue.toString())
        switchReminder.isChecked = habit.reminderEnabled
        tvReminderTime.text = habit.reminderTime
        
        // Show/hide reminder time based on initial state
        layoutReminderTime.visibility = if (habit.reminderEnabled) View.VISIBLE else View.GONE
        
        // Setup unit selector
        val units = arrayOf("times", "minutes", "hours", "glasses", "pages", "km")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        spinnerUnit.setAdapter(adapter)
        spinnerUnit.setText(habit.unit, false)
        
        // Setup unit selector with chips
        setupUnitSelector(dialogView, spinnerUnit, chipGroup)
        
        // Setup reminder toggle
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Setup reminder time click listener
        tvReminderTime.setOnClickListener {
            showTimePickerDialog(tvReminderTime)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_habit))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etHabitName.text?.toString()?.trim()
                val description = etHabitDescription.text?.toString()?.trim() ?: ""
                val targetText = etTargetValue.text?.toString()?.trim()
                val unit = spinnerUnit.text?.toString() ?: "times"
                val categoryId = getSelectedChipId(chipGroupCategories)
                val frequencyId = getSelectedChipId(chipGroupFrequency)
                val reminderEnabled = switchReminder.isChecked
                val reminderTime = tvReminderTime.text.toString()
                
                if (!name.isNullOrBlank() && !targetText.isNullOrBlank()) {
                    try {
                        val target = targetText.toInt()
                        if (target > 0) {
                            val updatedHabit = habit.copy(
                                name = name,
                                description = description,
                                targetValue = target,
                                unit = unit,
                                reminderEnabled = reminderEnabled,
                                reminderTime = reminderTime
                            )
                            prefsManager.saveHabit(updatedHabit)
                            
                            // Schedule or cancel habit reminder based on settings
                            if (reminderEnabled) {
                                com.example.glowtrack.receivers.HabitAlarmScheduler.scheduleHabitReminder(requireContext(), updatedHabit)
                            } else {
                                com.example.glowtrack.receivers.HabitAlarmScheduler.cancelHabitReminder(requireContext(), habit.id)
                            }
                            
                            notifyDataChange() // Use notifyDataChange instead of loadHabits
                            
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Habit updated successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Target value must be greater than 0",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid target number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please fill in all required fields",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showTimePickerDialog(timeTextView: TextView) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = android.app.TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeTextView.text = formatTime(formattedTime)
            },
            hour,
            minute,
            false // 12-hour format
        )
        
        timePickerDialog.show()
    }
    
    private fun formatTime(time: String): String {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        
        val amPm = if (hour >= 12) "PM" else "AM"
        val formattedHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        
        return String.format("%02d:%02d %s", formattedHour, minute, amPm)
    }
    
    private fun getSelectedChipId(chipGroup: ChipGroup): Int {
        val checkedId = chipGroup.checkedChipId
        return if (checkedId != View.NO_ID) checkedId else -1
    }
    
    private fun isTablet(): Boolean {
        val configuration = resources.configuration
        return configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    
    private fun setupTabLayoutIfNeeded() {
        // Check if tab layout components exist in current layout
        val tabLayout = view?.findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = view?.findViewById<ViewPager2>(R.id.view_pager)
        
        if (tabLayout != null && viewPager != null) {
            setupTabLayout()
        }
    }
    
    private fun setupTabLayout() {
        // Setup ViewPager with habit adapter
        val tabTitles = arrayOf("All Habits", "In Progress", "Completed")
        viewPager.adapter = HabitPagerAdapter(requireActivity())
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
    
    // Simplify the ViewPager adapter to make sure it works
    inner class HabitPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 3
        
        override fun createFragment(position: Int): Fragment {
            // Create a proper fragment that displays habits
            val fragment = com.example.glowtrack.ui.fragments.HabitListFragment.newInstance(position)
            // Set this HabitsFragment as the target fragment for communication
            fragment.setTargetFragment(this@HabitsFragment, 0)
            return fragment
        }
    }
    
    // Add a companion object to define the callback interface
    companion object {
        // Interface for habit data change callbacks
        interface OnHabitDataChangeListener {
            fun onHabitDataChanged()
        }
        
        // Add a method to get the instance of HabitsFragment from anywhere
        fun findInstance(fragment: Fragment): HabitsFragment? {
            var parent: Fragment? = fragment
            while (parent != null) {
                if (parent is HabitsFragment) {
                    return parent
                }
                parent = parent.parentFragment
            }
            return null
        }
    }
    
    // Add a list to hold listeners
    private val dataChangeListeners = mutableListOf<OnHabitDataChangeListener>()
    
    // Add method to register listeners
    fun addDataChangeListener(listener: OnHabitDataChangeListener) {
        if (!dataChangeListeners.contains(listener)) {
            dataChangeListeners.add(listener)
        }
    }
    
    // Add method to unregister listeners
    fun removeDataChangeListener(listener: OnHabitDataChangeListener) {
        dataChangeListeners.remove(listener)
    }
    
    // Add method to notify all listeners
    fun notifyDataChange() {
        // Notify this fragment's components
        loadHabits()
        
        // Notify all registered listeners
        dataChangeListeners.forEach { it.onHabitDataChanged() }
    }
}