package com.example.boredomfocus.core.permission

import android.app.NotificationManager
import android.content.Context
import com.example.boredomfocus.core.appconfig.domain.model.Difficulty
import com.example.boredomfocus.core.appconfig.domain.model.SessionPhase
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: AppSettingsRepository
) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    val hasPermission: Boolean
        get() = notificationManager.isNotificationPolicyAccessGranted

    suspend fun onSessionStart(level: Difficulty, phase: SessionPhase) {
        if(!hasPermission) return

        val savedFilter = settingsRepository.getPreviousInterruptionFilter().first()
        if(savedFilter == -1) {
            settingsRepository.savePreviousInterruptionFilter(
                notificationManager.currentInterruptionFilter
            )
        }

        if(level != Difficulty.BEGINNER) {
            applyFilter(level, phase)
        }
    }

    fun onPhaseChanged(level: Difficulty, phase: SessionPhase) {
        if(!hasPermission) return
        if(level != Difficulty.BEGINNER) {
            applyFilter(level, phase)
        }
    }

    suspend fun onSessionEnd() {
        if(!hasPermission) return

        val savedFilter = settingsRepository.getPreviousInterruptionFilter().first()
        if(savedFilter == -1) return

        notificationManager.setInterruptionFilter(savedFilter)
        settingsRepository.clearPreviousInterruptionFilter()
    }

    suspend fun recoverIfSessionKilled(isSessionRunning: Boolean) {
        if(!isSessionRunning) return

        val saved = settingsRepository.getPreviousInterruptionFilter().first()
        if(saved != -1) {
            onSessionEnd()
        }
    }

    private fun applyFilter(level: Difficulty, phase: SessionPhase) {
        val filter = filterFor(level, phase)
        if(notificationManager.currentInterruptionFilter != filter) {
            notificationManager.setInterruptionFilter(filter)
        }
    }

    private fun filterFor(level: Difficulty, phase: SessionPhase): Int = when(level) {
        Difficulty.BEGINNER -> NotificationManager.INTERRUPTION_FILTER_ALL
        Difficulty.FIGHTER -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        Difficulty.HARDCORE -> when(phase) {
            SessionPhase.DETOX -> NotificationManager.INTERRUPTION_FILTER_NONE
            SessionPhase.FOCUS -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
        }
    }

}