package com.example.boredomfocus.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.RangeDays
import com.example.boredomfocus.core.common.RangeMillis
import com.example.boredomfocus.core.common.formatDateFromEpochMillis
import com.example.boredomfocus.core.common.getCalendarMonthRange
import com.example.boredomfocus.core.common.getCalendarMonthRangeDay
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.core.common.getLastThreeCalendarMonthsRangeDay
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
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
    val averageFocusTimePeriod: Long? = null
)
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

    private val allTimeRangeMillis = RangeMillis(0L, System.currentTimeMillis())
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
            dailyStatsRepository.getFocusStatsBetween(if(period == StatisticsPeriod.ALL_TIME) 0 else currentRangeDays.startDay, currentRangeDays.endDay)
        ) { dailyStats, periodStats, focusTime ->
            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            updatedDailyStats.addAll(dailyStats)
            repeat(7 - dailyStats.size) {
                updatedDailyStats.add(null)
            }

            val updatedPeriodStats = mutableListOf<ChartItem>()
            var averageFocusTime = 0L

            when(period) {
                StatisticsPeriod.WEEK -> {
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    averageFocusTime = focusTime / updatedPeriodStats.size
                    for(i in updatedPeriodStats.size+1..7) {
                        updatedPeriodStats.add(ChartItem(toRussionWeekDay(i), -1, -1, -1))
                    }
                }
                StatisticsPeriod.MONTH -> {
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    for(i in updatedPeriodStats.size+1..5) {
                        updatedPeriodStats.add(ChartItem("$i НЕД", -1, -1, -1))
                    }
                    averageFocusTime = focusTime / (LocalDate.now().toEpochDay() - currentRangeDays.startDay)
                }
                StatisticsPeriod.ALL_TIME -> {
                    for(i in periodStats.size..2) {
                        updatedPeriodStats.add(ChartItem(getMonthName(getYearMonthByOffset(i)), -1, -1, -1))
                    }
                    updatedPeriodStats.addAll(periodStats.map { toChartItem(it) })
                    averageFocusTime = focusTime / (LocalDate.now().toEpochDay() - (dailyStatsRepository.getFirstDate() ?: 0))
                }
            }

            DailyStatsGroupFlow(
                dailyStats = updatedDailyStats,
                periodStats = updatedPeriodStats,
                totalFocusTimePeriod = focusTime,
                averageFocusTimePeriod = averageFocusTime
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
                    label = "${item.weekIndex + 1} НЕД",
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

    private fun epochDayToRussianWeekDay(epochDay: Long): String {
        return when (LocalDate.ofEpochDay(epochDay).dayOfWeek) {
            DayOfWeek.MONDAY -> "ПН"
            DayOfWeek.TUESDAY -> "ВТ"
            DayOfWeek.WEDNESDAY -> "СР"
            DayOfWeek.THURSDAY -> "ЧТ"
            DayOfWeek.FRIDAY -> "ПТ"
            DayOfWeek.SATURDAY -> "СБ"
            DayOfWeek.SUNDAY -> "ВС"
        }
    }

    private fun toRussionWeekDay(day: Int): String {
        return when(day) {
            1 -> "ПН"
            2 -> "ВТ"
            3 -> "СР"
            4 -> "ЧТ"
            5 -> "ПТ"
            6 -> "СБ"
            7 -> "ВС"
            else -> ""
        }
    }

    private fun getMonthName(yearMonth: String): String {
        return YearMonth.parse(yearMonth)
            .month
            .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
    }

    private fun getYearMonthByOffset(
        monthOffset: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return YearMonth.now(zoneId)
            .minusMonths(monthOffset.toLong())
            .toString()
    }
}