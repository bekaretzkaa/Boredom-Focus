package com.example.boredomfocus.feature.home.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.getCalendarWeekRangeDay
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dailyStatsRepository: DailyStatsRepository,
    private val sessionRepository: SessionRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    companion object {
        private const val HOME_ANIMATION_PLAYED_KEY = "home_animation_played"
    }

    fun shouldPlayHomeAnimation(): Boolean {
        val alreadyPlayed = savedStateHandle[HOME_ANIMATION_PLAYED_KEY] ?: false

        return if (alreadyPlayed) {
            false
        } else {
            savedStateHandle[HOME_ANIMATION_PLAYED_KEY] = true
            true
        }
    }


    init {
        viewModelScope.launch {
            val firstLaunchDate = appSettingsRepository.ensureFirstLaunchDateExists()
            val lastStatsDate = dailyStatsRepository.getLastStatsDate()
            if(lastStatsDate == null) {
                dailyStatsRepository.ensureStatsUntilToday(firstLaunchDate)
            } else {
                dailyStatsRepository.ensureStatsUntilToday(lastStatsDate)
            }
        }
    }

    private val currentWeekRange = getCalendarWeekRangeDay(0)

    val uiState: StateFlow<HomeUiState> =
        combine(
            dailyStatsRepository.getDailyStatsBetween(currentWeekRange.startDay, currentWeekRange.endDay),
            dailyStatsRepository.getSessionCountBetween(currentWeekRange.startDay, currentWeekRange.endDay),
            sessionRepository.getAllTimeFocusRecordFlow(),
            ) { dailyStats, sessionCount, focusRecord ->

            val today = LocalDate.now().toEpochDay()
            val streakCount = dailyStatsRepository.getCurrentStreak(today)

            val statsByDate = dailyStats.associateBy { it.date }

            val updatedDailyStats: List<DailyStatsEntity?> =
                (currentWeekRange.startDay..currentWeekRange.endDay).map { day ->
                    statsByDate[day]
                }

            val todayStreak = statsByDate[today]?.streakCounted == true

            HomeUiState(
                isLoading = false,
                dailyStats = updatedDailyStats,
                streakCount = streakCount,
                sessionCount = sessionCount,
                focusRecord = focusRecord ?: 0,
                todayStreak = todayStreak
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeUiState()
        )
}