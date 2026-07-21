package com.example.boredomfocus.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.appconfig.domain.model.DetoxDuration
import com.example.boredomfocus.core.appconfig.domain.model.Difficulty
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _navigateToPage = MutableSharedFlow<Int>(0)
    val navigateToPage = _navigateToPage.asSharedFlow()

    fun onNavigateToPageRequested(pageIndex: Int) {
        viewModelScope.launch {
            _navigateToPage.emit(pageIndex)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            appSettingsRepository.setOnboardingCompleted(true)
        }
    }

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