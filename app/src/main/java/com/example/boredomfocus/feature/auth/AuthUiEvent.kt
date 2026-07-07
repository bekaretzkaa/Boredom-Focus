package com.example.boredomfocus.feature.auth

sealed interface AuthUiEvent {

    data object NavigateToBack : AuthUiEvent

    data class ShowMessage(val message: String) : AuthUiEvent

}