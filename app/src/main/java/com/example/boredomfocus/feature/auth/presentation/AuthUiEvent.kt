package com.example.boredomfocus.feature.auth.presentation

sealed interface AuthUiEvent {

    data object LoginCompleted : AuthUiEvent

    data object OpenConfirmEmail : AuthUiEvent

    data object RegistrationCompleted : AuthUiEvent

}