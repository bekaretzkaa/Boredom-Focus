package com.example.boredomfocus.feature.statistics.presentation

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
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

    private var currentDetoxMinutes = 0
    private var currentFocusMinutes = 0

    private var isRenderingFromState = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        observeUiState()

        sessionAdapter = SessionAdapter()

        binding.cardChosenChartItem.visibility = View.GONE

        binding.rvSessionsStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }

        binding.detoxFocusBarChartView.onBarSelected = { item ->

            if(!cardVisible) {
                showDetailsCard(item)
                cardVisible = true
            } else {

                animateCardContentChange {
                    updateChartCard(item)
                }

            }
        }

        binding.detoxFocusBarChartView.onSelectionCleared = {

            hideDetailsCard()
            cardVisible = false

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
                    render(state)
                }
            }
        }
    }

    private fun render(state: StatisticsUiState) {
        isRenderingFromState = true

        binding.tvStatisticsFocusRecordTime.text = formatSeconds(state.statsSummary?.bestFocus ?: 0L)
        compareTime(binding.tvStatisticsFocusRecordComparison, state.statsSummary?.bestFocus, state.statsSummaryLast?.bestFocus, state.allTimeFocusRecord)

        binding.tvStatisticsFocusAverageTime.text = formatSeconds(state.statsSummary?.averageFocus?.toLong() ?: 0L)
        compareTime(binding.tvStatisticsFocusAverageComparison, state.statsSummary?.averageFocus?.toLong(), state.statsSummaryLast?.averageFocus?.toLong(), null)

        binding.tvStatisticsSessionsCount.text = state.statsSummary?.totalSessions.toString()
        compareSession(binding.tvStatisticsSessionsComparison,state.statsSummary?.totalSessions, state.statsSummaryLast?.totalSessions)

        binding.tvStatisticsDetoxPercent.text = "${state.statsSummary?.completionRate ?: 0.00}%"
        compareDetoxPercentage(binding.tvStatisticsDetoxPercentComparison,state.statsSummary?.completionRate, state.statsSummaryLast?.completionRate)


        val days = listOf(
            binding.viewMonday,
            binding.viewTuesday,
            binding.viewWednesday,
            binding.viewThursday,
            binding.viewFriday,
            binding.viewSaturday,
            binding.viewSunday
        )

        state.dailyStats.forEachIndexed { index, entity ->
            if(entity == null) {
                days[index].setBackgroundResource(R.drawable.circle_empty)
            } else {
                if(entity.streakCounted) {
                    days[index].setBackgroundResource(R.drawable.circle_filled)
                } else {
                    days[index].setBackgroundResource(R.drawable.circle_missed_red)
                }
            }
        }

        binding.detoxFocusBarChartView.submitData(state.dailyStats)

        sessionAdapter.submitList(state.lastSessions)

        isRenderingFromState = false
    }

    private fun compareTime(tv: TextView, current: Long?, last: Long?, record: Long?) {
        val currentValue = current ?: 0L
        val lastValue = last ?: 0L

        if(record != null) {
            if(currentValue >= record) {
                tv.text = "↑ лучший за всё время"
                tv.setTextColor(ContextCompat.getColor(tv.context, R.color.green_basic))
                binding.tvStatisticsFocusRecordTime.setTextColor(ContextCompat.getColor(tv.context, R.color.green_basic))
            } else {
                binding.tvStatisticsFocusRecordTime.setTextColor(ContextCompat.getColor(tv.context, R.color.white))
            }
        } else {
            val diff = if (currentValue > lastValue) currentValue - lastValue else lastValue - currentValue
            tv.text = if (currentValue > lastValue) "↑ +${formatSeconds(diff)} от прошлого" else "↓ −${formatSeconds(diff)} от прошлого"
            tv.setTextColor(
                ContextCompat.getColor(
                    tv.context,
                    if (currentValue > lastValue) R.color.green_basic else R.color.red_basic
                )
            )
        }
    }

    private fun compareSession(tv: TextView, current: Int?, last: Int?) {
        val currentValue = current ?: 0
        val lastValue = last ?: 0

        val diff = if(currentValue > lastValue) currentValue - lastValue else lastValue - currentValue
        tv.text = if(currentValue > lastValue) "↑ +$diff от прошлого" else "↓ −$diff от прошлого"
        tv.setTextColor(
            ContextCompat.getColor(
                tv.context,
                if (currentValue > lastValue) R.color.green_basic else R.color.red_basic
            )
        )
    }

    private fun compareDetoxPercentage(tv: TextView, current: Double?, last: Double?) {
        val currentValue = current ?: 0.0
        val lastValue = last ?: 0.0

        val diff = if(currentValue > lastValue) currentValue - lastValue else lastValue - currentValue
        tv.text = if(currentValue > lastValue) "↑ +${String.format("%.2f", diff)}% от прошлого" else "↓ −${String.format("%.2f", diff)}% от прошлого"
        tv.setTextColor(
            ContextCompat.getColor(
                tv.context,
                if (currentValue > lastValue) R.color.green_basic else R.color.red_basic
            )
        )
    }

    private fun updateChartCard(item: DailyStatsEntity) {

        binding.tvChartItemLabel.text = item.getDayLabel()

        animateMinutesText(
            binding.tvChartItemFocusTime,
            currentFocusMinutes,
            (item.totalFocusSeconds / 60).toInt()
        )

        animateMinutesText(
            binding.tvChartItemDetoxTime,
            currentDetoxMinutes,
            item.totalDetoxMinutes.toInt()
        )

        currentFocusMinutes = (item.totalFocusSeconds / 60).toInt()
        currentDetoxMinutes = item.totalDetoxMinutes.toInt()

        binding.tvChartItemSessionCount.text = "${item.sessionCount} сессий"
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        if(hours == 0) return "$minutes мин"
        return "$hours ч $minutes мин"
    }

    private fun formatSeconds(totalSeconds: Long): String {
        if(totalSeconds > 3600) {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun showDetailsCard(item: DailyStatsEntity) {

        binding.tvChartItemLabel.text = item.getDayLabel()

        currentFocusMinutes = 0
        currentDetoxMinutes = 0

        binding.tvChartItemSessionCount.text =
            "${item.sessionCount} сессий"

        binding.tvChartItemFocusTime.text = "0 мин"
        binding.tvChartItemDetoxTime.text = "0 мин"

        binding.cardChosenChartItem.apply {

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

                binding.cardChosenChartItem.postDelayed({
                    updateChartCard(item)
                }, 300)

            }
            .start()
    }

    private fun hideDetailsCard() {

        binding.cardChosenChartItem.animate()
            .alpha(0f)
            .translationY(12f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(150)
            .withEndAction {

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

    private fun animateCardContentChange(
        update: () -> Unit
    ) {

        binding.cardChosenChartItem.animate()
            .alpha(0.6f)
            .setDuration(100)
            .withEndAction {

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
        ValueAnimator.ofInt(
            fromMinutes,
            toMinutes
        ).apply {
            duration = 400
            addUpdateListener {
                val value = it.animatedValue as Int

                textView.text = formatMinutes(value)
            }
            start()
        }
    }

    private fun DailyStatsEntity.getDayLabel(): String {
        return when (LocalDate.ofEpochDay(date).dayOfWeek) {
            DayOfWeek.MONDAY -> "ПН"
            DayOfWeek.TUESDAY -> "ВТ"
            DayOfWeek.WEDNESDAY -> "СР"
            DayOfWeek.THURSDAY -> "ЧТ"
            DayOfWeek.FRIDAY -> "ПТ"
            DayOfWeek.SATURDAY -> "СБ"
            DayOfWeek.SUNDAY -> "ВС"
        }
    }
}