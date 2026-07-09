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

    data object EmptySignUp : AuthUiStatus

    data object EmptySignIn : AuthUiStatus

    data class EmptyTwo(
        val name: Boolean,
        val email: Boolean,
        val password: Boolean,
        val confirmPassword: Boolean
    ) : AuthUiStatus

    data object PasswordMismatch : AuthUiStatus

    data object EmptyName : AuthUiStatus

    data object EmptyEmail : AuthUiStatus

    data object EmptyPassword : AuthUiStatus

    data object EmptyConfirmPassword : AuthUiStatus

    data object GoogleFailed : AuthUiStatus

}