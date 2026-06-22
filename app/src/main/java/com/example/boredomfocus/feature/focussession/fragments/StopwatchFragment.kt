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
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentStopwatchBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import com.example.boredomfocus.feature.focussession.dialogs.StopFocusDialogFragment
import kotlinx.coroutines.launch

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStopwatchBinding.bind(view)

        binding.btnStopFocus.setOnClickListener {
            viewModel.onStopFocusClick()
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
                    binding.tvStopwatch.text = formatSeconds(state.focusSeconds)
                    binding.tvTodayTime.text = "${formatSeconds(state.focusSeconds)}..."
                    binding.tvLastRecordTime.text = formatSeconds(state.focusRecord)
                    if(state.isNewRecord) {
                        binding.tvRecordBeating.visibility = View.VISIBLE
                    } else {
                        binding.tvRecordBeating.visibility = View.GONE
                    }
                    if(state.selectedDetoxSeconds == 0L) {
                        binding.cardDetoxResult.visibility = View.GONE
                    } else {
                        binding.cardDetoxResult.visibility = View.VISIBLE
                    }
                    binding.tvDetoxTimerWord.text = "${state.selectedDetoxSeconds / 60} мин · завершён"
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToStopFocusDialog -> {
                            findNavController().navigate(R.id.actionStopwatchFragmentToStopFocusDialogFragment)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

}