package com.example.glowtrack.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.glowtrack.R
import com.example.glowtrack.models.models.MoodType

/**
 * Adapter for mood selector grid with enhanced UX and animations
 */
class MoodSelectorAdapter(
    private val onMoodSelected: (MoodType) -> Unit
) : RecyclerView.Adapter<MoodSelectorAdapter.MoodSelectorViewHolder>() {

    private var moods: List<MoodType> = emptyList()
    private var selectedPosition: Int = -1
    private var lastPosition = -1

    fun updateMoods(newMoods: List<MoodType>) {
        moods = newMoods
        notifyDataSetChanged()
        lastPosition = -1
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = -1
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodSelectorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_selector, parent, false)
        return MoodSelectorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodSelectorViewHolder, position: Int) {
        val mood = moods[position]
        holder.bind(mood, position == selectedPosition)
        
        // Add animation to items when they appear
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_up)
            holder.itemView.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = moods.size

    inner class MoodSelectorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMoodEmoji: TextView = itemView.findViewById(R.id.tv_mood_emoji)
        private val tvMoodLabel: TextView = itemView.findViewById(R.id.tv_mood_label)

        fun bind(mood: MoodType, isSelected: Boolean) {
            tvMoodEmoji.text = mood.emoji
            tvMoodLabel.text = mood.label

            // Update selection state
            itemView.isSelected = isSelected
            
            // Apply scale animation when selected
            if (isSelected) {
                itemView.scaleX = 1.1f
                itemView.scaleY = 1.1f
            } else {
                itemView.scaleX = 1.0f
                itemView.scaleY = 1.0f
            }

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                // Update the previous selected item
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                // Update the current selected item
                notifyItemChanged(selectedPosition)
                
                // Add selection animation
                val selectAnimation = AnimationUtils.loadAnimation(
                    itemView.context, 
                    R.anim.select_animation
                )
                itemView.startAnimation(selectAnimation)
                
                onMoodSelected(mood)
            }
        }
    }
}