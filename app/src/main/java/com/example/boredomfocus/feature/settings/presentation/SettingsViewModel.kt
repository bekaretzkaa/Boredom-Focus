package com.example.boredomfocus.feature.settings.presentation

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.notification.ReminderScheduler
import com.example.boredomfocus.core.settings.domain.model.AppLanguage
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import com.example.boredomfocus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val authRepository: AuthRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {


    val uiState: StateFlow<SettingsUiState> =
        combine(
            appSettingsRepository.getSettings(),
            authRepository.getCurrentUser(),
            appSettingsRepository.getLanguage()
        ) { settings, user, language ->
            SettingsUiState(
                isLoading = false,
                detoxDuration = settings.detoxDuration,
                difficulty = settings.difficulty,
                isSignedIn = user != null,
                name = user?.name ?: "",
                email = user?.email ?: "",
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
                language = language
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
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

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun saveReminder(
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        viewModelScope.launch {
            appSettingsRepository.saveReminder(enabled, hour, minute)

            if (enabled) {
                reminderScheduler.schedule(hour, minute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun changeLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appSettingsRepository.saveLanguage(language)

            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag)
            )
        }
    }
}