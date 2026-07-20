package com.example.boredomfocus.feature.focussession.fragments

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.daysWord
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentFocusResultBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import kotlinx.coroutines.launch
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.doOnPreDraw

class FocusResultFragment : Fragment(R.layout.fragment_focus_result) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentFocusResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFocusResultBinding.bind(view)

        binding.btnToHome.setOnClickListener {
            viewModel.onFocusResultHomeClick()
        }

        observeUiState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private var resultScreenHandled = false

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val grayBg = ContextCompat.getColor(requireContext(), R.color.gray_bg)
                    val greenBg = ContextCompat.getColor(requireContext(), R.color.green_bg)
                    val grayText = ContextCompat.getColor(requireContext(), R.color.gray_dark)
                    val greenText = ContextCompat.getColor(requireContext(), R.color.green_basic)
                    val white = ContextCompat.getColor(requireContext(), R.color.white)

                    binding.tvFocusTime.text = formatSeconds(state.focusUiState.focusSeconds)
                    val targets = listOf(
                        binding.cardSession,
                        binding.cardWeek,
                        binding.cardMonth,
                        binding.cardRecord
                    )
                    targets.forEach {
                        it.visibility = View.GONE
                    }
                    binding.llTargets.visibility = View.VISIBLE
                    val focusOnly = state.detoxUiState.detoxElapsedSeconds <= 0
                    if(!focusOnly) {
                        binding.cardDetox.visibility = View.VISIBLE
                        binding.tvDetoxTime.text = getString(R.string.focus_result_detox_completed, state.detoxUiState.selectedDetoxSeconds / 60)
                    } else {
                        binding.cardDetox.visibility = View.GONE
                    }
                    val streakCount = state.streakCount
                    binding.tvStreakCount.text = getString(R.string.focus_result_streak_days, streakCount, daysWord(streakCount))

                    binding.tvFocusComparisonWord.text = getString(R.string.focus_result_all_time_record)
                    binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.focusRecord ?: 0)

                    when(state.focusUiState.focusSeconds) {
                        in 0..(state.focusUiState.previousFocusSeconds ?: 0) -> {
                            binding.cardSessionComparison.setCardBackgroundColor(grayBg)
                            binding.tvSessionComparison.apply {
                                text = getString(R.string.focus_result_session_completed)
                                setTextColor(grayText)
                            }
                            binding.tvFocusTime.setTextColor(white)
                            binding.tvFocusComparison.apply {
                                text = getString(R.string.focus_result_less_than_last_session, formatSeconds((state.focusUiState.previousFocusSeconds ?: 0) - state.focusUiState.focusSeconds))
                                setTextColor(grayText)
                            }
                            binding.tvFocusComparisonWord.text = getString(R.string.focus_result_previous_session)
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.previousFocusSeconds ?: 0)

                            binding.llTargets.visibility = View.GONE
                        }
                        in (state.focusUiState.previousFocusSeconds ?: 0)..(state.focusUiState.weekFocusRecord ?: 0) -> {
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = getString(R.string.focus_result_better_than_last_session)
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = getString(R.string.focus_result_vs_last_session, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0)))
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.tvFocusComparisonWord.text = getString(R.string.focus_result_weekly_record)
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.weekFocusRecord ?: 0)
                        }
                        in (state.focusUiState.weekFocusRecord ?: 0)..(state.focusUiState.monthFocusRecord ?: 0) -> {
                            val weekNull = state.focusUiState.weekFocusRecord == null
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = if(weekNull) getString(R.string.focus_result_better_than_last_session) else getString(R.string.focus_result_week_record)
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = if(weekNull) getString(R.string.focus_result_vs_last_session, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0)))
                                else getString(R.string.focus_result_vs_week_record, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.weekFocusRecord ?: 0)))
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.tvFocusComparisonWord.text = if(weekNull) getString(R.string.focus_result_previous_session) else getString(R.string.focus_result_month_card)
                            binding.tvFocusComparisonTime.text = formatSeconds(state.focusUiState.monthFocusRecord ?: 0)
                        }
                        in (state.focusUiState.monthFocusRecord ?: 0)..(state.focusUiState.focusRecord) -> {
                            val monthNull = state.focusUiState.monthFocusRecord == null
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = if(monthNull) getString(R.string.focus_result_better_than_last_session) else getString(R.string.focus_result_month_record)
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = if(monthNull) getString(R.string.focus_result_vs_last_session, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0)))
                                else getString(R.string.focus_result_vs_month_record, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.monthFocusRecord ?: 0)))
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.cardMonth.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.cardSessionComparison.setCardBackgroundColor(greenBg)
                            binding.tvSessionComparison.apply {
                                text = getString(R.string.focus_result_new_all_time_record)
                                setTextColor(greenText)
                            }
                            binding.tvFocusTime.setTextColor(greenText)
                            binding.tvFocusComparison.apply {
                                text = getString(R.string.focus_result_vs_previous_record, formatSeconds(state.focusUiState.focusSeconds - (state.focusUiState.previousFocusSeconds ?: 0)))
                                setTextColor(greenText)
                            }
                            binding.cardSession.visibility = View.VISIBLE
                            binding.cardWeek.visibility = View.VISIBLE
                            binding.cardMonth.visibility = View.VISIBLE
                            binding.cardRecord.visibility = View.VISIBLE

                            binding.cardFocusTime.setCardBackgroundColor(greenBg)
                            binding.cardFocusTime.setStrokeColor(greenText)
                        }
                    }
                    if (!resultScreenHandled) {
                        resultScreenHandled = true

                        if (viewModel.shouldPlayResultAnimation()) {
                            playResultEnterAnimation(
                                focusSeconds = state.focusUiState.focusSeconds,
                                isNewAllTimeRecord = state.focusUiState.focusSeconds >= state.focusUiState.focusRecord
                            )
                        } else {
                            showResultWithoutAnimation(
                                focusSeconds = state.focusUiState.focusSeconds
                            )
                        }
                    }
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateHome -> {
                            findNavController().popBackStack(
                                R.id.homeFragment,
                                false
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun playResultEnterAnimation(
        focusSeconds: Long,
        isNewAllTimeRecord: Boolean
    ) = with(binding) {

        val views = listOf(
            tvFocusResult,
            cardSessionComparison,
            cardFocusTime,
            llDetoxFocus,
            cardStreak,
            btnToHome
        )

        views.forEach { view ->
            view.alpha = 0f
            view.translationY = 36f
        }

        llTargets.alpha = 1f
        llTargets.translationY = 0f

        cardFocusTime.scaleX = 0.96f
        cardFocusTime.scaleY = 0.96f

        val targetCards = listOf(cardSession, cardWeek, cardMonth, cardRecord)

        targetCards.forEach {
            it.alpha = 0f
            it.scaleX = 0.85f
            it.scaleY = 0.85f
            it.translationY = 24f
        }

        tvFocusTime.text = formatSeconds(0)

        root.doOnPreDraw {
            tvFocusResult.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardSessionComparison.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardFocusTime.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(240)
                .setDuration(450)
                .setInterpolator(OvershootInterpolator(1.1f))
                .withStartAction {
                    animateFocusTime(focusSeconds)
                }
                .start()

            targetCards.forEachIndexed { index, card ->
                card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(520L + index * 90L)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(1.4f))
                    .start()
            }

            llDetoxFocus.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(900)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardStreak.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(1020)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            btnToHome.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(1140)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator())
                .start()

            if (isNewAllTimeRecord) {
                confettiView.postDelayed({
                    confettiView.start()
                }, 850)
            }
        }
    }

    private fun animateFocusTime(finalSeconds: Long) {
        ValueAnimator.ofInt(0, finalSeconds.toInt()).apply {
            duration = 900
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->
                val seconds = animator.animatedValue as Int
                binding.tvFocusTime.text = formatSeconds(seconds.toLong())
            }

            start()
        }
    }

    private fun showResultWithoutAnimation(focusSeconds: Long) = with(binding) {
        val views = listOf(
            tvFocusResult,
            cardSessionComparison,
            cardFocusTime,
            llTargets,
            llDetoxFocus,
            cardStreak,
            btnToHome
        )

        views.forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        }

        val targetCards = listOf(
            cardSession,
            cardWeek,
            cardMonth,
            cardRecord
        )

        targetCards.forEach { card ->
            card.animate().cancel()
            card.alpha = 1f
            card.translationY = 0f
            card.scaleX = 1f
            card.scaleY = 1f
        }

        tvFocusTime.text = formatSeconds(focusSeconds)
        confettiView.stop()
    }
}