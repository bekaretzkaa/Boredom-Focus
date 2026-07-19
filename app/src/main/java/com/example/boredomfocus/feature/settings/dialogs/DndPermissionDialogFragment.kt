package com.example.boredomfocus.feature.settings.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.databinding.DialogDndPermissionBinding
import com.example.boredomfocus.databinding.DialogNotificationPermissionBinding
import com.example.boredomfocus.feature.settings.presentation.SettingsViewModel
import kotlin.getValue

class DndPermissionDialogFragment : DialogFragment() {

    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private var _binding: DialogDndPermissionBinding? = null
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
        _binding = DialogDndPermissionBinding.inflate(inflater, container, false)
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

    override fun onResume() {
        super.onResume()

        if (!openedSettings) return

        permissionViewModel.refreshPermissions()
        if (permissionViewModel.uiState.value.doNotDisturb) {
            permissionViewModel.refreshPermissions()
            findNavController().popBackStack()
        } else {
            showSecondUi()
        }
    }

    private var openedSettings = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showFirstUi()

        binding.btnChangeEmail.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showFirstUi() {
        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_dnd)
        binding.tvEmail.setText(R.string.settings_dnd_title)
        binding.tvEmailDescription.setText(R.string.settings_dnd_description)
        binding.btnConfirmed.setText(R.string.settings_dnd_open_settings)
        binding.btnChangeEmail.setText(R.string.settings_dnd_skip)
        binding.cardInstruction.visibility = View.VISIBLE

        binding.cardWarning.visibility = View.GONE

        binding.btnConfirmed.setOnClickListener {
            openedSettings = true
            startActivity(permissionViewModel.getDndSettingsIntent())
        }
    }

    private fun showSecondUi() {
        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_dnd_off)
        binding.tvEmail.setText(R.string.settings_dnd_not_granted_title)
        binding.tvEmailDescription.setText(R.string.settings_dnd_not_granted_description)
        binding.btnConfirmed.setText(R.string.settings_dnd_try_again)
        binding.btnChangeEmail.setText(R.string.settings_dnd_continue_without)
        binding.cardInstruction.visibility = View.GONE

        binding.cardWarning.visibility = View.VISIBLE

        binding.btnConfirmed.setOnClickListener {
            startActivity(permissionViewModel.getDndSettingsIntent())
        }
    }
}