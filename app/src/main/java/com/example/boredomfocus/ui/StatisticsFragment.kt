package com.example.boredomfocus.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.boredomfocus.R
import com.example.boredomfocus.SessionAdapter
import com.example.boredomfocus.SessionListItem
import com.example.boredomfocus.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionAdapter: SessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        val sessions = listOf(
            SessionListItem.Header("Сегодня"),

            SessionListItem.Session(
                300,
                935
            ),
            SessionListItem.Session(
                600,
                1233
            ),

            SessionListItem.Header("9 июня"),

            SessionListItem.Session(
                300,
                2100
            ),
            SessionListItem.Session(
                600,
                780
            ),

            SessionListItem.Header("8 июня"),

            SessionListItem.Session(
                300,
                633
            ),
            SessionListItem.Session(
                900,
                4256
            ),
        )

        sessionAdapter = SessionAdapter()

        binding.rvSessionsStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }

        sessionAdapter.submitList(sessions)
    }

}