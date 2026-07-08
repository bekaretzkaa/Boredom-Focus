package com.example.boredomfocus.feature.auth

import android.util.Log
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

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(name = name)
        }
    }
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

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update {
            it.copy(confirmPassword = password)
        }
    }

    fun onSignTypeChanged(isSignIn: Boolean) {
        _uiState.update {
            it.copy(
                isSignIn = isSignIn,
                name = "",
                confirmPassword = "",
            )
        }
    }

    fun signUp() {
        val name = uiState.value.name.trim()
        val email = uiState.value.email.trim()
        val password = uiState.value.password.trim()
        val confirmPassword = uiState.value.confirmPassword.trim()

        val emptyCount = listOf(
            name,
            email,
            password,
            confirmPassword
        ).count { it.isBlank() }

        when {
            emptyCount == 4 -> {
                _uiState.update {
                    it.copy(status = AuthUiStatus.EmptySignUp)
                }
                return
            }

            emptyCount >= 2 -> {
                _uiState.update {
                    it.copy(status = AuthUiStatus.EmptyTwo(
                        name = name.isBlank(),
                        email = email.isBlank(),
                        password = password.isBlank(),
                        confirmPassword = confirmPassword.isBlank()
                    ))
                }
                return
            }

            emptyCount == 1 -> {

                if (name.isBlank()) {
                    _uiState.update {
                        it.copy(status = AuthUiStatus.EmptyName)
                    }
                } else if (email.isBlank()) {
                    _uiState.update {
                        it.copy(status = AuthUiStatus.EmptyEmail)
                    }
                } else if (password.isBlank()) {
                    _uiState.update {
                        it.copy(status = AuthUiStatus.EmptyPassword)
                    }
                } else {
                    _uiState.update {
                        it.copy(status = AuthUiStatus.EmptyConfirmPassword)
                    }
                }
                return
            }

            password != confirmPassword -> {
                _uiState.update {
                    it.copy(status = AuthUiStatus.PasswordMismatch)
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AuthUiStatus.Loading)
            }

            when (val result = authRepository.signUp(name, email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            status = AuthUiStatus.Success,
                        )
                    }
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(status = result.error.toAuthUiStatus())
                    }
                }
            }
        }
    }

    fun signIn() {
        val email = uiState.value.email.trim()
        val password = uiState.value.password.trim()

        if (email.isBlank() && password.isBlank()) {
            _uiState.update {
                it.copy(status = AuthUiStatus.EmptySignIn)
            }
            return
        } else if (email.isBlank()) {
            _uiState.update {
                it.copy(status = AuthUiStatus.EmptyEmail)
            }
            return
        } else if (password.isEmpty()) {
            _uiState.update {
                it.copy(status = AuthUiStatus.EmptyPassword)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(status = AuthUiStatus.Loading)
            }

            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(status = AuthUiStatus.Success)
                    }
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(status = result.error.toAuthUiStatus())
                    }
                }
            }
        }
    }

    private fun AuthError.toAuthUiStatus(): AuthUiStatus {
        return when (this) {
            is AuthError.WeakPassword -> AuthUiStatus.WeakPassword
            is AuthError.EmailAlreadyExists -> AuthUiStatus.EmailAlreadyExists
            is AuthError.InvalidCredentials -> AuthUiStatus.InvalidCredentials
            is AuthError.UserNotFound -> AuthUiStatus.UserNotFound
            is AuthError.NetworkError -> AuthUiStatus.NetworkError
            is AuthError.Unknown -> AuthUiStatus.Unknown
        }
    }

}