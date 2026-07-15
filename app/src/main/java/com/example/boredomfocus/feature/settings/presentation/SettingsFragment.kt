package com.example.boredomfocus.feature.settings.presentation

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.PermissionViewModel
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.ui.selector.AnimatedCardGroupSelector
import com.example.boredomfocus.core.ui.selector.AnimatedCardSelector
import com.example.boredomfocus.databinding.FragmentSettingsBinding
import com.example.boredomfocus.feature.settings.dialogs.SignOutDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.settingsGraph)
    private val permissionViewModel: PermissionViewModel by activityViewModels()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var difficultySelector: AnimatedCardSelector? = null
    private var durationSelector: AnimatedCardGroupSelector? = null
    private var isRenderingFromSettings = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionViewModel.onNotificationPermissionResult(granted)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        setupDurationSelector()
        setupDifficultySelector()
        observeSettings()
        observeProfile()
        setupSignOutResultListener()
        setupClicks()
        observePermissions()
    }

    private fun setupDurationSelector() {
        val durationCards = listOf(
            binding.cardTimeFive,
            binding.cardTimeSeven,
            binding.cardTimeTen,
            binding.cardTimeFifteen
        )

        durationSelector = AnimatedCardGroupSelector(
            context = requireContext(),
            cards = durationCards,
            selectedStrokeColorRes = R.color.white,
            unselectedStrokeColorRes = R.color.gray_border,
            selectedScale = 1.04f,
            duration = 120L,
            onSelected = { selectedIndex ->

                if(isRenderingFromSettings) return@AnimatedCardGroupSelector

                val duration = when(selectedIndex) {
                    0 -> DetoxDuration.FIVE_MINUTES
                    1 -> DetoxDuration.SEVEN_MINUTES
                    2 -> DetoxDuration.TEN_MINUTES
                    3 -> DetoxDuration.FIFTEEN_MINUTES
                    else -> DetoxDuration.FIVE_MINUTES
                }

                viewModel.saveDetoxDuration(duration)
            }
        )

        durationCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                durationSelector?.select(index)
            }
        }
    }

    private fun setupDifficultySelector() {
        difficultySelector = AnimatedCardSelector(
            context = requireContext(),
            unselectedStrokeColorRes = R.color.card_unselected,
            items = listOf(
                AnimatedCardSelector.Item(
                    card = binding.cardDifficultyBeginner,
                    title = binding.tvDifficultyBeginner,
                    selectedColorRes = R.color.green_basic,
                    selectedBackgroundColorRes = R.color.green_bg
                ),
                AnimatedCardSelector.Item(
                    card = binding.cardDifficultyFighter,
                    title = binding.tvDifficultyFighter,
                    selectedColorRes = R.color.difficulty_orange,
                    selectedBackgroundColorRes = R.color.difficulty_orange_bg
                ),
                AnimatedCardSelector.Item(
                    card = binding.cardDifficultyHardcore,
                    title = binding.tvDifficultyHardcore,
                    selectedColorRes = R.color.red_basic,
                    selectedBackgroundColorRes = R.color.difficulty_red_bg
                )
            ),
            onSelected = { selectedIndex ->

                if(isRenderingFromSettings) return@AnimatedCardSelector

                val difficulty = when(selectedIndex) {
                    0 -> Difficulty.BEGINNER
                    1 -> Difficulty.FIGHTER
                    2 -> Difficulty.HARDCORE
                    else -> Difficulty.BEGINNER
                }

                viewModel.saveDifficulty(difficulty)
            }
        )

        binding.cardDifficultyBeginner.setOnClickListener {
            difficultySelector?.select(0)
        }
        binding.cardDifficultyFighter.setOnClickListener {
            difficultySelector?.select(1)
        }
        binding.cardDifficultyHardcore.setOnClickListener {
            difficultySelector?.select(2)
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if(state.isLoading) {
                        binding.settingsContent.alpha = 0f
                        return@collect
                    }

                    binding.settingsContent.alpha = 1f

                    renderSettings(state)
                }
            }
        }
    }

    private fun observePermissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionViewModel.uiState.collect { state ->
                    binding.switchDnd.setChecked(state.doNotDisturb)
                    binding.switchNotification.setChecked(state.postNotifications)
                }
            }
        }
    }
    private fun setupClicks() {
        binding.switchDnd.setOnClickListener {
            if (!permissionViewModel.uiState.value.doNotDisturb) {
                findNavController().navigate(R.id.actionSettingsFragmentToDndPermission)
            }
        }

        binding.switchNotification.setOnClickListener {
            if (!permissionViewModel.uiState.value.postNotifications) {
                findNavController().navigate(R.id.actionSettingsFragmentToNotificationPermission)
            }
        }
    }

    private fun renderSettings(state: SettingsUiState) {
        isRenderingFromSettings = true

        try {
            durationSelector?.select(
                when(state.detoxDuration) {
                    DetoxDuration.FIVE_MINUTES -> 0
                    DetoxDuration.SEVEN_MINUTES -> 1
                    DetoxDuration.TEN_MINUTES -> 2
                    DetoxDuration.FIFTEEN_MINUTES -> 3
                }
            )

            difficultySelector?.select(
                when(state.difficulty) {
                    Difficulty.BEGINNER -> 0
                    Difficulty.FIGHTER -> 1
                    Difficulty.HARDCORE -> 2
                }
            )
        } finally {
            isRenderingFromSettings = false
        }
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if(state.isSignedIn) {
                        if(state.name.isNotBlank()) {
                            binding.ivPersonIcon.visibility = View.GONE
                            binding.tvPersonIcon.visibility = View.VISIBLE
                            binding.tvPersonIcon.text = state.name.first().toString()
                        } else {
                            binding.ivPersonIcon.visibility = View.VISIBLE
                            binding.tvPersonIcon.visibility = View.GONE
                        }

                        binding.tvProfile1.text = state.name
                        binding.tvProfile2.text = state.email

                        binding.btnSignIn.text = "Выйти из аккаунта"
                        binding.btnSignIn.setOnClickListener {
                            SignOutDialogFragment().show(
                                parentFragmentManager,
                                "SignOutDialogFragment"
                            )
                        }
                    } else {
                        binding.ivPersonIcon.visibility = View.VISIBLE
                        binding.tvPersonIcon.visibility = View.GONE

                        binding.tvProfile1.text = "Гость"
                        binding.tvProfile2.text = "Прогресс хранится\\nтолько на устройстве"

                        binding.btnSignIn.text = "Войти"
                        binding.btnSignIn.setOnClickListener {
                            findNavController().navigate(R.id.actionSettingsFragmentToAuthGraph)
                        }
                    }
                }
            }
        }
    }

    private fun setupSignOutResultListener() {
        parentFragmentManager.setFragmentResultListener(
            SignOutDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->

            val isConfirmed = result.getBoolean(SignOutDialogFragment.RESULT_CONFIRMED)

            if(isConfirmed) {
                viewModel.signOut()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        permissionViewModel.refreshPermissions()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            permissionViewModel.refreshPermissions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        difficultySelector = null
        durationSelector = null
        _binding = null
    }
}