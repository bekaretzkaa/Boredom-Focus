package com.example.boredomfocus.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
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
    val dailyStats: List<DailyStatsEntity?> = listOf()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    private val currentWeekRange = getCalendarWeekRange(-1)
    private val previousWeekRange = getCalendarWeekRange(-2)

    private val currentWeekRangeDay = getCalendarWeekRangeDay(-1)

    val uiState: StateFlow<StatisticsUiState> =
        combine(
            sessionRepository.getStatsSummaryBetween(currentWeekRange.startMillis, currentWeekRange.endMillis),
            sessionRepository.getStatsSummaryBetween(previousWeekRange.startMillis, previousWeekRange.endMillis),
            sessionRepository.getAllTimeFocusRecord(),
            dailyStatsRepository.getDailyStatsBetween(currentWeekRangeDay.startDay, currentWeekRangeDay.endDay)
        ) { statsSummary,statsSummaryLast, focusRecord, dailyStats ->

            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            updatedDailyStats.addAll(dailyStats)
            repeat(7 - dailyStats.size) {
                updatedDailyStats.add(null)
            }

            StatisticsUiState(
                statsSummary = statsSummary,
                statsSummaryLast = statsSummaryLast,
                allTimeFocusRecord = focusRecord,
                dailyStats = updatedDailyStats
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )
}