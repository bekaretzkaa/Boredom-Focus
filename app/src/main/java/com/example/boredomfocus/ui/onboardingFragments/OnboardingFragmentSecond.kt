package com.example.boredomfocus.ui.onboardingFragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.boredomfocus.PermissionItem
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.ItemPermissionBinding
import com.example.boredomfocus.databinding.OnboardingFragmentSecondBinding
import com.example.boredomfocus.permission.PermissionStatus
import com.example.boredomfocus.permission.PermissionViewModel
import com.example.boredomfocus.viewmodel.OnboardingViewModel

class OnboardingFragmentSecond : Fragment(R.layout.onboarding_fragment_second) {

    private val viewModel: OnboardingViewModel by activityViewModels()
    private val permissionViewModel: PermissionViewModel by activityViewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionViewModel.refreshPermissions()
        if(isGranted) {
            checkAndRequestDnd()
        }
    }

    private var _binding: OnboardingFragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OnboardingFragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionViewModel.permissionStatus.observe(viewLifecycleOwner) { status ->
            renderPermissions(status)
        }
        permissionViewModel.refreshPermissions()

        binding.btnOnboarding2Allow.setOnClickListener {
            requestAllPermissions()
        }

        binding.btnOnboarding2NotNow.setOnClickListener {
            viewModel.onNavigateToPageRequested(2)
        }
    }

    private fun renderPermissions(status: PermissionStatus) {
        val permissionsList = listOf(
            PermissionItem(
                "dnd",
                "Режим «Не беспокоить»",
                "Блокирует уведомления во время детокса. Без этого смысл пропадает.",
                R.drawable.ic_notification_off,
                R.drawable.bg_ic_notification_off,
                status.doNotDisturb
            ),
            PermissionItem(
                "notifications",
                "Уведомления",
                "Ежедневное напоминание о сессии. Только одно, обещаем.",
                R.drawable.ic_notification,
                R.drawable.bg_ic_notification,
                status.postNotifications
            )
        )

        binding.llOnboarding2Container.removeAllViews()

        permissionsList
            .filter { !it.isGranted}
            .forEach { permission ->
            val itemBinding = ItemPermissionBinding.inflate(layoutInflater, binding.llOnboarding2Container, false)

            itemBinding.tvPermissionText1.text = permission.title
            itemBinding.tvPermissionText2.text = permission.description
            itemBinding.ivPermissionIcon.setImageResource(permission.iconRes)
            itemBinding.flPermissionIconContainer.setBackgroundResource(permission.iconBackground)

            binding.llOnboarding2Container.addView(itemBinding.root)
        }
    }

    private fun requestAllPermissions() {
        if(permissionViewModel.requiresRuntimeNotificationPermission()) {
            if(!permissionViewModel.isPostNotificationsGranted()) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
        }
        checkAndRequestDnd()
    }
    private fun checkAndRequestDnd() {
        if(!permissionViewModel.isDoNotDisturbGranted()) {
            startActivity(permissionViewModel.getDndSettingsIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        permissionViewModel.refreshPermissions()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}