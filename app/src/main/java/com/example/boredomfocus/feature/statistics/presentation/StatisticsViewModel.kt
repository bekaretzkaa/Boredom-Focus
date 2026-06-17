package com.example.boredomfocus.feature.statistics.presentation

import android.util.Range
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.RangeDays
import com.example.boredomfocus.core.common.RangeMillis
import com.example.boredomfocus.core.common.formatDateFromEpochMillis
import com.example.boredomfocus.core.common.getCalendarMonthRange
import com.example.boredomfocus.core.common.getCalendarMonthRangeDay
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.data.local.dao.MonthStatsResult
import com.example.boredomfocus.data.local.dao.MonthWeekStatsResult
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.WEEK,
    val isLoading: Boolean = true,
    val statsSummary: StatsSummary? = null,
    val statsSummaryLast: StatsSummary? = null,
    val allTimeFocusRecord: Long? = null,
    val lastSessions: List<SessionListItem> = listOf(),
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val periodStats: List<ChartItem> = listOf(),
    val totalFocusTimePeriod: Long? = null,
    val averageFocusTimePeriod: Long? = null
)

data class SessionGroupFlow(
    val statsSummary: StatsSummary? = null,
    val statsSummaryLast: StatsSummary? = null,
    val allTimeFocusRecord: Long? = null,
    val lastSessions: List<SessionListItem> = listOf()
)

data class DailyStatsGroupFlow(
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val periodStats: List<ChartItem> = listOf(),
    val totalFocusTimePeriod: Long? = null,
    val averageFocusTimePeriod: Long? = null
)

enum class StatisticsPeriod {
    WEEK,
    MONTH,
    ALL_TIME
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    private val currentWeekRangeMillis = getCalendarWeekRange(0)
    private val previousWeekRangeMillis = getCalendarWeekRange(-1)
    private val currentWeekDaysRangeDays = getCalendarWeekRangeDay(0)

    private val currentMonthRangeMillis = getCalendarMonthRange(0)
    private val previousMonthRangeMillis = getCalendarMonthRange(-1)
    private val currentMonthDaysRangeDays = getCalendarMonthRangeDay(0)

    private val currentYearRangeMillis = 0
    private val currentYearDaysRangeDays = 0

    private var selectedPeriod = MutableStateFlow(StatisticsPeriod.WEEK)

