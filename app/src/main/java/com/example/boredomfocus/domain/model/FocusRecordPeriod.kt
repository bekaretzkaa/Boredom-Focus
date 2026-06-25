package com.example.boredomfocus.domain.model

data class FocusRecordPeriod(
    val currentWeek: Long = 0L,
    val previousWeek: Long = 0L,
    val currentMonth: Long = 0L,
    val previousMonth: Long = 0L
)
