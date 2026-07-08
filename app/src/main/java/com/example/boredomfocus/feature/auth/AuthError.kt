package com.example.boredomfocus.feature.auth

sealed interface AuthError {

    data object WeakPassword : AuthError

    data object EmailAlreadyExists : AuthError

    data object InvalidCredentials : AuthError

    data object UserNotFound : AuthError

    data object NetworkError : AuthError

    data object Unknown : AuthError

}