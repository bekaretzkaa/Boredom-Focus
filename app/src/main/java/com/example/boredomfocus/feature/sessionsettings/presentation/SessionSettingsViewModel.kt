package com.example.boredomfocus.feature.sessionsettings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionSettingsUiState())
    val uiState: StateFlow<SessionSettingsUiState> = _uiState.asStateFlow()


    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().first()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    detoxDuration = settings.detoxDuration,
                    difficulty = settings.difficulty,
                    focusOnly = false
                )
            }
        }
    }

    fun selectDetoxDuration(duration: DetoxDuration) {
        _uiState.update {
            it.copy(detoxDuration = duration)
        }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _uiState.update {
            it.copy(difficulty = difficulty)
        }
    }

    fun toggleFocusOnly() {
        _uiState.update {
            it.copy(focusOnly = !it.focusOnly)
        }
    }

    fun setFocusOnly(focusOnly: Boolean) {
        _uiState.update {
            it.copy(focusOnly = focusOnly)
        }
    }
}