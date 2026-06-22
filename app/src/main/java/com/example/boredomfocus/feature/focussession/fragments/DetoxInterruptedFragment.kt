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
import com.example.boredomfocus.databinding.FragmentDetoxInterruptedBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch

class DetoxInterruptedFragment : Fragment(R.layout.fragment_detox_interrupted) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxInterruptedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxInterruptedBinding.bind(view)

        binding.btnStartAgain.setOnClickListener {
            viewModel.onRestartDetoxClick()
        }
        binding.btnToHome.setOnClickListener {
            viewModel.onDetoxInterruptedHomeClick()
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

                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToDetoxTimer -> {
                            findNavController().navigate(R.id.actionDetoxInterruptedFragmentToDetoxTimerFragment)
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