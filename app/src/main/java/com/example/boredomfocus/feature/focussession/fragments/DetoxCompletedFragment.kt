package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentDetoxCompletedBinding
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel

class DetoxCompletedFragment : Fragment(R.layout.fragment_detox_completed) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentDetoxCompletedBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetoxCompletedBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}