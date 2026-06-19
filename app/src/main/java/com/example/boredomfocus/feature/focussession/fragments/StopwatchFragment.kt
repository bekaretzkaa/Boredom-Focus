package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentStopwatchBinding
import com.example.boredomfocus.feature.stopwatch.presentation.StopwatchViewModel
import kotlinx.coroutines.launch

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {

    private val viewModel: StopwatchViewModel by viewModels()

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStopwatchBinding.bind(view)

        observeUi()

        if(!viewModel.isRunning) {
            viewModel.start()
        }

        binding.btnStopFocus.setOnClickListener {
            if(viewModel.isRunning) {
                viewModel.stop()
                findNavController().navigate(
                    resId = R.id.focusResultFragment
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    viewModel.time.collect {
                        binding.tvStopwatch.text = it
                    }
                }

                launch {
                    viewModel.time.collect {
                        binding.tvTodayTime.text = "$it..."
                    }
                }
            }
        }
    }

}