package com.example.boredomfocus.feature.focussession.presentation.fragments

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentDetoxTimerBinding
import com.example.boredomfocus.feature.focussession.presentation.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.presentation.FocusSessionViewModel
import kotlinx.coroutines.launch

class DetoxTimerFragment : Fragment(R.layout.fragment_detox_timer) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxTimerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxTimerBinding.bind(view)

        val focusOnly = arguments?.getBoolean("focusOnly") ?: false
        if(focusOnly) {
            findNavController().navigate(
                R.id.actionDetoxTimerFragmentToStopwatchFragment,
                arguments,
                navOptions {
                    popUpTo(R.id.detoxTimerFragment) {
                        inclusive = true
                    }
                }
            )
            return
        }

        binding.btnStop.setOnClickListener {
            viewModel.onInterruptDetoxClick()
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
                    val remainingSeconds = state.detoxUiState.detoxRemainingSeconds
                    val isFinishing = remainingSeconds <= 30L

                    binding.progressViewDetoxTimer.progress = state.detoxUiState.detoxProgress
                    binding.progressViewDetoxTimer.setFinishMode(isFinishing)

                    binding.tvDetoxTimer.text = formatSeconds(remainingSeconds)

                    binding.tvDetoxTimer.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isFinishing) R.color.green_basic else R.color.white
                        )
                    )

                    binding.tvDetoxTimer2.text = if (isFinishing) {
                        getString(R.string.detox_timer_finish)
                    } else {
                        getString(R.string.detox_timer_remaining)
                    }
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    if (findNavController().currentDestination?.id != R.id.detoxTimerFragment) return@collect

                    when(event) {
                        is FocusSessionEvent.NavigateToStopDetoxDialog -> {
                            findNavController().navigate(R.id.actionDetoxTimerFragmentToStopDetoxDialogFragment)
                        }
                        is FocusSessionEvent.NavigateToDetoxCompleted -> {
                            findNavController().navigate(R.id.actionDetoxTimerFragmentToDetoxCompletedFragment)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
