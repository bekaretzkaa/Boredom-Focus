package com.example.boredomfocus.domain.model

data class FocusRecordPeriod(
    val currentWeek: Long? = null,
    val previousWeek: Long? = null,
    val currentMonth: Long? = null,
    val previousMonth: Long? = null
)
