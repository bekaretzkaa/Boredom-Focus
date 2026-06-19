package com.example.boredomfocus.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnStartDetox.setOnClickListener {
            findNavController().navigate(
                resId = R.id.detoxTimerFragment
            )
        }

        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        binding.homeContent.visibility = View.INVISIBLE
                        return@collect
                    }

                    binding.homeContent.visibility = View.VISIBLE
                    render(state)
                }
            }
        }
    }

    private fun render(state: HomeUIState) {
        renderWeekCircles(state)

        binding.tvStreakCount.text = state.streakCount.toString()
        binding.tvSessionCount.text = state.sessionCount.toString()
        binding.tvStreak2.text = formatSeconds(state.focusRecord)

        if(state.todayStreak) {
            binding.tvStreakCounted.visibility = View.VISIBLE
            binding.viewStreakCounted.visibility = View.VISIBLE
            binding.btnStartDetox.text = "Ещё одна сессия"
        } else {
            binding.tvStreakCounted.visibility = View.GONE
            binding.viewStreakCounted.visibility = View.GONE
            binding.btnStartDetox.text = "Начать детокс"
        }
    }

    private fun renderWeekCircles(state: HomeUIState) {
        val days = listOf(
            binding.viewMonday,
            binding.viewTuesday,
            binding.viewWednesday,
            binding.viewThursday,
            binding.viewFriday,
            binding.viewSaturday,
            binding.viewSunday
        )

        days.forEachIndexed { index, view ->
            val entity = state.dailyStats.getOrNull(index)
            Log.d("HomeFragment", "renderWeekCircles: $entity")

            val background = when {
                entity == null -> R.drawable.circle_empty
                entity.streakCounted -> R.drawable.circle_filled
                else -> R.drawable.circle_missed
            }

            view.setBackgroundResource(background)
        }
    }
}