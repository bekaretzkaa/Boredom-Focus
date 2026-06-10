package com.example.boredomfocus

import androidx.recyclerview.widget.RecyclerView
import com.example.boredomfocus.databinding.ItemSessionBinding

class SessionViewHolder(
    private val binding: ItemSessionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: SessionListItem.Session
    ) {
        binding.sessionBarsView.bind(
            item.detoxTime,
            item.focusTime
        )
    }

}