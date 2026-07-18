package com.example.boredomfocus.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.permission.DndManager
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val dndManager: DndManager
) : ViewModel() {

    val isOnboardingCompleted = appSettingsRepository.isOnboardingCompleted().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun checkDndMode() {
        viewModelScope.launch {
            dndManager.recoverIfSessionKilled(appSettingsRepository.isSessionRunning())
        }
    }
}