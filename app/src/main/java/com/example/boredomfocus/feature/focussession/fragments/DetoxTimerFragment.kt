package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import android.view.View
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
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import com.example.boredomfocus.feature.focussession.dialogs.StopDetoxDialogFragment
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
            findNavController().navigate()
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
                    binding.progressViewDetoxTimer.progress = state.detoxProgress
                    binding.tvDetoxTimer.text = formatSeconds(state.detoxRemainingSeconds)
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToFocusTimer -> {
                            findNavController().navigate(
                                resId = R.id.stopwatchFragment
                            )
                        }
                        is FocusSessionEvent.NavigateToDetoxInterrupted -> {
                            findNavController().popBackStack() // TODO
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
