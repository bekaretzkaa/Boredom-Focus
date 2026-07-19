package com.example.boredomfocus.feature.statistics.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.R
import com.example.boredomfocus.core.common.RangeDays
import com.example.boredomfocus.core.common.RangeMillis
import com.example.boredomfocus.core.common.epochDayToDayOfWeekIndex
import com.example.boredomfocus.core.common.epochDayToRussianWeekDay
import com.example.boredomfocus.core.common.epochMillisToTime
import com.example.boredomfocus.core.common.formatDateFromEpochMillis
import com.example.boredomfocus.core.common.getCalendarMonthRange
import com.example.boredomfocus.core.common.getCalendarMonthRangeDay
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.core.common.getCurrentMonthWeeksCount
import com.example.boredomfocus.core.common.getLastThreeCalendarMonthsRangeDay
import com.example.boredomfocus.core.common.getMonthName
import com.example.boredomfocus.core.common.getYearMonthByOffset
import com.example.boredomfocus.core.common.toRussianWeekDay
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.model.MonthStatsResult
import com.example.boredomfocus.data.local.model.MonthWeekStatsResult
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.feature.statistics.presentation.model.StatisticsPeriod
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
private data class SessionGroupFlow(
    val statsSummary: StatsSummary? = null,
    val statsSummaryLast: StatsSummary? = null,
    val allTimeFocusRecord: Long? = null,
    val lastSessions: List<SessionListItem> = listOf()
)

