package com.example.boredomfocus.feature.statistics.presentation.model

data class ChartItem(
    val label: String,
    val detoxMinutes: Int,
    val focusMinutes: Int,
    val sessionsCount: Int
)