package com.example.boredomfocus.core.settings.data.repository

import androidx.activity.result.PickVisualMediaRequest
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.boredomfocus.core.settings.domain.model.AppSettings
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    private object Keys {
        val DETOX_DURATION = stringPreferencesKey("detox_duration")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FIRST_LAUNCH = longPreferencesKey("first_launch")
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

                val firstLaunchDate = preferences[Keys.FIRST_LAUNCH]

                AppSettings(
                    detoxDuration = detoxDuration,
                    difficulty = difficulty,
                    firstLaunch = firstLaunchDate
                )
            }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if(exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[Keys.ONBOARDING_COMPLETED] ?: false
            }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
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

    override suspend fun ensureFirstLaunchDateExists(): Long {
        val today = LocalDate.now().toEpochDay()
        var firstLaunchDate = today

        dataStore.edit { preferences ->
            val savedData = preferences[Keys.FIRST_LAUNCH]

            if(savedData == null) {
                preferences[Keys.FIRST_LAUNCH] = today
                firstLaunchDate = today
            } else {
                firstLaunchDate = savedData
            }
        }

        return firstLaunchDate
    }
}