private data class DailyStatsGroupFlow(
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val periodStats: List<ChartItem> = listOf(),
    val totalFocusTimePeriod: Long? = null,
    val averageFocusTimePeriod: Long? = null,
    val daysWithoutSession: Int = 0
)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    application: Application,
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : AndroidViewModel(application) {

    private val currentWeekRangeMillis = getCalendarWeekRange(0)
    private val previousWeekRangeMillis = getCalendarWeekRange(-1)
    private val currentWeekDaysRangeDays = getCalendarWeekRangeDay(0)

    private val currentMonthRangeMillis = getCalendarMonthRange(0)
    private val previousMonthRangeMillis = getCalendarMonthRange(-1)
    private val currentMonthDaysRangeDays = getCalendarMonthRangeDay(0)

    private val allTimeRangeMillis = RangeMillis(0L, Long.MAX_VALUE)
    private val currentYearDaysRangeDays = getLastThreeCalendarMonthsRangeDay()

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
                        currentRangeMillis = allTimeRangeMillis,
                        previousRangeMillis = allTimeRangeMillis,
                        currentRangeDays = currentYearDaysRangeDays,
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
            sessionRepository.getAllTimeFocusRecordFlow(),
            sessionRepository.getLastSessions(currentRangeMillis.startMillis, currentRangeMillis.endMillis)
        ) { statsSummary,statsSummaryLast, focusRecord, lastSessions ->

            val updatedLastSessions = mutableListOf<SessionListItem>()
            lastSessions.forEach { entity ->
                if(SessionListItem.Header(formatDateFromEpochMillis(getApplication(),entity.date)) in updatedLastSessions) {
                    updatedLastSessions.add(SessionListItem.Session(
                        entity.detoxMinutes.toInt(),
                        entity.detoxSeconds.toInt(),
                        entity.focusSeconds.toInt(),
                        epochMillisToTime(entity.date),
                        entity.completed
                    ))
                } else {
                    updatedLastSessions.add(SessionListItem.Header(formatDateFromEpochMillis(getApplication() ,entity.date)))
                    updatedLastSessions.add(SessionListItem.Session(
                        entity.detoxMinutes.toInt(),
                        entity.detoxSeconds.toInt(),
                        entity.focusSeconds.toInt(),
                        epochMillisToTime(entity.date),
                        entity.completed
                    ))
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
            dailyStatsRepository.getFocusStatsBetween(if(period == StatisticsPeriod.ALL_TIME) 0 else currentRangeDays.startDay, currentRangeDays.endDay),
            dailyStatsRepository.getDaysWithoutSession(LocalDate.now().toEpochDay())
        ) { dailyStats, periodStats, focusTime, daysWithoutSession ->
            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            for(i in 1 until epochDayToDayOfWeekIndex(dailyStats.first().date)) {
                updatedDailyStats.add(null)
            }
            updatedDailyStats.addAll(dailyStats)
            for(i in epochDayToDayOfWeekIndex(dailyStats.last().date)+1..7) {
                updatedDailyStats.add(null)
            }

            val updatedPeriodStats = mutableListOf<ChartItem>()
            var averageFocusTime = 0L

            when(period) {
                StatisticsPeriod.WEEK -> {
                    val first = periodStats.first() as DailyStatsEntity
                    val last = periodStats.last() as DailyStatsEntity
                    for(i in 1 until epochDayToDayOfWeekIndex(first.date)) {
                        updatedPeriodStats.add(ChartItem(toRussianWeekDay(i), -1, -1, -1))
                    }
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    averageFocusTime = focusTime / if(updatedPeriodStats.size == 0) 1 else updatedPeriodStats.size
                    for(i in epochDayToDayOfWeekIndex(last.date)+1..7) {
                        updatedPeriodStats.add(ChartItem(toRussianWeekDay(i), -1, -1, -1))
                    }
                }
                StatisticsPeriod.MONTH -> {
                    val first = periodStats.first() as MonthWeekStatsResult
                    val last = periodStats.last() as MonthWeekStatsResult
                    for(i in 1..first.weekIndex) {
                        updatedPeriodStats.add(ChartItem(application.getString(R.string.statistics_week_number, i), -1, -1, -1))
                    }
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    for(i in last.weekIndex+2..getCurrentMonthWeeksCount()) {
                        updatedPeriodStats.add(ChartItem(application.getString(R.string.statistics_week_number, i), -1, -1, -1))
                    }
                    val averageFocusTimeDivider = (LocalDate.now().toEpochDay() - currentRangeDays.startDay)
                    averageFocusTime = focusTime / if(averageFocusTimeDivider == 0L) 1 else averageFocusTimeDivider
                }
                StatisticsPeriod.ALL_TIME -> {
                    for(i in periodStats.size..2) {
                        updatedPeriodStats.add(ChartItem(getMonthName(getYearMonthByOffset(i)), -1, -1, -1))
                    }
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    val averageFocusTimeDivider = (LocalDate.now().toEpochDay() - (dailyStatsRepository.getFirstDate() ?: 0))
                    averageFocusTime = focusTime / if(averageFocusTimeDivider == 0L) 1 else averageFocusTimeDivider
                }
            }

            DailyStatsGroupFlow(
                dailyStats = updatedDailyStats,
                periodStats = updatedPeriodStats,
                totalFocusTimePeriod = focusTime,
                averageFocusTimePeriod = averageFocusTime,
                daysWithoutSession = daysWithoutSession
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
                averageFocusTimePeriod = dailyStatsGroup.averageFocusTimePeriod,
                daysWithoutSession = dailyStatsGroup.daysWithoutSession
            )

//          TEST
//            StatisticsUiState(
//                selectedPeriod = period,
//                isLoading = false,
//                statsSummary = StatsSummary(
//                    bestFocus = 150,
//                    averageFocus = 60.0,
//                    totalSessions = 3,
//                    completionRate = 50.0
//                ),
//                statsSummaryLast = StatsSummary(
//                    bestFocus = 300,
//                    averageFocus = 120.0,
//                    totalSessions = 7,
//                    completionRate = 89.0
//                ),
//                allTimeFocusRecord = 500,
//                lastSessions = sessionGroup.lastSessions,
//                dailyStats = dailyStatsGroup.dailyStats,
//                periodStats = dailyStatsGroup.periodStats,
//                totalFocusTimePeriod = dailyStatsGroup.totalFocusTimePeriod,
//                averageFocusTimePeriod = dailyStatsGroup.averageFocusTimePeriod
//            )
        }
    }

    private fun toChartItem(item: Any): ChartItem {
        return when (item) {
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
                    label = application.getString(R.string.statistics_week_number, item.weekIndex+1),
                    detoxMinutes = item.totalDetoxMinutes.toInt(),
                    focusMinutes = (item.totalFocusSeconds / 60).toInt(),
                    sessionsCount = item.sessionCount
                )
            }

            is MonthStatsResult -> {
                ChartItem(
                    label = getMonthName(item.yearMonth),
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
}