package com.example.boredomfocus.feature.sessionsettings.presentation

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.boredomfocus.R
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.ui.selector.AnimatedCardGroupSelector
import com.example.boredomfocus.core.ui.selector.AnimatedCardSelector
import com.example.boredomfocus.databinding.BottomSheetSessionSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class SessionSettingsBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: SessionSettingsViewModel by viewModels()
    private var _binding: BottomSheetSessionSettingsBinding? = null
    private val binding get() = _binding!!

    private var difficultySelector: AnimatedCardSelector? = null
    private var durationSelector: AnimatedCardGroupSelector? = null
    private var isRenderingFromSettings = false

    private var lastRenderedFocusOnly: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSessionSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getTheme(): Int {
        return R.style.Theme_BoredomFocus_BottomSheet
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val window = dialog.window ?: return

        window.navigationBarColor = "#0D0D0D".toColorInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
        }

        window.setDimAmount(0.45f)

        val bottomSheet = dialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        bottomSheet.requestLayout()

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isDraggable = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDurationSelector()
        setupDifficultySelector()
        setupFocusOnlyToggle()
        setupButtons()
        observeUiState()

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if(state.isLoading) {
                        binding.sessionSettings.alpha = 0f
                        return@collect
                    }

                    binding.sessionSettings.alpha = 1f

                    renderSettings(state)
                }
            }
        }
    }

    private fun renderSettings(state: SessionSettingsUiState) {
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

        renderFocusOnly(state.focusOnly)
    }

    private fun renderFocusOnly(focusOnly: Boolean) {
        val previousFocusOnly = lastRenderedFocusOnly
        lastRenderedFocusOnly = focusOnly

        val shouldAnimate = previousFocusOnly != null && previousFocusOnly != focusOnly

        binding.btnStart.text = if (focusOnly) {
            "Начать фокус"
        } else {
            "Начать детокс"
        }

        setDurationEnabled(!focusOnly)

        if (shouldAnimate) {
            animateFocusOnlyState(focusOnly)
        } else {
            applyFocusOnlyStateImmediately(focusOnly)
        }
    }

    private fun applyFocusOnlyStateImmediately(focusOnly: Boolean) {
        val green = "#639922".toColorInt()
        val normalStroke = "#1A1A1A".toColorInt()

        binding.cardFocusOnly.strokeColor = if (focusOnly) green else normalStroke
        binding.tvFocusOnly1.setTextColor(if (focusOnly) green else "#888888".toColorInt())

        binding.flCircleGreen.visibility = if (focusOnly) View.VISIBLE else View.GONE
        binding.viewCircle.visibility = if (focusOnly) View.GONE else View.VISIBLE

        val alpha = if (focusOnly) 0.35f else 1f

        getDetoxSettingsViews().forEach {
            it.alpha = alpha
        }
    }

    private fun animateFocusOnlyState(focusOnly: Boolean) {
        val green = "#639922".toColorInt()
        val grayText = "#888888".toColorInt()
        val normalStroke = "#1A1A1A".toColorInt()

        val targetStrokeColor = if (focusOnly) green else normalStroke
        val targetTextColor = if (focusOnly) green else grayText
        val targetAlpha = if (focusOnly) 0.35f else 1f

        animateCardStrokeColor(binding.cardFocusOnly, targetStrokeColor)
        animateTextColor(binding.tvFocusOnly1, targetTextColor)

        getDetoxSettingsViews().forEach { view ->
            view.animate()
                .alpha(targetAlpha)
                .setDuration(160L)
                .start()
        }

        animateFocusOnlyCircle(focusOnly)

        binding.cardFocusOnly.animate()
            .scaleX(1.02f)
            .scaleY(1.02f)
            .setDuration(80L)
            .withEndAction {
                binding.cardFocusOnly.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80L)
                    .start()
            }
            .start()
    }

    private fun getDetoxSettingsViews(): List<View> {
        return listOf(
            binding.tvDetox,
            binding.llDetoxTimeButtons
        )
    }

    private fun animateFocusOnlyCircle(focusOnly: Boolean) {
        if (focusOnly) {
            binding.flCircleGreen.alpha = 0f
            binding.flCircleGreen.visibility = View.VISIBLE

            binding.viewCircle.animate()
                .alpha(0f)
                .setDuration(120L)
                .withEndAction {
                    binding.viewCircle.visibility = View.GONE
                    binding.viewCircle.alpha = 1f
                }
                .start()

            binding.flCircleGreen.animate()
                .alpha(1f)
                .setDuration(160L)
                .start()
        } else {
            binding.viewCircle.alpha = 0f
            binding.viewCircle.visibility = View.VISIBLE

            binding.flCircleGreen.animate()
                .alpha(0f)
                .setDuration(120L)
                .withEndAction {
                    binding.flCircleGreen.visibility = View.GONE
                    binding.flCircleGreen.alpha = 1f
                }
                .start()

            binding.viewCircle.animate()
                .alpha(1f)
                .setDuration(160L)
                .start()
        }
    }

    private fun animateCardStrokeColor(
        card: MaterialCardView,
        targetColor: Int
    ) {
        ValueAnimator.ofArgb(card.strokeColor, targetColor).apply {
            duration = 160L
            addUpdateListener { animator ->
                card.strokeColor = animator.animatedValue as Int
            }
            start()
        }
    }

    private fun animateTextColor(
        textView: TextView,
        targetColor: Int
    ) {
        ValueAnimator.ofArgb(textView.currentTextColor, targetColor).apply {
            duration = 160L
            addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun setDurationEnabled(enabled: Boolean) {
        listOf(
            binding.cardTimeFive,
            binding.cardTimeSeven,
            binding.cardTimeTen,
            binding.cardTimeFifteen
        ).forEach {
            it.isEnabled = enabled
            it.isClickable = enabled
        }
    }

    private fun setupFocusOnlyToggle() {
        binding.cardFocusOnly.setOnClickListener {
            viewModel.toggleFocusOnly()
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnStart.setOnClickListener {
            val state = viewModel.uiState.value

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_DETOX_DURATION to state.detoxDuration,
                    KEY_DIFFICULTY to state.difficulty,
                    KEY_FOCUS_ONLY to state.focusOnly
                )
            )

            dismiss()
        }
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

                viewModel.selectDetoxDuration(duration)
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

                val difficulty = when (selectedIndex) {
                    0 -> Difficulty.BEGINNER
                    1 -> Difficulty.FIGHTER
                    2 -> Difficulty.HARDCORE
                    else -> Difficulty.BEGINNER
                }

                viewModel.selectDifficulty(difficulty)
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

    companion object {
        const val REQUEST_KEY = "sessionSettingsFragment"

        const val KEY_DETOX_DURATION = "detoxDuration"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_FOCUS_ONLY = "focusOnly"

        const val TAG = "SessionSettingsBottomSheet"
    }

}