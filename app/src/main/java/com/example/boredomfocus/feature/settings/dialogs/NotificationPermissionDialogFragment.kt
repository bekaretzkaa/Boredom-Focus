package com.example.boredomfocus.feature.settings.dialogs

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
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.databinding.DialogNotificationPermissionBinding
import com.example.boredomfocus.feature.settings.presentation.SettingsViewModel
import kotlin.getValue

class NotificationPermissionDialogFragment : DialogFragment() {

    private val permissionViewModel: PermissionViewModel by activityViewModels()

    private var _binding: DialogNotificationPermissionBinding? = null
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
        _binding = DialogNotificationPermissionBinding.inflate(inflater, container, false)
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

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            permissionViewModel.onNotificationPermissionResult(true)
            findNavController().popBackStack()
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            permissionViewModel.onNotificationPermissionResult(granted)

            if (granted) {
                findNavController().popBackStack()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showRequestUi()
                } else {
                    showOpenSettingsUi()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            showRequestUi()
        } else {
            showOpenSettingsUi()
        }

        binding.btnChangeEmail.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }

    private fun showRequestUi() {
        binding.cardIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_bg))
        binding.cardIcon.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_basic))
        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_notification_green)
        binding.tvEmail.text = "Разрешить уведомления"
        binding.tvEmailDescription.text = "Одно напоминание в день — чтобы не забыть про сессию. Никакого спама."
        binding.cardInstruction.visibility = View.GONE
        binding.btnConfirmed.text = "Разрешить"
        binding.btnChangeEmail.text = "Не сейчас"

        binding.btnConfirmed.setOnClickListener {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun showOpenSettingsUi() {
        binding.cardIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange_bg))
        binding.cardIcon.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.difficulty_orange))
        binding.ivBackgroundIcon.setImageResource(R.drawable.ic_notification_orange)
        binding.tvEmail.text = "Уведомления отключены"
        binding.tvEmailDescription.text = "Разрешение было отклонено. Включите вручную в настройках телефона."
        binding.cardInstruction.visibility = View.VISIBLE
        binding.btnConfirmed.text = "Открыть настройки"
        binding.btnChangeEmail.text = "Пропустить"

        binding.btnConfirmed.setOnClickListener {
            openAppSettings()
        }
    }
}