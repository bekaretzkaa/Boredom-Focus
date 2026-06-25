package com.example.boredomfocus.feature.focussession.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.databinding.FragmentStopwatchBinding
import com.example.boredomfocus.feature.focussession.FocusSessionEvent
import com.example.boredomfocus.feature.focussession.FocusSessionViewModel
import com.example.boredomfocus.feature.focussession.dialogs.StopFocusDialogFragment
import kotlinx.coroutines.launch

class StopwatchFragment : Fragment(R.layout.fragment_stopwatch) {
    private val viewModel: FocusSessionViewModel by hiltNavGraphViewModels(R.id.focusSessionGraph)

    private var _binding: FragmentStopwatchBinding? = null
    private val binding get() = _binding!!

    private var lastStage = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStopwatchBinding.bind(view)

//        binding.btnStopFocus.setOnClickListener {
//            viewModel.onStopFocusClick()
//        }

        observeUiState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val visualState = buildFocusVisualState(
                        focusSeconds = state.focusUiState.focusSeconds,
                        lastSessionSeconds = state.focusUiState.previousFocusSeconds,
                        weekRecordSeconds = state.focusUiState.weekFocusRecord,
                        monthRecordSeconds = state.focusUiState.monthFocusRecord,
                        allTimeRecordSeconds = state.focusUiState.focusRecord
                    )

