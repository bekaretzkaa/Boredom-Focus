package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentDetoxCompletedBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch

class DetoxCompletedFragment : Fragment(R.layout.fragment_detox_completed) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxCompletedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxCompletedBinding.bind(view)

        binding.btnStartFocus.setOnClickListener {
            viewModel.onStartFocusClick()
        }

        binding.btnToHome.setOnClickListener {
            viewModel.onDetoxCompletedHomeClick()
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
                    binding.tvDetoxTime1.text = formatSeconds(state.detoxUiState.selectedDetoxSeconds)
                    binding.tvNextText2.text = "Сейчас начнётся секундомер фокуса. Твой рекорд — ${formatSeconds(state.focusUiState.focusRecord)}."
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToStopwatch -> {
                            findNavController().navigate(R.id.actionDetoxCompletedFragmentToStopwatchFragment)
                        }
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