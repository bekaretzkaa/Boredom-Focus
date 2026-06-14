package com.example.boredomfocus.feature.onboarding.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.OnboardingFragmentFirstBinding
import com.example.boredomfocus.feature.onboarding.presentation.OnboardingViewModel

class OnboardingFragmentFirst : Fragment(R.layout.onboarding_fragment_first) {

    private val viewModel: OnboardingViewModel by activityViewModels()

    private var _binding: OnboardingFragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = OnboardingFragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOnboarding1GotIt.setOnClickListener {
            viewModel.onNavigateToPageRequested(1)
        }

        binding.btnOnboarding1Skip.setOnClickListener {
            viewModel.onNavigateToPageRequested(2)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}