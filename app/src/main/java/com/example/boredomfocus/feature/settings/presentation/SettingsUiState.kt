package com.example.boredomfocus.feature.settings.presentation

import com.example.boredomfocus.core.settings.domain.model.AppLanguage
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty

data class SettingsUiState(
    val isLoading: Boolean = true,
    val detoxDuration: DetoxDuration = DetoxDuration.FIVE_MINUTES,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val isSignedIn: Boolean = false,
    val name: String = "",
    val email: String = "",
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val language: AppLanguage = AppLanguage.RU
)