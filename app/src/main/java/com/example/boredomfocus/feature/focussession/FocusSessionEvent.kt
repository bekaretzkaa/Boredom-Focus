package com.example.boredomfocus.feature.focussession

sealed interface FocusSessionEvent {
    data object NavigateToDetoxCompleted : FocusSessionEvent
    data object NavigateToDetoxInterrupted : FocusSessionEvent
    data object NavigateToFocusTimer : FocusSessionEvent
    data object NavigateToFocusCompleted : FocusSessionEvent
    data object NavigateHome : FocusSessionEvent
}