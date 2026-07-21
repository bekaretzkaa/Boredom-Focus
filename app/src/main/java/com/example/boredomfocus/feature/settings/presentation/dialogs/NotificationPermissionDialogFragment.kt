package com.example.boredomfocus.feature.settings.presentation.dialogs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.databinding.DialogNotificationPermissionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationPermissionDialogFragment : DialogFragment() {

    private val permissionViewModel: PermissionViewModel by activityViewModels()

    private var _binding: DialogNotificationPermissionBinding? = null
    private val binding get() = _binding!!

    private var awaitingSystemDialogResult = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            awaitingSystemDialogResult = false

            permissionViewModel.onNotificationPermissionResult(granted)

            if (granted) {
                findNavController().popBackStack()
            } else {
                applyUiState(requested = true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomCenterDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNotificationPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.visibility = View.INVISIBLE

        binding.btnChangeEmail.setOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionViewModel.notificationPermissionRequested.collect { requested ->

                    if (awaitingSystemDialogResult) return@collect

                    applyUiState(requested)

                    if (binding.root.visibility != View.VISIBLE) {
                        binding.root.alpha = 0f
                        binding.root.visibility = View.VISIBLE
                        binding.root.animate().alpha(1f).setDuration(120).start()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            permissionViewModel.onNotificationPermissionResult(true)
            findNavController().popBackStack()
        }
    }

    private fun applyUiState(requested: Boolean) {
        val showRequest = !requested ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

        if (showRequest) showRequestUi() else showOpenSettingsUi()
    }

    private fun showRequestUi() {
        binding.cardIcon.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.green_bg)
        )
        binding.cardIcon.setStrokeColor(
            ContextCompat.getColor(requireContext(), R.color.green_basic)
        )

        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_notification_green)

        binding.tvEmail.setText(R.string.settings_notifications_title)
        binding.tvEmailDescription.setText(R.string.settings_notifications_description)

        binding.btnConfirmed.setText(R.string.settings_notifications_allow)
        binding.btnChangeEmail.setText(R.string.settings_notifications_not_now)

        binding.cardInstruction.visibility = View.GONE

        binding.btnConfirmed.setOnClickListener {

            awaitingSystemDialogResult = true
            permissionViewModel.markNotificationPermissionRequested()

            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun showOpenSettingsUi() {
        binding.cardIcon.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg)
        )
        binding.cardIcon.setStrokeColor(
            ContextCompat.getColor(requireContext(), R.color.difficulty_orange)
        )

        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_notification_orange)

        binding.tvEmail.setText(R.string.settings_notifications_disabled_title)
        binding.tvEmailDescription.setText(R.string.settings_notifications_disabled_description)

        binding.btnConfirmed.setText(R.string.settings_notifications_open_settings)
        binding.btnChangeEmail.setText(R.string.settings_notifications_skip)

        binding.cardInstruction.visibility = View.VISIBLE

        binding.btnConfirmed.setOnClickListener {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
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
}