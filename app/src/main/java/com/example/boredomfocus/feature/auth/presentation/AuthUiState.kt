package com.example.boredomfocus.feature.auth.presentation

import com.example.boredomfocus.feature.auth.presentation.AuthUiStatus

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignIn: Boolean = true,
    val status: AuthUiStatus = AuthUiStatus.Idle,
    val isEmailVerified: Boolean = false
)