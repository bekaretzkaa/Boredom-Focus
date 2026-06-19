package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentStopwatchBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStopwatchBinding.bind(view)

        viewModel.startFocusStopwatch()

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
                    binding.tvStopwatch.text = state.focusTimeText
                    binding.tvTodayTime.text = "${state.focusTimeText}..."

                    binding.btnStopFocus.setOnClickListener {
                        if(viewModel.isFocusRunning) {
                            viewModel.stopFocus()
                        }
                    }
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    if(event is FocusSessionEvent.FocusStopped) {
                        findNavController().navigate(
                            resId = R.id.focusResultFragment
                        )
                    }
                }
            }
        }
    }

}