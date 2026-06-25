package com.example.boredomfocus.feature.focussession

data class FocusSessionUiState(
    val detoxUiState: DetoxUiState = DetoxUiState(),
    val focusUiState: FocusUiState = FocusUiState(),
    val streakCount: Int = 0,
)