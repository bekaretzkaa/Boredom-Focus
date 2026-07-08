package com.example.boredomfocus.feature.auth

sealed interface AuthUiStatus {

    data object Idle : AuthUiStatus

    data object Loading : AuthUiStatus

    data object Success : AuthUiStatus

    data object WeakPassword : AuthUiStatus

    data object EmailAlreadyExists : AuthUiStatus

    data object InvalidCredentials : AuthUiStatus

    data object UserNotFound : AuthUiStatus

    data object NetworkError : AuthUiStatus

    data object Unknown : AuthUiStatus

}