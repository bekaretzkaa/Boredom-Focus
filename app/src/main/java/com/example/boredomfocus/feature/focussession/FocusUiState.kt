package com.example.boredomfocus.feature.focussession

data class FocusUiState(
    val focusSeconds: Long = 0L,
    val focusRecord: Long = 0L,
    val previousFocusSeconds: Long = 0L,
    val weekFocusRecord: Long = 0L,
    val previousWeekFocusRecord: Long = 0L,
    val monthFocusRecord: Long = 0L,
    val previousMonthFocusRecord: Long = 0L
)
