package com.example.boredomfocus.feature.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignIn: Boolean = true,
    val status: AuthUiStatus = AuthUiStatus.Idle
)