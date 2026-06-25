package com.example.boredomfocus.feature.focussession

data class FocusUiState(
    val focusSeconds: Long = 0L,
    val focusRecord: Long = 0L,
    val previousFocusSeconds: Long? = null,
    val weekFocusRecord: Long? = null,
    val previousWeekFocusRecord: Long? = null,
    val monthFocusRecord: Long? = null,
    val previousMonthFocusRecord: Long? = null
)
