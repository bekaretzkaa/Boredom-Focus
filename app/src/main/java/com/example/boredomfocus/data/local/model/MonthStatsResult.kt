package com.example.boredomfocus.data.local.model

data class MonthStatsResult(
    val yearMonth: String,
    val monthStartDay: Long,
    val totalDetoxMinutes: Long,
    val totalFocusSeconds: Long,
    val sessionCount: Int
)
