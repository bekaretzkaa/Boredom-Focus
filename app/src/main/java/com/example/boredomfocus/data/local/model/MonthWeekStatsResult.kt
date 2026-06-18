package com.example.boredomfocus.data.local.model

data class MonthWeekStatsResult(
    val weekIndex: Int,
    val weekStartDay: Long,
    val weekEndDay: Long,
    val totalDetoxMinutes: Long,
    val totalFocusSeconds: Long,
    val sessionCount: Int
)
