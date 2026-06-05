package com.example.boredomfocus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.boredomfocus.databinding.ItemPermissionBinding
import com.example.boredomfocus.databinding.OnboardingFragmentSecondBinding

class OnboardingFragmentSecond : Fragment(R.layout.onboarding_fragment_second) {

    private var _binding: OnboardingFragmentSecondBinding? = null
    private val binding get() = _binding!!

    private var permissionsList = listOf(
        PermissionItem("dnd", "Mode «Do not disturb»", "Blocks notifications during your detox. Otherwise, it defeats the purpose.", R.drawable.ic_notification_off, R.drawable.bg_ic_notification_off, false),
        PermissionItem("notifications", "Notifications", "Daily session reminder. Just one, promise.", R.drawable.ic_notification, R.drawable.bg_ic_notification, false)
    )

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
        renderPermissions()
    }

    private fun renderPermissions() {
        binding.llOnboarding2Container.removeAllViews()

        permissionsList.forEach { permission ->
            val itemBinding = ItemPermissionBinding.inflate(layoutInflater, binding.llOnboarding2Container, false)

            itemBinding.tvPermissionText1.text = permission.title
            itemBinding.tvPermissionText2.text = permission.description
            itemBinding.ivPermissionIcon.setImageResource(permission.iconRes)
            itemBinding.flPermissionIconContainer.setBackgroundResource(permission.iconBackground)

            binding.llOnboarding2Container.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}