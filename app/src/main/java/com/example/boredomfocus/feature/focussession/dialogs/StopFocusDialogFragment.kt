package com.example.boredomfocus.feature.focussession.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.DialogStopFocusBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch

class StopFocusDialogFragment : DialogFragment() {

    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)
    private var _binding: DialogStopFocusBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.CustomCenterDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogStopFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val params = attributes
            params.dimAmount = 0.78f
            attributes = params

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnContinue.setOnClickListener {
            viewModel.onDeclineStopFocusClick()

            findNavController().popBackStack()
        }

        binding.btnStop.setOnClickListener {
            viewModel.onConfirmStopFocusClick()

            findNavController().navigate(R.id.actionStopFocusDialogFragmentToFocusResultFragment)
        }

        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.tvFocusTimePassed2.text = formatSeconds(state.focusUiState.focusSeconds)

                    when(state.focusUiState.focusSeconds) {
                        in 0..(state.focusUiState.previousFocusSeconds ?: 0) -> {
                            binding.tvFocusTimeRecord1.text = getString(R.string.stop_focus_previous_session)
                            binding.tvFocusTimeRecord2.text = formatSeconds(state.focusUiState.previousFocusSeconds ?: 0)
                        }
                        in (state.focusUiState.previousFocusSeconds ?: 0)..(state.focusUiState.weekFocusRecord ?: 0) -> {
                            binding.tvFocusTimeRecord1.text = getString(R.string.stop_focus_week_record)
                            binding.tvFocusTimeRecord2.text = formatSeconds(state.focusUiState.weekFocusRecord ?: 0)
                        }
                        in (state.focusUiState.weekFocusRecord ?: 0)..(state.focusUiState.monthFocusRecord ?: 0) -> {
                            binding.tvFocusTimeRecord1.text = getString(R.string.stop_focus_month_record)
                            binding.tvFocusTimeRecord2.text = formatSeconds(state.focusUiState.monthFocusRecord ?: 0)
                        }
                        else -> {
                            binding.tvFocusTimeRecord1.text = getString(R.string.stop_focus_all_time_record)
                            binding.tvFocusTimeRecord2.text = formatSeconds(state.focusUiState.focusRecord)
                        }
                    }
                }
            }
        }
    }
}