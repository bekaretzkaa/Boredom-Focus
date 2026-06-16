package com.example.boredomfocus.feature.statistics.presentation.model

data class StatsSummary(
    val bestFocus: Long,
    val averageFocus: Double,
    val totalSessions: Int,
    val completionRate: Double
)
