package com.example.boredomfocus.feature.auth

sealed interface AuthUiEvent {

    data object LoginCompleted : AuthUiEvent

    data object OpenConfirmEmail : AuthUiEvent

    data object RegistrationCompleted : AuthUiEvent

}