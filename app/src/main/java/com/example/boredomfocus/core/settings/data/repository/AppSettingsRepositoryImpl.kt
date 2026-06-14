package com.example.boredomfocus.core.settings.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    private object Keys {
        val DETOX_DURATION = stringPreferencesKey("detox_duration")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { exception ->
                if(exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val detoxDuration = preferences[Keys.DETOX_DURATION]
                    ?.let { value ->
                        runCatching { DetoxDuration.valueOf(value) }.getOrNull()
                    }
                    ?: DetoxDuration.FIVE_MINUTES

                val difficulty = preferences[Keys.DIFFICULTY]
                    ?.let { value ->
                        runCatching { Difficulty.valueOf(value) }.getOrNull()
                    }
                    ?: Difficulty.BEGINNER

                AppSettings(
                    detoxDuration = detoxDuration,
                    difficulty = difficulty
                )
            }
    }

    override suspend fun saveDetoxDuration(detoxDuration: DetoxDuration) {
        dataStore.edit { preferences ->
            preferences[Keys.DETOX_DURATION] = detoxDuration.name
        }
    }

    override suspend fun saveDifficulty(difficulty: Difficulty) {
        dataStore.edit { preferences ->
            preferences[Keys.DIFFICULTY] = difficulty.name
        }
    }
}