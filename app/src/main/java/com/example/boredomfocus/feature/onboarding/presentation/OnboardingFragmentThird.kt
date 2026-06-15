package com.example.boredomfocus.feature.onboarding.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.boredomfocus.R
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.ui.selector.AnimatedCardGroupSelector
import com.example.boredomfocus.core.ui.selector.AnimatedCardSelector
import com.example.boredomfocus.databinding.OnboardingFragmentThirdBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragmentThird : Fragment(R.layout.onboarding_fragment_third) {

    private val viewModel: OnboardingViewModel by viewModels()

    private var _binding: OnboardingFragmentThirdBinding? = null
    private val binding get() = _binding!!

    private var difficultySelector: AnimatedCardSelector? = null
    private var selectedDifficulty = "beginner"
    private var durationSelector: AnimatedCardGroupSelector? = null
    private var selectedDuration = 5

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = OnboardingFragmentThirdBinding.bind(view)

        binding.btnOnboarding3Start.setOnClickListener {
//            viewModel.completeOnboarding()
            requireActivity().finish()
        }

        setupDurationSelector()
        setupDifficultySelector()
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

        durationSelector?.select(0)
    }

    private fun setupDifficultySelector() {
        difficultySelector = AnimatedCardSelector(
            context = requireContext(),
            unselectedStrokeColorRes = R.color.card_unselected,
            items = listOf(
                AnimatedCardSelector.Item(
                    card = binding.cardDifficultyBeginner,
                    title = binding.tvDifficultyBeginner,
                    selectedColorRes = R.color.difficulty_green,
                    selectedBackgroundColorRes = R.color.difficulty_green_bg
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
                    selectedColorRes = R.color.difficulty_red,
                    selectedBackgroundColorRes = R.color.difficulty_red_bg
                )
            ),
            onSelected = { selectedIndex ->
                val difficulty = when (selectedIndex) {
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

        difficultySelector?.select(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        difficultySelector = null
        durationSelector = null
    }
}