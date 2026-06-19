package com.example.boredomfocus.feature.focussession

sealed interface FocusSessionEvent {
    data object DetoxFinished : FocusSessionEvent
    data object FocusStopped : FocusSessionEvent
}