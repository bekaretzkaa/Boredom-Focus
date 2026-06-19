package com.example.boredomfocus.feature.home.presentation

import com.example.boredomfocus.data.local.entity.DailyStatsEntity

data class HomeUIState(
    val isLoading: Boolean = true,
    val dailyStats: List<DailyStatsEntity?> = listOf(),
    val streakCount: Int = 0,
    val sessionCount: Int = 0,
    val focusRecord: Long = 0,
    val todayStreak: Boolean = false
)
