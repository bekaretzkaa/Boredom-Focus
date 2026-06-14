package com.example.boredomfocus.feature.onboarding.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.boredomfocus.R
import com.example.boredomfocus.core.ui.selector.AnimatedCardGroupSelector
import com.example.boredomfocus.core.ui.selector.AnimatedCardSelector
import com.example.boredomfocus.databinding.OnboardingFragmentThirdBinding

class OnboardingFragmentThird : Fragment(R.layout.onboarding_fragment_third) {

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
            requireActivity().finish()
        }

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
                selectedDuration = when (selectedIndex) {
                    0 -> 5
                    1 -> 7
                    2 -> 10
                    3 -> 15
                    else -> 5
                }
            }
        )

        durationCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                durationSelector?.select(index)
            }
        }

        durationSelector?.select(0)

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
                selectedDifficulty = when (selectedIndex) {
                    0 -> "beginner"
                    1 -> "fighter"
                    2 -> "hardcore"
                    else -> "beginner"
                }
            }
        )
        difficultySelector?.select(0)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        difficultySelector = null
        durationSelector = null
    }
}