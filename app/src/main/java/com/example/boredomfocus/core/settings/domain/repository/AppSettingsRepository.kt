package com.example.boredomfocus.core.settings.domain.repository

import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {

    fun getSettings(): Flow<AppSettings>

    suspend fun saveDetoxDuration(detoxDuration: DetoxDuration)

    suspend fun saveDifficulty(difficulty: Difficulty)

}