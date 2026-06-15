package com.example.boredomfocus.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    val settings = appSettingsRepository.getSettings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun saveDetoxDuration(detoxDuration: DetoxDuration) {
        viewModelScope.launch {
            appSettingsRepository.saveDetoxDuration(detoxDuration)
        }
    }

    fun saveDifficulty(difficulty: Difficulty) {
        viewModelScope.launch {
            appSettingsRepository.saveDifficulty(difficulty)
        }
    }

}