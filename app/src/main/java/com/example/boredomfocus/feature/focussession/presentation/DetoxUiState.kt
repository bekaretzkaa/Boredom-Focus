package com.example.boredomfocus.feature.focussession.presentation

data class DetoxUiState(
    val selectedDetoxSeconds: Long = 0L,
    val detoxElapsedSeconds: Long = 0L,
    val detoxRemainingSeconds: Long = 0L,
    val detoxProgress: Float = 1f
)