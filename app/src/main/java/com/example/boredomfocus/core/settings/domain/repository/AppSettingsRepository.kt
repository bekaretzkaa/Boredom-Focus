package com.example.boredomfocus.core.settings.domain.repository

import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {

    fun getSettings(): Flow<AppSettings>

    suspend fun getCurrentSettings(): AppSettings

    fun isOnboardingCompleted(): Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun saveDetoxDuration(detoxDuration: DetoxDuration)

    suspend fun saveDifficulty(difficulty: Difficulty)

    suspend fun ensureFirstLaunchDateExists(): Long

    suspend fun saveReminder(enabled: Boolean, hour: Int, minute: Int)

    suspend fun savePreviousInterruptionFilter(filter: Int)

    fun getPreviousInterruptionFilter(): Flow<Int>

    suspend fun clearPreviousInterruptionFilter()

    suspend fun isSessionRunning(): Boolean

    suspend fun setSessionRunning(running: Boolean)

    val notificationPermissionRequested: Flow<Boolean>

    suspend fun setNotificationPermissionRequested(requested: Boolean)
}