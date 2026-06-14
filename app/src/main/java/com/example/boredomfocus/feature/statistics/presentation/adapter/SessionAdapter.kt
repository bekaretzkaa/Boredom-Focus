package com.example.boredomfocus.feature.statistics.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.databinding.ItemSessionBinding
import com.example.boredomfocus.databinding.ItemSessionHeaderBinding
import com.example.boredomfocus.feature.statistics.presentation.adapter.HeaderViewHolder
import com.example.boredomfocus.feature.statistics.presentation.adapter.SessionViewHolder

class SessionAdapter :
        ListAdapter<SessionListItem, RecyclerView.ViewHolder>(
            SessionDiffCallback()
        ) {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SESSION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is SessionListItem.Header -> TYPE_HEADER
            is SessionListItem.Session -> TYPE_SESSION
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when(viewType) {

            TYPE_HEADER -> {
                val binding = ItemSessionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }

            else -> {
                val binding = ItemSessionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                SessionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when(val item = getItem(position)) {
            is SessionListItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is SessionListItem.Session -> {
                (holder as SessionViewHolder).bind(item)
            }
        }
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<SessionListItem>() {
    override fun areItemsTheSame(
        oldItem: SessionListItem,
        newItem: SessionListItem
    ): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(
        oldItem: SessionListItem,
        newItem: SessionListItem
    ): Boolean {
        return oldItem == newItem
    }
}