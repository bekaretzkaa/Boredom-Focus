package com.example.boredomfocus.feature.focussession.fragments

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
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

    private var hasRenderedOnce = false
    private var celebratedDoneTypes = emptySet<MilestoneType>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStopwatchBinding.bind(view)

        binding.btnStopFocus.setOnClickListener {
            viewModel.onStopFocusClick()
        }

        observeUiState()
        observeEvents()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null

        flashHideRunnable?.let {
            _binding?.flashCard?.removeCallbacks(it)
        }
        flashHideRunnable = null

        _binding?.flashCard?.animate()?.cancel()
        _binding?.flashCard?.visibility = View.GONE

        _binding?.confettiView?.stop()

        super.onDestroyView()
        _binding = null
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
//                    val visualState = buildFocusVisualState(
//                        focusSeconds = state.focusUiState.focusSeconds,
//                        lastSessionSeconds = state.focusUiState.previousFocusSeconds,
//                        weekRecordSeconds = state.focusUiState.weekFocusRecord,
//                        monthRecordSeconds = state.focusUiState.monthFocusRecord,
//                        allTimeRecordSeconds = state.focusUiState.focusRecord.takeIf { it > 0L }
//                    )

//                    FOR TESTING
                    val visualState = buildFocusVisualState(
                        focusSeconds = state.focusUiState.focusSeconds,
                        lastSessionSeconds = 20,
                        weekRecordSeconds = null,
                        monthRecordSeconds = 25,
                        allTimeRecordSeconds = 30
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

    private enum class MilestoneType(
        val targetName: String,
        val passedName: String,
        val comparisonName: String,
        val priority: Int
    ) {
        LAST_SESSION(
            targetName = "прошлая сессия",
            passedName = "прошлую сессию",
            comparisonName = "прошлой сессии",
            priority = 1
        ),

        WEEK(
            targetName = "рекорд недели",
            passedName = "рекорд недели",
            comparisonName = "рекорда недели",
            priority = 2
        ),

        MONTH(
            targetName = "рекорд месяца",
            passedName = "рекорд месяца",
            comparisonName = "рекорда месяца",
            priority = 3
        ),

        ALL_TIME(
            targetName = "абсолютный рекорд",
            passedName = "абсолютный рекорд",
            comparisonName = "абсолютного рекорда",
            priority = 4
        )
    }

    private enum class MilestoneStatus {
        MISSING,
        FUTURE,
        ACTIVE,
        DONE
    }

    private data class MilestoneUiState(
        val type: MilestoneType,
        val targetSeconds: Long?,
        val status: MilestoneStatus
    )

    private data class FocusVisualState(
        val timeText: String,
        val timeColor: Int,

        val badgeText: String,
        val badgeColor: Int,

        val comparisonText: String?,

        val targetText: String,
        val targetTime: String,
        val targetTimeColor: Int,

        val progressPercent: Float,
        val progressColor: Int,

        val milestones: List<MilestoneUiState>,
        val doneTypes: Set<MilestoneType>,

        val showZone: Boolean,

        val explanationText: String?
    )

    private data class FlashMessage(
        val icon: String,
        val title: String,
        val subtitle: String
    )

    private fun buildFocusVisualState(
        focusSeconds: Long,
        lastSessionSeconds: Long?,
        weekRecordSeconds: Long?,
        monthRecordSeconds: Long?,
        allTimeRecordSeconds: Long?
    ): FocusVisualState {

        val green = requireContext().getColor(R.color.green_basic)
        val white = requireContext().getColor(R.color.white)
        val red = Color.parseColor("#E24B4A")

        val rawMilestones = listOf(
            MilestoneType.LAST_SESSION to lastSessionSeconds.positiveOrNull(),
            MilestoneType.WEEK to weekRecordSeconds.positiveOrNull(),
            MilestoneType.MONTH to monthRecordSeconds.positiveOrNull(),
            MilestoneType.ALL_TIME to allTimeRecordSeconds.positiveOrNull()
        )

        val availableMilestones = rawMilestones
            .filter { (_, seconds) -> seconds != null }

        val activeTargetSeconds = availableMilestones
            .mapNotNull { it.second }
            .filter { targetSeconds ->
                focusSeconds <= targetSeconds
            }
            .minOrNull()

        val milestones = rawMilestones.map { (type, targetSeconds) ->
            val status = when {
                targetSeconds == null -> {
                    MilestoneStatus.MISSING
                }

                focusSeconds > targetSeconds -> {
                    MilestoneStatus.DONE
                }

                activeTargetSeconds != null && targetSeconds == activeTargetSeconds -> {
                    MilestoneStatus.ACTIVE
                }

                else -> {
                    MilestoneStatus.FUTURE
                }
            }

            MilestoneUiState(
                type = type,
                targetSeconds = targetSeconds,
                status = status
            )
        }

        val explanationText = buildExplanationText(
            lastSessionSeconds = lastSessionSeconds,
            weekRecordSeconds = weekRecordSeconds,
            monthRecordSeconds = monthRecordSeconds,
            allTimeRecordSeconds = allTimeRecordSeconds
        )

        val doneMilestones = milestones.filter {
            it.status == MilestoneStatus.DONE
        }

        val doneTypes = doneMilestones
            .map { it.type }
            .toSet()

        val activeMilestones = milestones.filter {
            it.status == MilestoneStatus.ACTIVE
        }

        val allTargetsMissing = availableMilestones.isEmpty()

        val allAvailableTargetsDone =
            availableMilestones.isNotEmpty() &&
                    milestones
                        .filter { it.targetSeconds != null }
                        .all { it.status == MilestoneStatus.DONE }

        val strongestDoneGroup = doneMilestones
            .groupBy { it.targetSeconds }
            .maxByOrNull { entry ->
                entry.key ?: 0L
            }
            ?.value
            .orEmpty()

        val strongestDoneTypes = strongestDoneGroup.map { it.type }

        val badgeText = when {
            allTargetsMissing -> {
                "● первый фокус"
            }

            MilestoneType.ALL_TIME in doneTypes -> {
                "★ НОВЫЙ РЕКОРД"
            }

            strongestDoneTypes.isNotEmpty() -> {
                val strongest = strongestDoneTypes.strongest()

                when (strongest) {
                    MilestoneType.LAST_SESSION -> "✓ лучше прошлой сессии"
                    MilestoneType.WEEK -> "✓ рекорд недели"
                    MilestoneType.MONTH -> "✓ рекорд месяца"
                    MilestoneType.ALL_TIME -> "★ НОВЫЙ РЕКОРД"
                    null -> "✓ цель пройдена"
                }
            }

            else -> {
                "● фокус"
            }
        }

        val badgeColor = when {
            MilestoneType.ALL_TIME in doneTypes -> white
            else -> green
        }

        val comparisonText = strongestDoneGroup.firstOrNull()?.targetSeconds?.let { targetSeconds ->
            val diff = focusSeconds - targetSeconds
            "+${diff.formatAsTimer()} от ${strongestDoneTypes.toComparisonText()} ↑"
        }

        val targetText: String
        val targetTime: String
        val targetTimeColor: Int
        val progressPercent: Float
        val progressColor: Int
        val showZone: Boolean

        when {
            allTargetsMissing -> {
                targetText = "первая фокус-сессия"
                targetTime = "—"
                targetTimeColor = white
                progressPercent = 0f
                progressColor = green
                showZone = false
            }

            activeMilestones.isNotEmpty() -> {
                val targetSeconds = activeMilestones.first().targetSeconds ?: 0L
                val activeTypes = activeMilestones.map { it.type }

                targetText = "цель — ${activeTypes.toTargetText()}"
                targetTime = targetSeconds.formatAsTimer()

                val activeContainsAllTime = activeTypes.contains(MilestoneType.ALL_TIME)

                targetTimeColor = if (activeContainsAllTime) red else white
                progressColor = if (activeContainsAllTime) red else green

                progressPercent = getProgressPercent(
                    currentSeconds = focusSeconds,
                    targetSeconds = targetSeconds
                )

                showZone = false
            }

            allAvailableTargetsDone -> {
                targetText = "∞ зона некомфорта"
                targetTime = "∞"
                targetTimeColor = green
                progressPercent = 100f
                progressColor = green
                showZone = true
            }

            else -> {
                targetText = "фокус"
                targetTime = "—"
                targetTimeColor = white
                progressPercent = 0f
                progressColor = green
                showZone = false
            }
        }

        return FocusVisualState(
            timeText = focusSeconds.formatAsTimer(),
            timeColor = if (doneTypes.isNotEmpty()) green else white,

            badgeText = badgeText,
            badgeColor = badgeColor,

            comparisonText = comparisonText,

            targetText = targetText,
            targetTime = targetTime,
            targetTimeColor = targetTimeColor,

            progressPercent = progressPercent,
            progressColor = progressColor,

            milestones = milestones,
            doneTypes = doneTypes,

            showZone = showZone,

            explanationText = explanationText
        )
    }

    private fun buildExplanationText(
        lastSessionSeconds: Long?,
        weekRecordSeconds: Long?,
        monthRecordSeconds: Long?,
        allTimeRecordSeconds: Long?
    ): String? {
        val noLastSession = lastSessionSeconds == null
        val noWeek = weekRecordSeconds == null
        val noMonth = monthRecordSeconds == null
        val noAllTime = allTimeRecordSeconds == null

        if (noLastSession && noWeek && noMonth && noAllTime) {
            return "Первая фокус-сессия — рекорды появятся после завершения"
        }

        return when {
            noWeek && noMonth -> {
                "Первая сессия недели и месяца — рекорды появятся после завершения"
            }

            noWeek -> {
                "Первая сессия недели — недельный рекорд появится после завершения"
            }

            noMonth -> {
                "Первая сессия месяца — месячный рекорд появится после завершения"
            }

            else -> null
        }
    }


    private fun Long?.positiveOrNull(): Long? {
        return this?.takeIf { it > 0L }
    }

    private fun getProgressPercent(
        currentSeconds: Long,
        targetSeconds: Long
    ): Float {
        if (targetSeconds <= 0L) return 0f

        return ((currentSeconds.toFloat() / targetSeconds.toFloat()) * 100f)
            .coerceIn(0f, 100f)
    }

    private fun Long.formatAsTimer(): String {
        val minutes = this / 60
        val seconds = this % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun List<MilestoneType>.strongest(): MilestoneType? {
        return this.maxByOrNull { it.priority }
    }

    private fun List<MilestoneType>.toTargetText(): String {
        val strongest = strongest() ?: return "фокус"

        return strongest.targetName
    }

    private fun List<MilestoneType>.toPassedText(): String {
        val strongest = strongest() ?: return "цель"

        return strongest.passedName
    }

    private fun List<MilestoneType>.toComparisonText(): String {
        val strongest = strongest() ?: return "цели"

        return strongest.comparisonName
    }

    private fun renderFocusState(state: FocusVisualState) = with(binding) {
        tvStatusBadge.text = state.badgeText
        tvStatusBadge.setTextColor(state.badgeColor)

        tvFocusTime.text = state.timeText
        tvFocusTime.setTextColor(state.timeColor)

        tvFocusComparison.text = state.comparisonText.orEmpty()
        tvFocusComparison.visibility =
            if (state.comparisonText == null) View.INVISIBLE else View.VISIBLE

        renderTargetChain(state.milestones)

        if (state.explanationText == null) {
            explanationCard.visibility = View.GONE
        } else {
            explanationCard.visibility = View.VISIBLE
            tvExplanation.text = state.explanationText
        }

        tvFocusTarget.text = state.targetText
        tvFocusTargetTime.text = state.targetTime
        tvFocusTargetTime.setTextColor(state.targetTimeColor)

        progressFill.setBackgroundColor(state.progressColor)
        setSmoothProgress(state.progressPercent)

        zoneCard.visibility = if (state.showZone) View.VISIBLE else View.GONE

        handleCelebrations(state)
    }

    private fun getFlashFor(types: Set<MilestoneType>): FlashMessage {
        val strongest = types.maxBy { it.priority }

        return when (strongest) {
            MilestoneType.LAST_SESSION -> {
                FlashMessage(
                    icon = "🔥",
                    title = "Лучше чем в прошлый раз!",
                    subtitle = "Теперь цель — следующий рекорд"
                )
            }

            MilestoneType.WEEK -> {
                FlashMessage(
                    icon = "⚡",
                    title = "Лучший результат недели!",
                    subtitle = "Теперь цель — рекорд месяца"
                )
            }

            MilestoneType.MONTH -> {
                FlashMessage(
                    icon = "💪",
                    title = "Лучший результат месяца!",
                    subtitle = "Остался один барьер — побей рекорд всего времени"
                )
            }

            MilestoneType.ALL_TIME -> {
                FlashMessage(
                    icon = "🏆",
                    title = "Новый рекорд всего времени!",
                    subtitle = "Ты в неизведанной территории — продолжай!"
                )
            }
        }
    }

    private fun handleCelebrations(state: FocusVisualState) {
        if (!hasRenderedOnce) {
            celebratedDoneTypes = state.doneTypes
            hasRenderedOnce = true
            return
        }

        val newlyDoneTypes = state.doneTypes - celebratedDoneTypes

        if (newlyDoneTypes.isEmpty()) return

        val flashMessage = getFlashFor(newlyDoneTypes)

        showFlash(flashMessage)

        if (MilestoneType.ALL_TIME in newlyDoneTypes) {
            binding.confettiView.start()
        }

        celebratedDoneTypes = celebratedDoneTypes + newlyDoneTypes
    }
    private fun renderTargetChain(milestones: List<MilestoneUiState>) = with(binding) {
        val green = requireContext().getColor(R.color.green_basic)
        val white = requireContext().getColor(R.color.white)
        val gray = requireContext().getColor(R.color.gray_focus)

        val dots = listOf(
            dotSession,
            dotWeek,
            dotMonth,
            dotAllTime
        )

        val labels = listOf(
            tvSession,
            tvWeek,
            tvMonth,
            tvAllTime
        )

        val lines = listOf(
            lineSession,
            lineWeek,
            lineMonth
        )

        val firstActiveIndex = milestones.indexOfFirst {
            it.status == MilestoneStatus.ACTIVE
        }

        val lastDoneIndex = milestones.indexOfLast {
            it.status == MilestoneStatus.DONE
        }

        fun shouldMissingLookDone(index: Int): Boolean {
            if (milestones[index].status != MilestoneStatus.MISSING) return false
            if (lastDoneIndex == -1) return false

            return if (firstActiveIndex != -1) {
                index < firstActiveIndex
            } else {
                index < lastDoneIndex
            }
        }

        fun isVisuallyDone(index: Int): Boolean {
            return milestones[index].status == MilestoneStatus.DONE ||
                    shouldMissingLookDone(index)
        }

        fun isVisuallyActive(index: Int): Boolean {
            return milestones[index].status == MilestoneStatus.ACTIVE
        }

        dots.forEachIndexed { index, dot ->
            val label = labels[index]
            val milestone = milestones[index]

            dot.alpha = 1f
            label.alpha = 1f

            when {
                isVisuallyDone(index) -> {
                    dot.setBackgroundResource(R.drawable.circle_filled_green)
                    label.setTextColor(green)
                }

                isVisuallyActive(index) -> {
                    dot.setBackgroundResource(R.drawable.circle_empty)
                    label.setTextColor(white)
                }

                else -> {
                    dot.setBackgroundResource(R.drawable.circle_filled_gray)
                    label.setTextColor(gray)
                }
            }
        }

        lines.forEachIndexed { index, line ->
            val leftDone = isVisuallyDone(index)
            val rightDoneOrActive =
                isVisuallyDone(index + 1) || isVisuallyActive(index + 1)

            val shouldLineBeGreen = leftDone && rightDoneOrActive

            line.alpha = 1f
            line.setBackgroundColor(
                if (shouldLineBeGreen) green else gray
            )
        }
    }

    private var flashHideRunnable: Runnable? = null
    private fun showFlash(message: FlashMessage) {
        val binding = _binding ?: return

        binding.flashCard.removeCallbacks(flashHideRunnable)

        binding.flashIcon.text = message.icon
        binding.tvFlash1.text = message.title
        binding.tvFlash2.text = message.subtitle

        binding.flashCard.visibility = View.VISIBLE
        binding.flashCard.alpha = 0f
        binding.flashCard.translationY = -16f

        binding.flashCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250L)
            .start()

        flashHideRunnable = Runnable {
            val currentBinding = _binding ?: return@Runnable

            currentBinding.flashCard.animate()
                .alpha(0f)
                .translationY(-16f)
                .setDuration(250L)
                .withEndAction {
                    currentBinding.flashCard.visibility = View.GONE
                }
                .start()
        }

        binding.flashCard.postDelayed(flashHideRunnable, 5500L)
    }

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress = 0f

    private fun setSmoothProgress(percent: Float) {
        val binding = _binding ?: return

        val targetProgress = (percent / 100f).coerceIn(0f, 1f)

        binding.progressFill.pivotX = 0f

        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = 700L
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                val currentBinding = _binding

                if (currentBinding == null) {
                    cancel()
                    return@addUpdateListener
                }

                val value = animator.animatedValue as Float
                currentBinding.progressFill.scaleX = value
                currentProgress = value
            }

            start()
        }
    }

}