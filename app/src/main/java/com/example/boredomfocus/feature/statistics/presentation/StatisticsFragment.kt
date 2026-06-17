package com.example.boredomfocus.feature.statistics.presentation

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.Visibility
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import com.example.boredomfocus.R
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.feature.statistics.presentation.adapter.SessionAdapter
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.databinding.FragmentStatisticsBinding
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionAdapter: SessionAdapter

    private var cardVisible = false
    private var hasRenderedOnce = false

    private var currentDetoxMinutes = 0
    private var currentFocusMinutes = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        sessionAdapter = SessionAdapter()

        binding.cardChosenChartItem.visibility = View.GONE

        setupRecyclerView()
        setupChart()
        setupStatisticsChips()

        observeUiState()
    }

    override fun onDestroyView() {
        binding.rvSessionsStatistics.adapter = null

        binding.cardChosenChartItem.animate().cancel()
        binding.detoxFocusBarChartView.onBarSelected = null
        binding.detoxFocusBarChartView.onSelectionCleared = null

        _binding = null

        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        binding.rvSessionsStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }
    }

    private fun setupChart() {
        binding.detoxFocusBarChartView.onBarSelected = { item ->
            if (!cardVisible) {
                showDetailsCard(item)
                cardVisible = true
            } else {
                animateCardContentChange {
                    updateChartCard(item)
                }
            }
        }

        binding.detoxFocusBarChartView.onSelectionCleared = {
            if (cardVisible) {
                hideDetailsCard()
                cardVisible = false
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    if (state.isLoading) {
                        if (!hasRenderedOnce) {
                            binding.statisticsContent.visibility = View.INVISIBLE
                        }

                        selectStatisticsPeriod(state.selectedPeriod)
                        return@collect
                    }

                    binding.statisticsContent.visibility = View.VISIBLE

                    render(state)

                    hasRenderedOnce = true
                }
            }
        }
    }

    private fun render(state: StatisticsUiState) {
        val summary = state.statsSummary
        val lastSummary = state.statsSummaryLast
        val period = state.selectedPeriod

        binding.tvStatisticsFocusRecordTime.text =
            formatSeconds(summary?.bestFocus ?: 0L)

        binding.tvStatisticsFocusAverageTime.text =
            formatSeconds(summary?.averageFocus?.toLong() ?: 0L)

        binding.tvStatisticsSessionsCount.text =
            (summary?.totalSessions ?: 0).toString()

        binding.tvStatisticsSessionsCountWord.text = when (period) {
            StatisticsPeriod.WEEK -> "сессий за неделю"
            StatisticsPeriod.MONTH -> "сессий за месяц"
            StatisticsPeriod.ALL_TIME -> "всего сессий"
        }

        binding.tvStatisticsOverallTimeWord1.text = when (period) {
            StatisticsPeriod.WEEK -> "всего фокуса за неделю"
            StatisticsPeriod.MONTH -> "всего фокуса за месяц"
            StatisticsPeriod.ALL_TIME -> "всего фокуса за всё время"
        }

        binding.tvStatisticsDetoxPercent.text =
            "${summary?.completionRate?.toInt() ?: 0}%"

        compareTime(
            tv = binding.tvStatisticsFocusRecordComparison,
            period = period,
            current = summary?.bestFocus,
            last = lastSummary?.bestFocus,
            record = state.allTimeFocusRecord,
            highlightRecordText = true
        )

        if (period == StatisticsPeriod.ALL_TIME) {
            renderAllTimeComparisons()
        } else {
            compareTime(
                tv = binding.tvStatisticsFocusAverageComparison,
                period = period,
                current = summary?.averageFocus?.toLong(),
                last = lastSummary?.averageFocus?.toLong(),
                record = null,
                highlightRecordText = false
            )

            compareSession(
                tv = binding.tvStatisticsSessionsComparison,
                current = summary?.totalSessions,
                last = lastSummary?.totalSessions
            )

            compareDetoxPercentage(
                tv = binding.tvStatisticsDetoxPercentComparison,
                current = summary?.completionRate,
                last = lastSummary?.completionRate
            )
        }

        binding.tvStatisticsOverallTimeWord2.text =
            formatSeconds(state.totalFocusTimePeriod ?: 0L)

        binding.tvStatisticsAverageTimeWord2.text =
            formatSeconds(state.averageFocusTimePeriod ?: 0L)

        renderWeekCircles(state)

        selectStatisticsPeriod(period)

        binding.detoxFocusBarChartView.submitData(state.periodStats)

        sessionAdapter.submitList(state.lastSessions)
    }

    private fun renderAllTimeComparisons() {
        val gray = ContextCompat.getColor(requireContext(), R.color.gray_basic)

        binding.tvStatisticsFocusAverageComparison.text = "за всё время"
        binding.tvStatisticsFocusAverageComparison.setTextColor(gray)

        binding.tvStatisticsDetoxPercentComparison.text = "за всё время"
        binding.tvStatisticsDetoxPercentComparison.setTextColor(gray)

        binding.tvStatisticsSessionsComparison.text = "с первого дня"
        binding.tvStatisticsSessionsComparison.setTextColor(gray)

        binding.tvStatisticsFocusRecordComparison.text = "лучший за всё время"
        binding.tvStatisticsFocusRecordComparison.setTextColor(gray)

        binding.tvStatisticsFocusRecordTime.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
    }

    private fun renderWeekCircles(state: StatisticsUiState) {
        val days = listOf(
            binding.viewMonday,
            binding.viewTuesday,
            binding.viewWednesday,
            binding.viewThursday,
            binding.viewFriday,
            binding.viewSaturday,
            binding.viewSunday
        )

        days.forEachIndexed { index, view ->
            val entity = state.dailyStats.getOrNull(index)

            val background = when {
                entity == null -> R.drawable.circle_empty
                entity.streakCounted -> R.drawable.circle_filled
                else -> R.drawable.circle_missed
            }

            view.setBackgroundResource(background)
        }
    }

    private fun compareTime(
        tv: TextView,
        period: StatisticsPeriod,
        current: Long?,
        last: Long?,
        record: Long?,
        highlightRecordText: Boolean
    ) {
        val currentValue = current ?: 0L
        val lastValue = last ?: 0L

        if (period == StatisticsPeriod.ALL_TIME) {
            tv.text = "за всё время"
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.gray_basic))
            return
        }

        if (record != null && currentValue >= record && currentValue > 0L) {
            tv.text = "↑ лучший за всё время"
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.green_basic))

            if (highlightRecordText) {
                binding.tvStatisticsFocusRecordTime.setTextColor(
                    ContextCompat.getColor(tv.context, R.color.green_basic)
                )
            }

            return
        }

        if (highlightRecordText) {
            binding.tvStatisticsFocusRecordTime.setTextColor(
                ContextCompat.getColor(tv.context, R.color.white)
            )
        }

        if (currentValue == lastValue) {
            tv.text = "без изменений"
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.gray_basic))
            return
        }

        val diff = kotlin.math.abs(currentValue - lastValue)
        val isBetter = currentValue > lastValue

        tv.text = if (isBetter) {
            "↑ +${formatSeconds(diff)} от прошлого"
        } else {
            "↓ −${formatSeconds(diff)} от прошлого"
        }

        tv.setTextColor(
            ContextCompat.getColor(
                tv.context,
                if (isBetter) R.color.green_basic else R.color.red_basic
            )
        )
    }

    private fun compareSession(
        tv: TextView,
        current: Int?,
        last: Int?
    ) {
        val currentValue = current ?: 0
        val lastValue = last ?: 0

        if (currentValue == lastValue) {
            tv.text = "без изменений"
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.gray_basic))
            return
        }

        val diff = kotlin.math.abs(currentValue - lastValue)
        val isBetter = currentValue > lastValue

        tv.text = if (isBetter) {
            "↑ +$diff от прошлого"
        } else {
            "↓ −$diff от прошлого"
        }

        tv.setTextColor(
            ContextCompat.getColor(
                tv.context,
                if (isBetter) R.color.green_basic else R.color.red_basic
            )
        )
    }

    private fun compareDetoxPercentage(
        tv: TextView,
        current: Double?,
        last: Double?
    ) {
        val currentValue = current ?: 0.0
        val lastValue = last ?: 0.0

        if (currentValue == lastValue) {
            tv.text = "без изменений"
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.gray_basic))
            return
        }

        val diff = kotlin.math.abs(currentValue - lastValue)
        val isBetter = currentValue > lastValue

        tv.text = if (isBetter) {
            "↑ +${String.format("%.1f", diff)}% от прошлого"
        } else {
            "↓ −${String.format("%.1f", diff)}% от прошлого"
        }

        tv.setTextColor(
            ContextCompat.getColor(
                tv.context,
                if (isBetter) R.color.green_basic else R.color.red_basic
            )
        )
    }

    private fun updateChartCard(item: ChartItem) {
        binding.tvChartItemLabel.text = item.label

        animateMinutesText(
            textView = binding.tvChartItemFocusTime,
            fromMinutes = currentFocusMinutes,
            toMinutes = item.focusMinutes
        )

        animateMinutesText(
            textView = binding.tvChartItemDetoxTime,
            fromMinutes = currentDetoxMinutes,
            toMinutes = item.detoxMinutes
        )

        currentFocusMinutes = item.focusMinutes
        currentDetoxMinutes = item.detoxMinutes

        binding.tvChartItemSessionCount.text =
            "${item.sessionsCount} сессий"
    }

    private fun showDetailsCard(item: ChartItem) {
        binding.tvChartItemLabel.text = item.label

        currentFocusMinutes = 0
        currentDetoxMinutes = 0

        binding.tvChartItemSessionCount.text =
            "${item.sessionsCount} сессий"

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
                        updateChartCard(item)
                    }
                }, 300)
            }
            .start()
    }

    private fun hideDetailsCard() {
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

    private fun animateCardContentChange(update: () -> Unit) {
        binding.cardChosenChartItem.animate().cancel()

        binding.cardChosenChartItem.animate()
            .alpha(0.6f)
            .setDuration(100)
            .withEndAction {
                if (_binding == null) return@withEndAction

                update()

                binding.cardChosenChartItem.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateMinutesText(
        textView: TextView,
        fromMinutes: Int,
        toMinutes: Int
    ) {
        ValueAnimator.ofInt(fromMinutes, toMinutes).apply {
            duration = 400

            addUpdateListener {
                val value = it.animatedValue as Int
                textView.text = formatMinutes(value)
            }

            start()
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

    private fun setupStatisticsChips() = with(binding) {
        cardWeek.setOnClickListener {
            onPeriodClicked(StatisticsPeriod.WEEK)
        }

        cardMonth.setOnClickListener {
            onPeriodClicked(StatisticsPeriod.MONTH)
        }

        cardAllTime.setOnClickListener {
            onPeriodClicked(StatisticsPeriod.ALL_TIME)
        }
    }

    private fun onPeriodClicked(period: StatisticsPeriod) {
        if (viewModel.uiState.value.selectedPeriod == period) return

        binding.detoxFocusBarChartView.clearSelection(
            notify = false,
            animate = false
        )

        if (cardVisible) {
            hideDetailsCard()
            cardVisible = false
        }

        selectStatisticsPeriod(period)

        viewModel.selectPeriod(period)
    }

    private fun selectStatisticsPeriod(period: StatisticsPeriod) {
        renderChip(
            card = binding.cardWeek,
            textView = binding.tvWeek,
            isSelected = period == StatisticsPeriod.WEEK
        )

        renderChip(
            card = binding.cardMonth,
            textView = binding.tvMonth,
            isSelected = period == StatisticsPeriod.MONTH
        )

        renderChip(
            card = binding.cardAllTime,
            textView = binding.tvAllTime,
            isSelected = period == StatisticsPeriod.ALL_TIME
        )
    }

    private fun renderChip(
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
}