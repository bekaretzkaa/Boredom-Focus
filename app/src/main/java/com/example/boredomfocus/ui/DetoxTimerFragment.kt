package com.example.boredomfocus.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentDetoxTimerBinding
import com.example.boredomfocus.viewmodel.DetoxTimerViewModel
import kotlinx.coroutines.launch

class DetoxTimerFragment : Fragment(R.layout.fragment_detox_timer) {

    private val viewModel: DetoxTimerViewModel by viewModels()

    private var _binding: FragmentDetoxTimerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxTimerBinding.bind(view)

        observeUi()

        if(!viewModel.isRunning) {
            viewModel.startTimer(10)
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
                    viewModel.progress.collect {
                        binding.progressViewDetoxTimer.progress = it
                    }
                }

                launch {
                    viewModel.time.collect {
                        binding.tvDetoxTimer.text = it
                    }
                }

                launch {
                    viewModel.finished.collect {
                        if(it) {
                            findNavController().navigate(
                                resId = R.id.stopwatchFragment
                            )
                        }
                    }
                }
            }
        }
    }
}