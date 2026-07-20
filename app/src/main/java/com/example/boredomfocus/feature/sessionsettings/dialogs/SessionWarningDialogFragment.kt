package com.example.boredomfocus.feature.sessionsettings.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.databinding.DialogSessionWarningBinding
import kotlinx.coroutines.launch

class SessionWarningDialogFragment : DialogFragment() {

    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private var _binding: DialogSessionWarningBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "SessionWarningDialogFragment"

        const val ARG_DURATION = "duration"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_FOCUS_ONLY = "focusOnly"

        const val REQUEST_KEY = "warning_result"
        const val KEY_CONFIRMED = "confirmed"

        fun newInstance(
            detoxDuration: DetoxDuration,
            difficulty: Difficulty,
            focusOnly: Boolean
        ) = SessionWarningDialogFragment().apply {
            arguments = bundleOf(
                ARG_DURATION to detoxDuration,
                ARG_DIFFICULTY to difficulty,
                ARG_FOCUS_ONLY to focusOnly
            )
        }
    }

    private val difficulty: Difficulty
        get() = requireArguments().getSerializable(ARG_DIFFICULTY) as Difficulty

    private val detoxDuration: DetoxDuration
        get() = requireArguments().getSerializable(ARG_DURATION) as DetoxDuration

    private val focusOnly: Boolean
        get() = requireArguments().getBoolean(ARG_FOCUS_ONLY)

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
        _binding = DialogSessionWarningBinding.inflate(inflater, container, false)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observePermissions()
    }

    private fun observePermissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionViewModel.uiState.collect { state ->

                    binding.btnStart.setOnClickListener {
                        parentFragmentManager.setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                KEY_CONFIRMED to true,
                                ARG_DURATION to detoxDuration,
                                ARG_DIFFICULTY to difficulty,
                                ARG_FOCUS_ONLY to focusOnly
                            )
                        )
                        dismiss()
                    }

                    binding.btnCancel.setOnClickListener {
                        parentFragmentManager.setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(KEY_CONFIRMED to false)
                        )
                        dismiss()
                    }

                    binding.btnStart.text = getString(R.string.session_warning_button_start)
                    binding.btnCancel.text = getString(R.string.session_warning_button_cancel)
                    binding.cardInstruction.visibility = View.VISIBLE
                    binding.cardSettings.visibility = View.GONE

                    if(difficulty == Difficulty.BEGINNER) {
                        showBeginnerUi()
                    } else if(state.doNotDisturb) {
                        when(difficulty) {
                            Difficulty.BEGINNER -> showBeginnerUi()
                            Difficulty.FIGHTER -> showFighterUi()
                            Difficulty.HARDCORE -> showHardcoreUi()
                        }
                    } else {
                        showPermissionDnd()
                    }
                }
            }
        }
    }

    private fun showBeginnerUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_basic))
        binding.tvDifficulty.text = getString(R.string.session_warning_level_beginner)
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_basic))

        binding.tvSessionDescription.text = getString(R.string.session_warning_beginner_description)
        binding.tvInstruction.text = getString(R.string.session_warning_beginner_instruction)
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    private fun showFighterUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))
        binding.tvDifficulty.text = getString(R.string.session_warning_level_fighter)
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))

        binding.tvSessionDescription.text = getString(R.string.session_warning_fighter_description)
        binding.tvInstruction.text = getString(R.string.session_warning_fighter_instruction)
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }

    private fun showHardcoreUi() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_red_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
        binding.tvDifficulty.text = getString(R.string.session_warning_level_hardcore)
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_basic))

        binding.tvSessionDescription.text = getString(R.string.session_warning_hardcore_description)
        binding.tvInstruction.text = getString(R.string.session_warning_hardcore_instruction)
        binding.tvInstruction.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
    }

    private fun showPermissionDnd() {
        binding.cardDifficulty.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_red_bg))
        binding.cardDifficulty.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red_basic))
        binding.tvDifficulty.text = getString(R.string.session_warning_level_error)
        binding.tvDifficulty.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_basic))

        binding.tvStart.text = getString(R.string.session_warning_permission_title)
        binding.tvSessionDescription.text = getString(R.string.session_warning_permission_description)

        binding.cardInstruction.visibility = View.GONE
        binding.cardSettings.visibility = View.VISIBLE

        binding.btnStart.text = getString(R.string.session_warning_permission_retry)
        binding.btnCancel.text = getString(R.string.session_warning_permission_settings)

        binding.btnStart.setOnClickListener {
            permissionViewModel.refreshPermissions()
        }

        binding.btnCancel.setOnClickListener {
            startActivity(permissionViewModel.getDndSettingsIntent())
        }
    }

}