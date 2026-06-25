package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.boredomfocus.R
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
                    if(false) { // TODO: need to Update isNewFocusRecord
                        binding.tvNewFocusRecord.visibility = View.VISIBLE
                        binding.tvFocusResultComparison.text = "↑ +${formatSeconds(state.focusUiState.focusRecord - state.focusUiState.focusSeconds)} от прошлого рекорда"
                    } else {
                        binding.tvNewFocusRecord.visibility = View.GONE

                        if(state.focusUiState.focusSeconds > state.focusUiState.previousFocusSeconds) {
                            binding.tvFocusResultComparison.text = "↑ +${formatSeconds(state.focusUiState.focusSeconds - state.focusUiState.previousFocusSeconds)} от прошлой сессий"
                        } else {
                            binding.tvFocusResultComparison.text = "↓ ${formatSeconds(state.focusUiState.previousFocusSeconds - state.focusUiState.focusSeconds)} от прошлой сессий"
                        }
                    }

                    binding.tvFocusResultTime.text = formatSeconds(state.focusUiState.focusSeconds)
                    binding.tvDetoxTimerWord2.text = "${state.detoxUiState.selectedDetoxSeconds / 60} мин · завершён"
                    binding.tvStreakCountWord.text = "${state.streakCount} дней подряд 🔥"

                    if(state.detoxUiState.detoxElapsedSeconds == 0L) {
                        binding.cardDetoxResult2Content.visibility = View.GONE
                        binding.tvNewFocusRecord.visibility = View.GONE
                    } else {
                        binding.cardDetoxResult2Content.visibility = View.VISIBLE
                        binding.tvNewFocusRecord.visibility = View.VISIBLE
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