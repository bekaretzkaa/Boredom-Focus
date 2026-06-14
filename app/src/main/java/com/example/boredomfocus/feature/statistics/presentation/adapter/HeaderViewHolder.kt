package com.example.boredomfocus.feature.statistics.presentation.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.databinding.ItemSessionHeaderBinding

class HeaderViewHolder(
    private val binding: ItemSessionHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: SessionListItem.Header) {
        binding.tvDate.text = item.date
    }

}