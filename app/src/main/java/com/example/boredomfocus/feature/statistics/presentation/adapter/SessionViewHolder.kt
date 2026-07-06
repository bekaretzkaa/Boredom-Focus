package com.example.boredomfocus.feature.statistics.presentation.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.databinding.ItemSessionBinding

class SessionViewHolder(
    private val binding: ItemSessionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SessionListItem.Session
    ) {
        binding.sessionBarsView.bind(
            item.detoxSelectedMinutes,
            item.detoxElapsedSeconds,
            item.focusTime,
            item.time,
            item.completed
        )
    }

}