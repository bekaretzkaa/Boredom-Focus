package com.example.boredomfocus.feature.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyStatsRepository: DailyStatsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val currentWeekRange = getCalendarWeekRangeDay(0)

    val uiState: StateFlow<HomeUIState> =
        combine(
            dailyStatsRepository.getDailyStatsBetween(currentWeekRange.startDay, currentWeekRange.endDay),
            dailyStatsRepository.getSessionCountBetween(currentWeekRange.startDay, currentWeekRange.endDay),
            sessionRepository.getAllTimeFocusRecord(),
            ) { dailyStats, sessionCount, focusRecord ->
            val updatedDailyStats = mutableListOf<DailyStatsEntity?>()
            updatedDailyStats.addAll(dailyStats)
            repeat(7 - dailyStats.size) {
                Log.d("HomeViewModel", "add null")
                updatedDailyStats.add(null)
            }
            val streakCount = dailyStatsRepository.getCurrentStreak(LocalDate.now().toEpochDay())

            HomeUIState(
                isLoading = false,
                dailyStats = updatedDailyStats,
                streakCount = streakCount,
                sessionCount = sessionCount,
                focusRecord = focusRecord ?: 0
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeUIState()
        )
}