                    renderFocusState(visualState)
                }
            }
        }
    }
    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.events.collect { event ->
                    when(event) {
                        is FocusSessionEvent.NavigateToStopFocusDialog -> {
                            findNavController().navigate(R.id.actionStopwatchFragmentToStopFocusDialogFragment)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private data class FocusVisualState(
        val stage: Int,
        val timeText: String,
        val timeColor: Int,
        val badgeText: String,
        val badgeColor: Int,
        val comparisonText: String?,
        val targetText: String,
        val targetTime: String,
        val targetTimeColor: Int,
        val progressPercent: Int,
        val progressColor: Int,
        val flash: FlashMessage?,
        val showZone: Boolean
    )

    private data class FlashMessage(
        val icon: String,
        val title: String,
        val subtitle: String
    )

    private fun buildFocusVisualState(
        focusSeconds: Long,
        lastSessionSeconds: Long,
        weekRecordSeconds: Long,
        monthRecordSeconds: Long,
        allTimeRecordSeconds: Long
    ): FocusVisualState {

        val green = requireContext().getColor(R.color.green_basic)
        val white = requireContext().getColor(R.color.white)
        val red = Color.parseColor("#E24B4A")

        return when {
            allTimeRecordSeconds > 0 && focusSeconds > allTimeRecordSeconds -> {
                FocusVisualState(
                    stage = 4,
                    timeText = focusSeconds.formatAsTimer(),
                    timeColor = green,
                    badgeText = "★ НОВЫЙ РЕКОРД",
                    badgeColor = white,
                    comparisonText = "+${(focusSeconds - allTimeRecordSeconds).formatAsTimer()} от абсолютного рекорда ↑",
                    targetText = "∞ зона некомфорта",
                    targetTime = "∞",
                    targetTimeColor = green,
                    progressPercent = 100,
                    progressColor = green,
                    flash = FlashMessage(
                        icon = "🏆",
                        title = "Новый рекорд всего времени!",
                        subtitle = "Ты в неизведанной территории — продолжай!"
                    ),
                    showZone = true
                )
            }

            monthRecordSeconds > 0 && focusSeconds > monthRecordSeconds -> {
                FocusVisualState(
                    stage = 3,
                    timeText = focusSeconds.formatAsTimer(),
                    timeColor = green,
                    badgeText = "✓ обогнал рекорд месяца",
                    badgeColor = green,
                    comparisonText = "+${(focusSeconds - monthRecordSeconds).formatAsTimer()} от рекорда месяца ↑",
                    targetText = "цель — абсолютный рекорд",
                    targetTime = allTimeRecordSeconds.formatAsTimer(),
                    targetTimeColor = red,
                    progressPercent = getProgressPercent(focusSeconds, allTimeRecordSeconds),
                    progressColor = red,
                    flash = FlashMessage(
                        icon = "💪",
                        title = "Лучший результат месяца!",
                        subtitle = "Остался один барьер — побей рекорд всего времени"
                    ),
                    showZone = false
                )
            }

            weekRecordSeconds > 0 && focusSeconds > weekRecordSeconds -> {
                FocusVisualState(
                    stage = 2,
                    timeText = focusSeconds.formatAsTimer(),
                    timeColor = green,
                    badgeText = "✓ обогнал рекорд недели",
                    badgeColor = green,
                    comparisonText = "+${(focusSeconds - weekRecordSeconds).formatAsTimer()} от рекорда недели ↑",
                    targetText = "цель — рекорд месяца",
                    targetTime = monthRecordSeconds.formatAsTimer(),
                    targetTimeColor = white,
                    progressPercent = getProgressPercent(focusSeconds, monthRecordSeconds),
                    progressColor = green,
                    flash = FlashMessage(
                        icon = "⚡",
                        title = "Лучший результат недели!",
                        subtitle = "Теперь цель — рекорд месяца"
                    ),
                    showZone = false
                )
            }

            lastSessionSeconds > 0 && focusSeconds > lastSessionSeconds -> {
                FocusVisualState(
                    stage = 1,
                    timeText = focusSeconds.formatAsTimer(),
                    timeColor = green,
                    badgeText = "✓ обогнал прошлую сессию",
                    badgeColor = green,
                    comparisonText = "+${(focusSeconds - lastSessionSeconds).formatAsTimer()} от прошлой сессии ↑",
                    targetText = "цель — рекорд недели",
                    targetTime = weekRecordSeconds.formatAsTimer(),
                    targetTimeColor = white,
                    progressPercent = getProgressPercent(focusSeconds, weekRecordSeconds),
                    progressColor = green,
                    flash = FlashMessage(
                        icon = "🔥",
                        title = "Лучше чем в прошлый раз!",
                        subtitle = "Теперь цель — рекорд недели"
                    ),
                    showZone = false
                )
            }

            else -> {
                FocusVisualState(
                    stage = 0,
                    timeText = focusSeconds.formatAsTimer(),
                    timeColor = white,
                    badgeText = "● фокус",
                    badgeColor = green,
                    comparisonText = null,
                    targetText = "цель — прошлая сессия",
                    targetTime = lastSessionSeconds.formatAsTimer(),
                    targetTimeColor = white,
                    progressPercent = getProgressPercent(focusSeconds, lastSessionSeconds),
                    progressColor = green,
                    flash = null,
                    showZone = false
                )
            }
        }
    }

    private fun getProgressPercent(
        currentSeconds: Long,
        targetSeconds: Long
    ): Int {
        if (targetSeconds <= 0L) return 0

        return ((currentSeconds.toFloat() / targetSeconds.toFloat()) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun Long.formatAsTimer(): String {
        val minutes = this / 60
        val seconds = this % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun renderFocusState(state: FocusVisualState) = with(binding) {
        tvStatusBadge.text = state.badgeText
        tvStatusBadge.setTextColor(state.badgeColor)

        tvFocusTime.text = state.timeText
        tvFocusTime.setTextColor(state.timeColor)

        tvFocusComparison.text = state.comparisonText.orEmpty()
        tvFocusComparison.visibility =
            if (state.comparisonText == null) View.INVISIBLE else View.VISIBLE

        tvFocusTarget.text = state.targetText
        tvFocusTargetTime.text = state.targetTime
        tvFocusTargetTime.setTextColor(state.targetTimeColor)

        progressFill.setBackgroundColor(state.progressColor)
        setProgressWidth(state.progressPercent)

        renderTargetChain(state.stage)

        zoneCard.visibility = if (state.showZone) View.VISIBLE else View.GONE

        if (state.stage > lastStage) {
            state.flash?.let {
                showFlash(it)
            }
        }

        lastStage = state.stage
    }

    private fun renderTargetChain(stage: Int) = with(binding) {
        val green = requireContext().getColor(R.color.green_basic)
        val white = requireContext().getColor(R.color.white)
        val gray = requireContext().getColor(R.color.gray_focus)

        val dots = listOf(dotSession, dotWeek, dotMonth, dotAllTime)
        val labels = listOf(tvSession, tvWeek, tvMonth, tvAllTime)
        val lines = listOf(lineSession, lineWeek, lineMonth)

        dots.forEachIndexed { index, dot ->
            when {
                index < stage -> {
                    dot.setBackgroundResource(R.drawable.circle_filled_green)
                    labels[index].setTextColor(green)
                }

                index == stage && stage < dots.size -> {
                    dot.setBackgroundResource(R.drawable.circle_empty)
                    labels[index].setTextColor(white)
                }

                else -> {
                    dot.setBackgroundResource(R.drawable.circle_filled_gray)
                    labels[index].setTextColor(gray)
                }
            }
        }

        lines.forEachIndexed { index, line ->
            if (index < stage) {
                line.setBackgroundColor(green)
            } else {
                line.setBackgroundColor(gray)
            }
        }
    }

    private fun showFlash(message: FlashMessage) = with(binding) {
        flashIcon.text = message.icon
        tvFlash1.text = message.title
        tvFlash2.text = message.subtitle

        flashCard.visibility = View.VISIBLE
        flashCard.alpha = 0f
        flashCard.translationY = -16f

        flashCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250L)
            .start()

        flashCard.postDelayed({
            flashCard.animate()
                .alpha(0f)
                .translationY(-16f)
                .setDuration(250L)
                .withEndAction {
                    flashCard.visibility = View.GONE
                }
                .start()
        }, 3500L)
    }

    private fun setProgressWidth(percent: Int) {
        val updateWidth = {
            val fullWidth = binding.progressContainer.width
            val newWidth = (fullWidth * percent / 100f).toInt()

            binding.progressFill.updateLayoutParams<FrameLayout.LayoutParams> {
                width = newWidth
            }
        }

        if (binding.progressContainer.width == 0) {
            binding.progressContainer.doOnLayout {
                updateWidth()
            }
        } else {
            updateWidth()
        }
    }

}