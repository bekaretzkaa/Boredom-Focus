package com.example.boredomfocus.feature.sessionsettings.presentation

import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty

data class SessionSettingsUiState(
    val detoxDuration: DetoxDuration = DetoxDuration.FIVE_MINUTES,
    val difficulty: Difficulty = Difficulty.BEGINNER,
    val focusOnly: Boolean = false
)