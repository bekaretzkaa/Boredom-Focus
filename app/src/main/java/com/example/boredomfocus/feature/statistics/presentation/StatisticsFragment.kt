package com.example.boredomfocus.feature.statistics.presentation

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.boredomfocus.R
import com.example.boredomfocus.databinding.FragmentStatisticsBinding
import com.example.boredomfocus.feature.statistics.presentation.adapter.SessionAdapter
import com.example.boredomfocus.feature.statistics.presentation.model.CardType
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.feature.statistics.presentation.model.StatisticsPeriod
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionAdapter: SessionAdapter

    private var isSelectedChartCardVisible = false
    private var hasRenderedContentOnce = false

    private var selectedChartCardDetoxMinutes = 0
    private var selectedChartCardFocusMinutes = 0

    private var previousUiState = StatisticsUiState()

    // region Lifecycle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStatisticsBinding.bind(view)
        sessionAdapter = SessionAdapter()

        binding.cardChosenChartItem.visibility = View.GONE

        setupSessionList()
        setupBarChart()
        setupPeriodChips()
        collectUiState()
    }

    override fun onDestroyView() {
        binding.rvSessionsStatistics.adapter = null

        binding.cardChosenChartItem.animate().cancel()
        binding.detoxFocusBarChartView.onBarSelected = null
        binding.detoxFocusBarChartView.onSelectionCleared = null

        _binding = null

        super.onDestroyView()
    }

    // endregion

    // region Setup

    private fun setupSessionList() {
        binding.rvSessionsStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }
    }

    private fun setupBarChart() {
        binding.detoxFocusBarChartView.onBarSelected = { item ->
            if (!isSelectedChartCardVisible) {
                showSelectedChartCard(item)
                isSelectedChartCardVisible = true
            } else {
                animateSelectedChartCardContentChange {
                    renderSelectedChartCardContent(item)
                }
            }
        }

        binding.detoxFocusBarChartView.onSelectionCleared = {
            if (isSelectedChartCardVisible) {
                hideSelectedChartCard()
                isSelectedChartCardVisible = false
            }
        }
    }

    private fun setupPeriodChips() = with(binding) {
        cardWeek.setOnClickListener {
            onPeriodChipClicked(StatisticsPeriod.WEEK)
        }

        cardMonth.setOnClickListener {
            onPeriodChipClicked(StatisticsPeriod.MONTH)
        }

        cardAllTime.setOnClickListener {
            onPeriodChipClicked(StatisticsPeriod.ALL_TIME)
        }
    }

    // endregion

    // region State collection

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        renderLoadingState(state.selectedPeriod)
                        return@collect
                    }

                    binding.statisticsContent.visibility = View.VISIBLE

                    renderStatisticsScreen(state)

                    hasRenderedContentOnce = true
                }
            }
        }
    }

    private fun renderLoadingState(selectedPeriod: StatisticsPeriod) {
        if (!hasRenderedContentOnce) {
            binding.statisticsContent.visibility = View.INVISIBLE
        }

        renderSelectedPeriodChips(selectedPeriod)
    }

    // endregion

    // region Main render

    private fun renderStatisticsScreen(state: StatisticsUiState) {
        val summary = state.statsSummary
        val previousSummary = state.statsSummaryLast
        val selectedPeriod = state.selectedPeriod

        val isFirstPeriod = selectedPeriod != StatisticsPeriod.ALL_TIME &&
                hasNoPreviousStatsForComparison(previousSummary)

        val isPeriodJustStarted = selectedPeriod != StatisticsPeriod.ALL_TIME &&
                !isFirstPeriod &&
                hasPeriodJustStarted(selectedPeriod)

//        TEST
//        val isFirstPeriod = false
//        val isPeriodJustStarted = false

        renderSummaryValues(
            summary = summary,
            selectedPeriod = selectedPeriod
        )

        renderComparisonLabels(
            state = state,
            summary = summary,
            previousSummary = previousSummary,
            selectedPeriod = selectedPeriod,
            isFirstPeriod = isFirstPeriod,
            isPeriodJustStarted = isPeriodJustStarted,
            daysWithoutSession = state.daysWithoutSession
        )

        renderPeriodTotalValues(state)
        renderWeeklyStreakCircles(state)
        renderSelectedPeriodChips(selectedPeriod)

        binding.detoxFocusBarChartView.submitData(state.periodStats)
        renderLastSessions(state.lastSessions, selectedPeriod, previousSummary?.totalSessions ?: 0)

        animateStatisticsCardsEntrance()
        animateChangedSummaryValues(
            previousState = previousUiState,
            currentState = state
        )

        previousUiState = state
    }

    private fun renderLastSessions(sessions: List<SessionListItem>, period: StatisticsPeriod, previous: Int) {
        if(sessions.isEmpty()) {
            binding.tvLastSessions1.visibility = View.VISIBLE
            binding.tvLastSessions2.visibility = View.VISIBLE
            binding.tvLastSessions3.visibility = View.VISIBLE
            binding.flLastSessions.visibility = View.VISIBLE

            when(period) {
                StatisticsPeriod.WEEK -> {
                    binding.ivLastSessions.setBackgroundResource(R.drawable.ic_event_busy)
                    binding.tvLastSessions1.text = "сессии за неделю"
                    binding.tvLastSessions2.text = "На этой неделе пусто"
                    binding.tvLastSessions3.text = "На прошлой было $previous сессий"
                }
                StatisticsPeriod.MONTH -> {
                    binding.ivLastSessions.setBackgroundResource(R.drawable.ic_event_busy)
                    binding.tvLastSessions1.text = "сессии за месяц"
                    binding.tvLastSessions2.text = "В этом месяце пусто"
                    binding.tvLastSessions3.text = "В прошлом месяце было $previous сессий"
                }
                StatisticsPeriod.ALL_TIME -> {
                    binding.ivLastSessions.setBackgroundResource(R.drawable.ic_play)
                    binding.tvLastSessions1.text = "последние сессии"
                    binding.tvLastSessions2.text = "Ещё нет сессий"
                    binding.tvLastSessions3.text = "Начни первую — она появится здесь"
                }
            }

            binding.rvSessionsStatistics.visibility = View.GONE
        } else {
            sessionAdapter.submitList(sessions)
            binding.rvSessionsStatistics.visibility = View.VISIBLE

            binding.tvLastSessions1.visibility = View.GONE
            binding.tvLastSessions2.visibility = View.GONE
            binding.tvLastSessions3.visibility = View.GONE
            binding.flLastSessions.visibility = View.GONE
        }
    }

    private fun renderSummaryValues(
        summary: StatsSummary?,
        selectedPeriod: StatisticsPeriod
    ) {
        binding.tvStatisticsFocusRecordTime.text =
            formatSeconds(summary?.bestFocus ?: 0L)

        binding.tvStatisticsFocusAverageTime.text =
            formatSeconds(summary?.averageFocus?.toLong() ?: 0L)

        binding.tvStatisticsSessionsCount.text =
            (summary?.totalSessions ?: 0).toString()

        binding.tvStatisticsSessionsCountWord.text =
            getSessionCountTitle(selectedPeriod)

        binding.tvStatisticsOverallTimeWord1.text =
            getTotalFocusTitle(selectedPeriod)

        renderCompletionRateValue(summary?.completionRate)
    }

    private fun renderCompletionRateValue(completionRate: Double?) {
        val completionRateInt = completionRate?.toInt() ?: 0
        val textColor = if (completionRateInt == 100) {
            R.color.green_basic
        } else {
            R.color.detox_percent
        }

        binding.tvStatisticsDetoxPercent.text = "$completionRateInt%"
        binding.tvStatisticsDetoxPercent.setTextColor(
            ContextCompat.getColor(requireContext(), textColor)
        )
    }

    private fun renderComparisonLabels(
        state: StatisticsUiState,
        summary: StatsSummary?,
        previousSummary: StatsSummary?,
        selectedPeriod: StatisticsPeriod,
        isFirstPeriod: Boolean,
        isPeriodJustStarted: Boolean,
        daysWithoutSession: Int? = null
    ) {
        renderTimeComparison(
            textView = binding.tvStatisticsFocusRecordComparison,
            period = selectedPeriod,
            current = summary?.bestFocus,
            previous = previousSummary?.bestFocus,
            allTimeRecord = state.allTimeFocusRecord,
            shouldHighlightRecordValue = true,
            isFirstPeriod = isFirstPeriod,
            isPeriodJustStarted = isPeriodJustStarted
        )

        if (selectedPeriod == StatisticsPeriod.ALL_TIME) {
            renderAllTimeComparisonLabels()
            return
        }

        renderTimeComparison(
            textView = binding.tvStatisticsFocusAverageComparison,
            period = selectedPeriod,
            current = summary?.averageFocus?.toLong(),
            previous = previousSummary?.averageFocus?.toLong(),
            allTimeRecord = null,
            shouldHighlightRecordValue = false,
            isFirstPeriod = isFirstPeriod,
            isPeriodJustStarted = isPeriodJustStarted,
        )

        renderSessionCountComparison(
            textView = binding.tvStatisticsSessionsComparison,
            period = selectedPeriod,
            current = summary?.totalSessions,
            previous = previousSummary?.totalSessions,
            isFirstPeriod = isFirstPeriod,
            isPeriodJustStarted = isPeriodJustStarted,
            daysWithoutSession
        )

        renderCompletionRateComparison(
            textView = binding.tvStatisticsDetoxPercentComparison,
            period = selectedPeriod,
            current = summary?.completionRate,
            previous = previousSummary?.completionRate,
            isFirstPeriod = isFirstPeriod,
            isPeriodJustStarted = isPeriodJustStarted
        )
    }

    private fun renderAllTimeComparisonLabels() {
        val gray = ContextCompat.getColor(requireContext(), R.color.gray_basic)

        binding.tvStatisticsFocusAverageComparison.text = "за всё время"
        binding.tvStatisticsFocusAverageComparison.setTextColor(gray)

        binding.tvStatisticsDetoxPercentComparison.text = "за всё время"
        binding.tvStatisticsDetoxPercentComparison.setTextColor(gray)

        binding.tvStatisticsSessionsComparison.text = "с первого дня"
        binding.tvStatisticsSessionsComparison.setTextColor(gray)

        binding.tvStatisticsFocusRecordComparison.text = "↑ лучший за всё время"
        binding.tvStatisticsFocusRecordComparison.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.green_basic)
        )

        binding.tvStatisticsFocusRecordTime.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.green_basic)
        )
    }

    private fun renderPeriodTotalValues(state: StatisticsUiState) {
        binding.tvStatisticsOverallTimeWord2.text =
            formatSeconds(state.totalFocusTimePeriod ?: 0L)

        binding.tvStatisticsAverageTimeWord2.text =
            formatSeconds(state.averageFocusTimePeriod ?: 0L)
    }

    private fun renderWeeklyStreakCircles(state: StatisticsUiState) {
        val dayViews = listOf(
            binding.viewMonday,
            binding.viewTuesday,
            binding.viewWednesday,
            binding.viewThursday,
            binding.viewFriday,
            binding.viewSaturday,
            binding.viewSunday
        )

        dayViews.forEachIndexed { index, dayView ->
            val dailyStats = state.dailyStats.getOrNull(index)

            val background = when {
                dailyStats == null -> R.drawable.circle_empty
                dailyStats.streakCounted -> R.drawable.circle_filled
                else -> R.drawable.circle_missed
            }

            dayView.setBackgroundResource(background)
        }
    }

    // endregion

    // region Comparison logic

    private fun renderTimeComparison(
        textView: TextView,
        period: StatisticsPeriod,
        current: Long?,
        previous: Long?,
        allTimeRecord: Long?,
        shouldHighlightRecordValue: Boolean,
        isFirstPeriod: Boolean,
        isPeriodJustStarted: Boolean
    ) {
        val currentValue = current ?: 0L
        val previousValue = previous ?: 0L

        if (period == StatisticsPeriod.ALL_TIME) {
            textView.text = "за всё время"
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if (isFirstPeriod) {
            if(shouldHighlightRecordValue && currentValue > 0) {
                textView.text = "↑ твой первый рекорд"
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.green_basic))

                return
            }

            textView.text = getFirstPeriodComparisonText(if(shouldHighlightRecordValue) CardType.FOCUS_RECORD else CardType.FOCUS_AVERAGE,period)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))

            if (shouldHighlightRecordValue) {
                binding.tvStatisticsFocusRecordTime.setTextColor(
                    ContextCompat.getColor(textView.context, R.color.white)
                )
            }

            return
        }

        if (allTimeRecord != null && currentValue >= allTimeRecord && currentValue > 0L) {
            textView.text = "★ новый рекорд"
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.green_basic))

            if (shouldHighlightRecordValue) {
                binding.tvStatisticsFocusRecordTime.setTextColor(
                    ContextCompat.getColor(textView.context, R.color.green_basic)
                )
            }

            return
        }

        if (shouldHighlightRecordValue) {
            binding.tvStatisticsFocusRecordTime.setTextColor(
                ContextCompat.getColor(textView.context, R.color.white)
            )
        }

        if (isPeriodJustStarted && currentValue < previousValue && previousValue > 0L) {
            textView.text = getPeriodJustStartedComparisonText(period, if(shouldHighlightRecordValue) CardType.FOCUS_RECORD else CardType.FOCUS_AVERAGE, if(currentValue == 0L) null else currentValue, previous, true, null)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        } else if(!isPeriodJustStarted && currentValue == 0L) {
            textView.text = getPeriodJustStartedComparisonText(period, if(shouldHighlightRecordValue) CardType.FOCUS_RECORD else CardType.FOCUS_AVERAGE, if(currentValue == 0L) null else currentValue, previous, false, null)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if (currentValue == previousValue) {
            textView.text = "— стабильно"
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        val difference = kotlin.math.abs(currentValue - previousValue)
        val isBetter = currentValue > previousValue
        val bigDrop = currentValue.toDouble() / previousValue < 0.65 && !shouldHighlightRecordValue

        textView.text = if (isBetter) {
            "↑ +${formatSeconds(difference)} от прошлого"
        } else {
            if(bigDrop) {
                "прошлый был пиком"
            } else {
                "↓ −${formatSeconds(difference)} от прошлого"
            }
        }

        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                if (isBetter) R.color.green_basic else if(bigDrop) R.color.gray_basic else R.color.red_basic
            )
        )
    }

    private fun renderSessionCountComparison(
        textView: TextView,
        period: StatisticsPeriod,
        current: Int?,
        previous: Int?,
        isFirstPeriod: Boolean,
        isPeriodJustStarted: Boolean,
        daysWithoutSession: Int?
    ) {
        val currentValue = current ?: 0
        val previousValue = previous ?: 0

        if (isFirstPeriod) {
            textView.text = getFirstPeriodComparisonText(CardType.SESSIONS, period)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if (isPeriodJustStarted && currentValue < previousValue && previousValue > 0) {
            textView.text = getPeriodJustStartedComparisonText(period, CardType.SESSIONS, if(currentValue == 0) null else currentValue.toLong(), previous?.toLong(), true, daysWithoutSession)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        } else if(!isPeriodJustStarted && currentValue == 0) {
            textView.text = getPeriodJustStartedComparisonText(period, CardType.SESSIONS, if(currentValue == 0) null else currentValue.toLong(), previous?.toLong(), false, daysWithoutSession)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if (currentValue == previousValue) {
            textView.text = "— стабильно"
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        val difference = kotlin.math.abs(currentValue - previousValue)
        val isBetter = currentValue > previousValue
        val bigDrop = currentValue.toDouble() / previousValue < 0.65

        textView.text = if (isBetter) {
            "↑ +$difference от прошлого"
        } else {
            if(bigDrop) "меньше обычного" else "↓ −$difference от прошлого"
        }

        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                if (isBetter) R.color.green_basic else if(bigDrop) R.color.gray_basic else R.color.red_basic
            )
        )
    }

    private fun renderCompletionRateComparison(
        textView: TextView,
        period: StatisticsPeriod,
        current: Double?,
        previous: Double?,
        isFirstPeriod: Boolean,
        isPeriodJustStarted: Boolean
    ) {
        val currentValue = current ?: 0.0
        val previousValue = previous ?: 0.0

        if (isFirstPeriod) {
            textView.text = getFirstPeriodComparisonText(CardType.DETOX_ENDED, period)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if (isPeriodJustStarted && currentValue < previousValue && previousValue > 0.0) {
            textView.text = getPeriodJustStartedComparisonText(period, CardType.DETOX_ENDED, if(currentValue == 0.0) null else currentValue?.toLong(), previous?.toLong(), true, null)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        } else if (!isPeriodJustStarted && currentValue == 0.0) {
            textView.text = getPeriodJustStartedComparisonText(period, CardType.DETOX_ENDED, if(currentValue == 0.0) null else currentValue?.toLong(), previous?.toLong(), false, null)
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        if(currentValue == 100.0) {
            textView.text = when(period) {
                StatisticsPeriod.ALL_TIME -> "идеально ✓"
                StatisticsPeriod.MONTH -> "идеальный месяц ✓"
                StatisticsPeriod.WEEK -> "идеальная неделя ✓"
            }
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.green_basic))
            return
        }

        if (currentValue == previousValue) {
            textView.text = "— стабильно"
            textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray_basic))
            return
        }

        val difference = kotlin.math.abs(currentValue - previousValue)
        val isBetter = currentValue > previousValue
        val bigDrop = currentValue.toDouble() / previousValue < 0.65

        textView.text = if (isBetter) {
            "↑ +${String.format("%.1f", difference)}% от прошлого"
        } else {
            if(bigDrop) "обычно у тебя ${String.format("%.1f", previousValue)}%" else "↓ −${String.format("%.1f", difference)}% от прошлого"
        }

        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                if (isBetter) R.color.green_basic else if(bigDrop) R.color.gray_basic else R.color.red_basic
            )
        )
    }

    private fun hasNoPreviousStatsForComparison(previousSummary: StatsSummary?): Boolean {
        return previousSummary == null ||
                (
                        previousSummary.bestFocus == 0L &&
                                previousSummary.averageFocus == 0.0 &&
                                previousSummary.totalSessions == 0 &&
                                previousSummary.completionRate == 0.0
                        )
    }

    private fun hasPeriodJustStarted(period: StatisticsPeriod): Boolean {
        val today = LocalDate.now()

        return when (period) {
            StatisticsPeriod.WEEK -> today.dayOfWeek.value <= 2
            StatisticsPeriod.MONTH -> today.dayOfMonth <= 3
            StatisticsPeriod.ALL_TIME -> false
        }
    }

    private fun getFirstPeriodComparisonText(cardType: CardType, period: StatisticsPeriod): String {
        return when (cardType) {
            CardType.FOCUS_RECORD -> when(period) {
                StatisticsPeriod.WEEK -> "первая неделя"
                StatisticsPeriod.MONTH -> "первый месяц"
                StatisticsPeriod.ALL_TIME -> "↑ лучший за всё время"
            }
            CardType.FOCUS_AVERAGE -> "отправная точка"
            CardType.SESSIONS -> "начало пути"
            CardType.DETOX_ENDED -> "первый результат"
        }
    }

    private fun getPeriodJustStartedComparisonText(period: StatisticsPeriod, cardType: CardType, current: Long?, previous: Long?, justStarted: Boolean, daysWithoutSession: Int?): String {
        return when(cardType) {
            CardType.FOCUS_RECORD -> {
                if(justStarted) {
                    "цель: ${formatSeconds(previous ?: 0)}"
                } else {
                    when(period) {
                        StatisticsPeriod.WEEK -> "прошлая: ${formatSeconds(previous ?: 0)}"
                        else -> "прошлый: ${formatSeconds(previous ?: 0)}"
                    }
                }
            }
            CardType.FOCUS_AVERAGE -> {
                if(justStarted) {
                    if(current == null) {
                        when(period) {
                            StatisticsPeriod.WEEK -> "начало недели"
                            else -> "начало месяца"
                        }
                    } else "это только начало"
                } else {
                    "начни сессию"
                }
            }
            CardType.SESSIONS -> {
                if(justStarted) {
                    when(period) {
                        StatisticsPeriod.WEEK -> "прошлая: ${previous ?: 0}"
                        else -> "прошлый: ${previous ?: 0}"
                    }
                } else {
                    "${daysWithoutSession ?: 0} дней без сессий"
                }
            }
            CardType.DETOX_ENDED -> {
                if(justStarted) {
                    if(current == null) {
                        when(period) {
                            StatisticsPeriod.WEEK -> "начало недели"
                            else -> "начало месяца"
                        }
                    } else "рано судить"
                } else {
                    "начни сегодня"
                }
            }
        }
    }

    // endregion

    // region Selected chart card

    private fun showSelectedChartCard(item: ChartItem) {
        binding.tvChartItemLabel.text = item.label

        selectedChartCardFocusMinutes = 0
        selectedChartCardDetoxMinutes = 0

        binding.tvChartItemSessionCount.text = "${item.sessionsCount} сессий"
        binding.tvChartItemFocusTime.text = "0 мин"
        binding.tvChartItemDetoxTime.text = "0 мин"

        binding.cardChosenChartItem.apply {
            animate().cancel()

            alpha = 0f
            translationY = 24f
            scaleX = 0.96f
            scaleY = 0.96f
        }

        TransitionManager.beginDelayedTransition(
            binding.cardChosenChartItem.parent as ViewGroup,
            AutoTransition()
        )

        binding.cardChosenChartItem.visibility = View.VISIBLE

        binding.cardChosenChartItem.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setDuration(280)
            .withEndAction {
                if (_binding == null) return@withEndAction

                binding.cardChosenChartItem.postDelayed({
                    if (_binding != null) {
                        renderSelectedChartCardContent(item)
                    }
                }, 300)
            }
            .start()
    }

    private fun hideSelectedChartCard() {
        binding.cardChosenChartItem.animate().cancel()

        binding.cardChosenChartItem.animate()
            .alpha(0f)
            .translationY(12f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(150)
            .withEndAction {
                if (_binding == null) return@withEndAction

                TransitionManager.beginDelayedTransition(
                    binding.cardChosenChartItem.parent as ViewGroup,
                    AutoTransition().apply {
                        duration = 200
                    }
                )

                binding.cardChosenChartItem.visibility = View.GONE

                binding.cardChosenChartItem.translationY = 0f
                binding.cardChosenChartItem.scaleX = 1f
                binding.cardChosenChartItem.scaleY = 1f
            }
            .start()
    }

    private fun renderSelectedChartCardContent(item: ChartItem) {
        binding.tvChartItemLabel.text = item.label

        animateMinuteValue(
            textView = binding.tvChartItemFocusTime,
            fromMinutes = selectedChartCardFocusMinutes,
            toMinutes = item.focusMinutes
        )

        animateMinuteValue(
            textView = binding.tvChartItemDetoxTime,
            fromMinutes = selectedChartCardDetoxMinutes,
            toMinutes = item.detoxMinutes
        )

        selectedChartCardFocusMinutes = item.focusMinutes
        selectedChartCardDetoxMinutes = item.detoxMinutes

        binding.tvChartItemSessionCount.text = "${item.sessionsCount} сессий"
    }

    private fun animateSelectedChartCardContentChange(updateContent: () -> Unit) {
        binding.cardChosenChartItem.animate().cancel()

        binding.cardChosenChartItem.animate()
            .alpha(0.6f)
            .setDuration(100)
            .withEndAction {
                if (_binding == null) return@withEndAction

                updateContent()

                binding.cardChosenChartItem.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    // endregion

    // region Period chips

    private fun onPeriodChipClicked(period: StatisticsPeriod) {
        if (viewModel.uiState.value.selectedPeriod == period) return

        binding.detoxFocusBarChartView.clearSelection(
            notify = false,
            animate = false
        )

        if (isSelectedChartCardVisible) {
            hideSelectedChartCard()
            isSelectedChartCardVisible = false
        }

        renderSelectedPeriodChips(period)

        viewModel.selectPeriod(period)
    }

    private fun renderSelectedPeriodChips(period: StatisticsPeriod) {
        renderPeriodChip(
            card = binding.cardWeek,
            textView = binding.tvWeek,
            isSelected = period == StatisticsPeriod.WEEK
        )

        renderPeriodChip(
            card = binding.cardMonth,
            textView = binding.tvMonth,
            isSelected = period == StatisticsPeriod.MONTH
        )

        renderPeriodChip(
            card = binding.cardAllTime,
            textView = binding.tvAllTime,
            isSelected = period == StatisticsPeriod.ALL_TIME
        )
    }

    private fun renderPeriodChip(
        card: MaterialCardView,
        textView: TextView,
        isSelected: Boolean
    ) {
        val selectedBackground = Color.parseColor("#1A1A1A")
        val unselectedBackground = Color.parseColor("#0D0D0D")

        val selectedStroke = Color.parseColor("#3A3A3A")
        val unselectedStroke = Color.parseColor("#1F1F1F")

        val selectedText = Color.WHITE
        val unselectedText = Color.parseColor("#5F5F5F")

        card.setCardBackgroundColor(
            if (isSelected) selectedBackground else unselectedBackground
        )

        card.strokeColor =
            if (isSelected) selectedStroke else unselectedStroke

        textView.setTextColor(
            if (isSelected) selectedText else unselectedText
        )
    }

    // endregion

    // region Animations

    private fun animateChangedSummaryValues(
        previousState: StatisticsUiState,
        currentState: StatisticsUiState
    ) {
        val previousSummary = previousState.statsSummary
        val currentSummary = currentState.statsSummary

        binding.tvStatisticsSessionsCount.animateIntValueText(
            from = previousSummary?.totalSessions,
            to = currentSummary?.totalSessions ?: 0
        )

        binding.tvStatisticsDetoxPercent.animateIntValueText(
            from = previousSummary?.completionRate?.toInt(),
            to = currentSummary?.completionRate?.toInt() ?: 0,
            suffix = "%"
        )

        animateSecondsValue(
            textView = binding.tvStatisticsFocusRecordTime,
            fromSeconds = previousSummary?.bestFocus ?: 0L,
            toSeconds = currentSummary?.bestFocus ?: 0L,
            duration = 800
        )

        animateSecondsValue(
            textView = binding.tvStatisticsFocusAverageTime,
            fromSeconds = previousSummary?.averageFocus?.toLong() ?: 0L,
            toSeconds = currentSummary?.averageFocus?.toLong() ?: 0L,
            duration = 800
        )

        animateSecondsValue(
            textView = binding.tvStatisticsOverallTimeWord2,
            fromSeconds = previousState.totalFocusTimePeriod ?: 0L,
            toSeconds = currentState.totalFocusTimePeriod ?: 0L,
            duration = 800
        )

        animateSecondsValue(
            textView = binding.tvStatisticsAverageTimeWord2,
            fromSeconds = previousState.averageFocusTimePeriod ?: 0L,
            toSeconds = currentState.averageFocusTimePeriod ?: 0L,
            duration = 800
        )
    }

    private fun animateStatisticsCardsEntrance() {
        val cards = listOf(
            binding.cardStatisticsFocusRecord,
            binding.cardStatisticsFocusAverage,
            binding.cardStatisticsSessions,
            binding.cardStatisticsDetoxEnded,
            binding.cardStatisticsOverall
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 12.dp.toFloat()
            card.scaleX = 0.97f
            card.scaleY = 0.97f

            card.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(index * 55L)
                .setDuration(300L)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
    }

    private fun animateMinuteValue(
        textView: TextView,
        fromMinutes: Int,
        toMinutes: Int
    ) {
        ValueAnimator.ofInt(fromMinutes, toMinutes).apply {
            duration = 400

            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                textView.text = formatMinutes(value)
            }

            start()
        }
    }

    private fun TextView.animateIntValueText(
        from: Int?,
        to: Int,
        suffix: String = ""
    ) {
        ValueAnimator.ofInt(from ?: 0, to).apply {
            duration = 800
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                text = "${animator.animatedValue}$suffix"
            }

            start()
        }
    }

    private fun animateSecondsValue(
        textView: TextView,
        fromSeconds: Long,
        toSeconds: Long,
        duration: Long = 500
    ) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                val currentValue = fromSeconds + ((toSeconds - fromSeconds) * progress).toLong()

                textView.text = formatSecondsAsTimer(currentValue)
            }

            start()
        }
    }

    // endregion

    // region Text helpers

    private fun getSessionCountTitle(period: StatisticsPeriod): String {
        return when (period) {
            StatisticsPeriod.WEEK -> "сессий за неделю"
            StatisticsPeriod.MONTH -> "сессий за месяц"
            StatisticsPeriod.ALL_TIME -> "всего сессий"
        }
    }

    private fun getTotalFocusTitle(period: StatisticsPeriod): String {
        return when (period) {
            StatisticsPeriod.WEEK -> "всего фокуса за неделю"
            StatisticsPeriod.MONTH -> "всего фокуса за месяц"
            StatisticsPeriod.ALL_TIME -> "всего фокуса за всё время"
        }
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours == 0) {
            "$minutes мин"
        } else {
            "$hours ч $minutes мин"
        }
    }

    private fun formatSeconds(totalSeconds: Long): String {
        return if (totalSeconds >= 3600) {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun formatSecondsAsTimer(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }

    // endregion

    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()
}
