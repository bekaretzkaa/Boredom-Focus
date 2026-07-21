package com.example.boredomfocus.feature.focussession.presentation

import com.example.boredomfocus.feature.focussession.presentation.FocusUiState

data class FocusSessionUiState(
    val detoxUiState: DetoxUiState = DetoxUiState(),
    val focusUiState: FocusUiState = FocusUiState(),
    val streakCount: Int = 0,
)