    val uiState: StateFlow<StatisticsUiState> =
        selectedPeriod.flatMapLatest { period ->
            when(period) {
                StatisticsPeriod.WEEK -> {
                    buildPeriodState(
                        period = StatisticsPeriod.WEEK,
                        currentRangeMillis = currentWeekRangeMillis,
                        previousRangeMillis = previousWeekRangeMillis,
                        currentRangeDays = currentWeekDaysRangeDays,
                        currentRangeWeekDays = currentWeekDaysRangeDays
                    )
                }

                StatisticsPeriod.MONTH -> {
                    buildPeriodState(
                        period = StatisticsPeriod.MONTH,
                        currentRangeMillis = currentMonthRangeMillis,
                        previousRangeMillis = previousMonthRangeMillis,
                        currentRangeDays = currentMonthDaysRangeDays,
                        currentRangeWeekDays = currentWeekDaysRangeDays
                    )
                }

                StatisticsPeriod.ALL_TIME -> {
                    buildPeriodState(
                        period = StatisticsPeriod.ALL_TIME,
                        currentRangeMillis = currentWeekRangeMillis, // TODO
                        previousRangeMillis = previousWeekRangeMillis, // TODO
                        currentRangeDays = currentWeekDaysRangeDays, // TODO
                        currentRangeWeekDays = currentWeekDaysRangeDays
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())
    fun selectPeriod(period: StatisticsPeriod) {
        selectedPeriod.value = period
    }

    private fun buildPeriodState(
        period: StatisticsPeriod,
        currentRangeMillis: RangeMillis,
        previousRangeMillis: RangeMillis,
        currentRangeDays: RangeDays,
        currentRangeWeekDays: RangeDays
    ) : Flow<StatisticsUiState> {

        val sessionGroup = combine(
            sessionRepository.getStatsSummaryBetween(currentRangeMillis.startMillis, currentRangeMillis.endMillis),
            sessionRepository.getStatsSummaryBetween(previousRangeMillis.startMillis, previousRangeMillis.endMillis),
            sessionRepository.getAllTimeFocusRecord(),
            sessionRepository.getLastSessions(10)
        ) { statsSummary,statsSummaryLast, focusRecord, lastSessions ->

            val updatedLastSessions = mutableListOf<SessionListItem>()
            lastSessions.forEach { entity ->
                if(entity.focusSeconds != 0L && entity.detoxMinutes != 0L) {
                    if(SessionListItem.Header(formatDateFromEpochMillis(entity.date)) in updatedLastSessions) {
                        updatedLastSessions.add(SessionListItem.Session(
                            entity.detoxMinutes.toInt() * 60,
                            entity.focusSeconds.toInt()
                        ))
                    } else {
                        updatedLastSessions.add(SessionListItem.Header(formatDateFromEpochMillis(entity.date)))
                        updatedLastSessions.add(SessionListItem.Session(
                            entity.detoxMinutes.toInt() * 60,
                            entity.focusSeconds.toInt()
                        ))
                    }
                }
            }

            SessionGroupFlow(
                statsSummary = statsSummary,
                statsSummaryLast = statsSummaryLast,
                allTimeFocusRecord = focusRecord,
                lastSessions = updatedLastSessions
            )
        }

        val dailyStatsGroup = combine(
            dailyStatsRepository.getDailyStatsBetween(currentRangeWeekDays.startDay, currentRangeWeekDays.endDay),
            when(period) {
                StatisticsPeriod.WEEK -> {
                    dailyStatsRepository.getDailyStatsBetween(currentRangeDays.startDay, currentRangeDays.endDay)
                }
                StatisticsPeriod.MONTH -> {
                    dailyStatsRepository.getMonthStatsByWeeks(currentRangeDays.startDay, currentRangeDays.endDay)
                }
                StatisticsPeriod.ALL_TIME -> {
                    dailyStatsRepository.getStatsByMonths(currentRangeDays.startDay, currentRangeDays.endDay)
                }
            },
            dailyStatsRepository.getFocusStatsBetween(currentRangeDays.startDay, currentRangeDays.endDay)
        ) { dailyStats, periodStats, focusTime ->
            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            updatedDailyStats.addAll(dailyStats)
            repeat(7 - dailyStats.size) {
                updatedDailyStats.add(null)
            }

            val updatedPeriodStats = mutableListOf<ChartItem>()

            when(period) {
                StatisticsPeriod.WEEK -> {
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    for(i in updatedPeriodStats.size+1..7) {
                        updatedPeriodStats.add(ChartItem(toRussionWeekDay(i), -1, -1, -1))
                    }
                }
                StatisticsPeriod.MONTH -> {
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    for(i in updatedPeriodStats.size+1..5) {
                        updatedPeriodStats.add(ChartItem("$i нед", -1, -1, -1))
                    }
                }
                StatisticsPeriod.ALL_TIME -> {
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    for(i in updatedPeriodStats.size+1..3) {
                        updatedPeriodStats.add(ChartItem("$i мес", -1, -1, -1))
                    }
                }
            }

            DailyStatsGroupFlow(
                dailyStats = updatedDailyStats,
                periodStats = updatedPeriodStats,
                totalFocusTimePeriod = focusTime,
                averageFocusTimePeriod = focusTime / 7
            )
        }

        return combine(
            sessionGroup,
            dailyStatsGroup
        ) { sessionGroup, dailyStatsGroup ->
            StatisticsUiState(
                selectedPeriod = period,
                isLoading = false,
                statsSummary = sessionGroup.statsSummary,
                statsSummaryLast = sessionGroup.statsSummaryLast,
                allTimeFocusRecord = sessionGroup.allTimeFocusRecord,
                lastSessions = sessionGroup.lastSessions,
                dailyStats = dailyStatsGroup.dailyStats,
                periodStats = dailyStatsGroup.periodStats,
                totalFocusTimePeriod = dailyStatsGroup.totalFocusTimePeriod,
                averageFocusTimePeriod = dailyStatsGroup.averageFocusTimePeriod
            )
        }
    }

    private fun toChartItem(item: Any): ChartItem {
        return when(item) {
            is DailyStatsEntity -> {
                ChartItem(
                    label = epochDayToRussianWeekDay(item.date),
                    detoxMinutes = item.totalDetoxMinutes.toInt(),
                    focusMinutes = item.totalFocusSeconds.toInt() / 60,
                    sessionsCount = item.sessionCount
                )
            }
            is MonthWeekStatsResult -> {
                ChartItem(
                    label = "${item.weekIndex + 1} нед",
                    detoxMinutes = item.totalDetoxMinutes.toInt(),
                    focusMinutes = (item.totalFocusSeconds / 60).toInt(),
                    sessionsCount = item.sessionCount
                )
            }
            is MonthStatsResult -> {
                ChartItem(
                    label = item.yearMonth,
                    detoxMinutes = item.totalDetoxMinutes.toInt(),
                    focusMinutes = (item.totalFocusSeconds / 60).toInt(),
                    sessionsCount = item.sessionCount
                )
            }
            else -> ChartItem(
                label = "",
                detoxMinutes = 0,
                focusMinutes = 0,
                sessionsCount = 0
            )
        }
    }

    private fun epochDayToRussianWeekDay(epochDay: Long): String {
        return when (LocalDate.ofEpochDay(epochDay).dayOfWeek) {
            DayOfWeek.MONDAY -> "пн"
            DayOfWeek.TUESDAY -> "вт"
            DayOfWeek.WEDNESDAY -> "ср"
            DayOfWeek.THURSDAY -> "чт"
            DayOfWeek.FRIDAY -> "пт"
            DayOfWeek.SATURDAY -> "сб"
            DayOfWeek.SUNDAY -> "вс"
        }
    }

    private fun toRussionWeekDay(day: Int): String {
        return when(day) {
            1 -> "пн"
            2 -> "вт"
            3 -> "ср"
            4 -> "чт"
            5 -> "пт"
            6 -> "сб"
            7 -> "вс"
            else -> ""
        }
    }
}