package com.example.boredomfocus.feature.focussession

data class FocusSessionUiState(
    val selectedDetoxSeconds: Long = 0L,
    val detoxElapsedSeconds: Long = 0L,
    val detoxRemainingSeconds: Long = 0L,
    val detoxProgress: Float = 1f,
    val focusSeconds: Long = 0L,
    val focusRecord: Long = 0L,
    val streakCount: Int = 0,
    val isNewRecord: Boolean = false
)