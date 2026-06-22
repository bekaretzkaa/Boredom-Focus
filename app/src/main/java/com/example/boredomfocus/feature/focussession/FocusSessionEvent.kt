package com.example.boredomfocus.feature.focussession

sealed interface FocusSessionEvent {
    data object NavigateToStopDetoxDialog : FocusSessionEvent
    data object NavigateToDetoxCompleted : FocusSessionEvent
    data object NavigateToDetoxInterrupted : FocusSessionEvent


    data object NavigateToStopwatch : FocusSessionEvent
    data object NavigateToStopFocusDialog : FocusSessionEvent
    data object NavigateToFocusResult : FocusSessionEvent

    data object NavigateHome : FocusSessionEvent
    data object NavigateToDetoxTimer : FocusSessionEvent
}