package com.example.glowtrack.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.example.glowtrack.R
import com.example.glowtrack.models.models.Habit
import com.example.glowtrack.models.repository.SharedPreferencesManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class QuickHabitBottomSheetFragment : BottomSheetDialogFragment() {
    
    private lateinit var etHabitName: TextInputEditText
    private lateinit var etTargetValue: TextInputEditText
    private lateinit var spinnerUnit: AutoCompleteTextView
    private lateinit var prefsManager: SharedPreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_quick_habit, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        etHabitName = view.findViewById(R.id.et_habit_name)
        etTargetValue = view.findViewById(R.id.et_target_value)
        spinnerUnit = view.findViewById(R.id.spinner_unit)
        val btnAddHabit = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_habit)
        
        // Initialize SharedPreferences manager
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        
        // Setup unit spinner
        val units = arrayOf("times", "minutes", "hours", "glasses", "pages", "km")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        spinnerUnit.setAdapter(adapter)
        spinnerUnit.setText("times", false)
        
        // Setup add button click listener
        btnAddHabit.setOnClickListener {
            addHabit()
        }
    }
    
    private fun addHabit() {
        val name = etHabitName.text?.toString()?.trim()
        val targetText = etTargetValue.text?.toString()?.trim()
        val unit = spinnerUnit.text?.toString() ?: "times"
        
        if (!name.isNullOrBlank() && !targetText.isNullOrBlank()) {
            try {
                val target = targetText.toInt()
                if (target > 0) {
                    val habit = Habit(
                        name = name,
                        targetValue = target,
                        unit = unit
                    )
                    
                    prefsManager.saveHabit(habit)
                    
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Habit added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Dismiss the bottom sheet
                    dismiss()
                    
                    // Notify the parent fragment to refresh the habits list
                    HabitsFragment.findInstance(this)?.notifyDataChange()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Target value must be greater than 0",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a valid target number",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Please fill in all required fields",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}