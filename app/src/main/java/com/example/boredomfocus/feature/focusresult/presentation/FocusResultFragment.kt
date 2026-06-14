package com.example.boredomfocus.feature.focusresult.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentFocusResultBinding

class FocusResultFragment : Fragment(R.layout.fragment_focus_result) {

    private var _binding: FragmentFocusResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFocusResultBinding.bind(view)

        binding.btnToHome.setOnClickListener {
            findNavController().navigate(
                R.id.homeFragment,
                null,
                navOptions {
                    popUpTo(R.id.homeFragment) {
                        inclusive = false
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}