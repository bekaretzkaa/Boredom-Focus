package com.example.boredomfocus.feature.focussession.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.daysWord
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentFocusResultBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch

class FocusResultFragment : Fragment(R.layout.fragment_focus_result) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentFocusResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFocusResultBinding.bind(view)

        binding.btnToHome.setOnClickListener {
            viewModel.onFocusResultHomeClick()
        }

        observeUiState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val grayBg = ContextCompat.getColor(requireContext(), R.color.gray_bg)
                    val greenBg = ContextCompat.getColor(requireContext(), R.color.green_bg)
                    val grayText = ContextCompat.getColor(requireContext(), R.color.gray_dark)
                    val greenText = ContextCompat.getColor(requireContext(), R.color.green_basic)
                    val white = ContextCompat.getColor(requireContext(), R.color.white)

                    binding.tvFocusTime.text = formatSeconds(state.focusUiState.focusSeconds)
                    val targets = listOf(
                        binding.cardSession,
                        binding.cardWeek,
                        binding.cardMonth,
                        binding.cardRecord
                    )
                    targets.forEach {
                        it.visibility = View.GONE
                    }
                    binding.llTargets.visibility = View.VISIBLE
                    val focusOnly = state.detoxUiState.detoxElapsedSeconds <= 0
                    if(!focusOnly) {
                        binding.cardDetox.visibility = View.VISIBLE
                        binding.tvDetoxTime.text = "${state.detoxUiState.selectedDetoxSeconds / 60} мин ✓"
                    } else {
                        binding.cardDetox.visibility = View.GONE
                    }
                    val streakCount = state.streakCount
                    binding.tvStreakCount.text = "$streakCount ${daysWord(streakCount)} подряд"

                    binding.tvFocusComparisonWord.text = "рекорд всего времени"
                    binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.focusRecord ?: 0)

                    when(state.focusUiState.focusSeconds) {
                        in 0..(state.focusUiState.previousFocusSeconds ?: 0) -> {
                            binding.cardSessionComparison.setCardBackgroundColor(grayBg)
                            binding.tvSessionComparison.apply {
                                text = "•  сессия завершена"
                                setTextColor(grayText)
                            }
                            binding.tvFocusTime.setTextColor(white)
                            binding.tvFocusComparison.apply {
                                text = "на ${formatSeconds((state.focusUiState.previousFocusSeconds ?: 0) - state.focusUiState.focusSeconds)} меньше прошлой сессий"
                                setTextColor(grayText)
                            }
                            binding.tvFocusComparisonWord.text = "прошлая сессия"
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.previousFocusSeconds ?: 0)

                            binding.llTargets.visibility = View.GONE
                        }
                        in (state.focusUiState.previousFocusSeconds ?: 0)..(state.focusUiState.weekFocusRecord ?: 0) -> {
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = "↑ лучше прошлой сессии"
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0))} от прошлой сессии"
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.tvFocusComparisonWord.text = "рекорд недели"
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.weekFocusRecord ?: 0)
                        }
                        in (state.focusUiState.weekFocusRecord ?: 0)..(state.focusUiState.monthFocusRecord ?: 0) -> {
                            val weekNull = state.focusUiState.weekFocusRecord == null
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = if(weekNull) "↑ лучше прошлой сессии" else "↑ рекорд недели"
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = if(weekNull) "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0))} от прошлой сессии" else "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.weekFocusRecord ?: 0))} от рекорда недели"
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.tvFocusComparisonWord.text = if(weekNull) "прошлая сессия" else "рекорд месяца"
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.monthFocusRecord ?: 0)
                        }
                        in (state.focusUiState.monthFocusRecord ?: 0)..(state.focusUiState.focusRecord) -> {
                            val monthNull = state.focusUiState.monthFocusRecord == null
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = if(monthNull) "↑ лучше прошлой сессии" else "↑ рекорд месяца"
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = if(monthNull) "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0))} от прошлой сессии" else "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.monthFocusRecord ?: 0))} от рекорда месяца"
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.cardMonth.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = "★ новый абсолютный рекорд"
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = "+${formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.focusRecord ?: 0))} от предыдущего рекорда"
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.cardMonth.visibility = View.VISIBLE
                            binding.cardRecord.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateHome -> {
                            findNavController().popBackStack(
                                R.id.homeFragment,
                                false
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}