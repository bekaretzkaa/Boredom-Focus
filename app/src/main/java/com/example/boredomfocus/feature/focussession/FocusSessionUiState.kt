package com.example.boredomfocus.feature.focussession

data class FocusSessionUiState(
    val selectedDetoxSeconds: Int = 0,
    val detoxTimeText: String = "00:00",
    val detoxProgress: Float = 1f,
    val focusSeconds: Long = 0,
    val focusTimeText: String = "00:00",
    val isDetoxFinished: Boolean = false,
    val isFocusStopped: Boolean = false
)
