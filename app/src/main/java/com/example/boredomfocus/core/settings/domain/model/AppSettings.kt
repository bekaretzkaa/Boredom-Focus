package com.example.boredomfocus.core.settings.domain.model

data class AppSettings(
    val detoxDuration: DetoxDuration = DetoxDuration.FIVE_MINUTES,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val firstLaunch: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
)