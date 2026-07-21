package com.example.boredomfocus.feature.auth.presentation

sealed interface AuthResult<out T> {
    data class Success<T>(
        val data: T
    ) : AuthResult<T>

    data class Error(
        val error: AuthError
    ) : AuthResult<Nothing>

}