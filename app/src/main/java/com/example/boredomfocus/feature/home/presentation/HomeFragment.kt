package com.example.boredomfocus.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.databinding.FragmentHomeBinding
import com.example.boredomfocus.feature.sessionsettings.presentation.SessionSettingsBottomSheet
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
            SessionSettingsBottomSheet()
                .show(childFragmentManager, SessionSettingsBottomSheet.TAG)
        }
        setupSessionBottomSheetResult()

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
                        binding.homeContent.alpha = 0f
                        return@collect
                    }

                    binding.homeContent.alpha = 1f
                    render(state)
                }
            }
        }
    }

    private fun render(state: HomeUiState) {
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

    private fun renderWeekCircles(state: HomeUiState) {
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

    private fun setupSessionBottomSheetResult() {
        childFragmentManager.setFragmentResultListener(
            SessionSettingsBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val detoxDuration =
                bundle.getSerializable(SessionSettingsBottomSheet.KEY_DETOX_DURATION) as? DetoxDuration
                    ?: DetoxDuration.FIVE_MINUTES

            val difficulty =
                bundle.getSerializable(SessionSettingsBottomSheet.KEY_DIFFICULTY) as? Difficulty
                    ?: Difficulty.BEGINNER

            val focusOnly = bundle.getBoolean(SessionSettingsBottomSheet.KEY_FOCUS_ONLY)

            val args = bundleOf(
                "detoxDuration" to detoxDuration,
                "difficulty" to difficulty,
                "focusOnly" to focusOnly
            )

            findNavController().navigate(
                R.id.actionHomeFragmentToFocusSessionGraph,
                args
            )
        }
    }
}