package com.example.boredomfocus.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AuthUiEvent>()
    val event = _event.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(email = email)
        }
    }
    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(password = password)
        }
    }

    fun signUp() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password.trim()

        viewModelScope.launch {
            try {
                authRepository.signUp(email, password)

                _event.emit(AuthUiEvent.ShowMessage("Аккаунт создан"))
                _event.emit(AuthUiEvent.NavigateToBack)
            } catch(e: AuthException) {
                _event.emit(AuthUiEvent.ShowMessage(e.message ?: "Ошибка регистрации"))
            } finally {

            }
        }
    }

    fun signIn() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password.trim()

        viewModelScope.launch {
            try {
                authRepository.signIn(email, password)

                _event.emit(AuthUiEvent.ShowMessage("Успешный вход"))
                _event.emit(AuthUiEvent.NavigateToBack)
            } catch (e: AuthException) {
                _event.emit(AuthUiEvent.ShowMessage(e.message ?: "Ошибка входа"))
            } finally {

            }
        }
    }

}