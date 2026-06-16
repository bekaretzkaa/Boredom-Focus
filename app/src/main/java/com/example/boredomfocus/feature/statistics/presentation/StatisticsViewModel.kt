package com.example.boredomfocus.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.formatDateFromEpochMillis
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val statsSummary: StatsSummary? = null,
    val statsSummaryLast: StatsSummary? = null,
    val allTimeFocusRecord: Long? = null,
    val lastSessions: List<SessionListItem> = listOf(),
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val totalFocusTimePeriod: Long? = null,
)

data class SessionGroupFlow(
    val statsSummary: StatsSummary? = null,
    val statsSummaryLast: StatsSummary? = null,
    val allTimeFocusRecord: Long? = null,
    val lastSessions: List<SessionListItem> = listOf()
)

data class DailyStatsGroupFlow(
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val totalFocusTimePeriod: Long? = null,
    val averageFocusTimePeriod: Long? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    private val currentWeekRange = getCalendarWeekRange(-1)
    private val previousWeekRange = getCalendarWeekRange(-2)
    private val currentWeekRangeDay = getCalendarWeekRangeDay(-1)

    private val sessionGroup =
        combine(
            sessionRepository.getStatsSummaryBetween(currentWeekRange.startMillis, currentWeekRange.endMillis),
            sessionRepository.getStatsSummaryBetween(previousWeekRange.startMillis, previousWeekRange.endMillis),
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

    private val dailyStatsGroup =
        combine(
            dailyStatsRepository.getDailyStatsBetween(currentWeekRangeDay.startDay, currentWeekRangeDay.endDay),
            dailyStatsRepository.getFocusWeekStatsBetween(currentWeekRange.startMillis, currentWeekRange.endMillis)
        ) { dailyStats, focusTime ->
            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            updatedDailyStats.addAll(dailyStats)
            repeat(7 - dailyStats.size) {
                updatedDailyStats.add(null)
            }
            DailyStatsGroupFlow(
                dailyStats = updatedDailyStats,
                totalFocusTimePeriod = focusTime,
                averageFocusTimePeriod = focusTime / 7
            )
        }

    val uiState: StateFlow<StatisticsUiState> =
        combine(
            sessionGroup,
            dailyStatsGroup
        ) { sessionGroup, dailyStatsGroup ->
            StatisticsUiState(
                statsSummary = sessionGroup.statsSummary,
                statsSummaryLast = sessionGroup.statsSummaryLast,
                allTimeFocusRecord = sessionGroup.allTimeFocusRecord,
                lastSessions = sessionGroup.lastSessions,
                dailyStats = dailyStatsGroup.dailyStats,
                totalFocusTimePeriod = dailyStatsGroup.totalFocusTimePeriod
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StatisticsUiState()
        )
}