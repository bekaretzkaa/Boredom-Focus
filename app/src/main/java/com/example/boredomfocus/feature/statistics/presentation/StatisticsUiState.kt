package com.example.boredomfocus.feature.statistics.presentation

import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import com.example.boredomfocus.feature.statistics.presentation.model.SessionListItem
import com.example.boredomfocus.feature.statistics.presentation.model.StatisticsPeriod
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary

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