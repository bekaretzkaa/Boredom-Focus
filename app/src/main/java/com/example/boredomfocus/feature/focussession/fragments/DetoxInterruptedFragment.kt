package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentDetoxInterruptedBinding
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel

class DetoxInterruptedFragment : Fragment(R.layout.fragment_detox_interrupted) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxInterruptedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxInterruptedBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}