package com.example.boredomfocus.feature.settings.presentation

import com.example.boredomfocus.core.appconfig.domain.model.AppLanguage
import com.example.boredomfocus.core.appconfig.domain.model.AppSettings
import com.example.boredomfocus.core.appconfig.domain.model.DetoxDuration
import com.example.boredomfocus.core.appconfig.domain.model.Difficulty
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import com.example.boredomfocus.core.notification.ReminderScheduler
import com.example.boredomfocus.domain.model.AuthUser
import com.example.boredomfocus.domain.repository.AuthRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsViewModelTest {

    private val settings = MutableStateFlow(AppSettings(
        detoxDuration = DetoxDuration.FIVE_MINUTES,
        difficulty = Difficulty.BEGINNER,
        firstLaunch = 0,
        reminderEnabled = true,
        reminderHour = 12,
        reminderMinute = 0,
    ))
    private val language = MutableStateFlow(AppLanguage.EN)

    private val appSettingsRepository = mockk<AppSettingsRepository>(relaxed = true) {
        every { getSettings() } returns settings
        every { getLanguage() } returns language
    }
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            appSettingsRepository = appSettingsRepository,
            authRepository = authRepository,
            reminderScheduler = reminderScheduler
        )


    @Test
    fun `uiState contains correct data when user is signed in`() = runTest {
        every {
            authRepository.getCurrentUser()
        } returns flowOf(AuthUser(
            uid = "001",
            name = "Bekzat",
            email = "bekaretzka@gmail.com"
        ))

        val viewModel = createViewModel()

        val state = viewModel.uiState
            .filter { !it.isLoading }
            .first()

        assertEquals(DetoxDuration.FIVE_MINUTES, state.detoxDuration)
        assertEquals(Difficulty.BEGINNER, state.difficulty)
        assertTrue(state.isSignedIn)
        assertEquals("Bekzat", state.name)
        assertEquals("bekaretzka@gmail.com", state.email)
        assertEquals(12, state.reminderHour)
        assertEquals(0, state.reminderMinute)
        assertEquals(AppLanguage.EN, state.language)

    }

    @Test
    fun `uiState contains correct data when user is signed out`() = runTest {
        every {
            authRepository.getCurrentUser()
        } returns flowOf(null)

        val viewModel = createViewModel()

        val state = viewModel.uiState
            .filter { !it.isLoading }
            .first()

        assertEquals(DetoxDuration.FIVE_MINUTES, state.detoxDuration)
        assertEquals(Difficulty.BEGINNER, state.difficulty)
        assertFalse(state.isSignedIn)
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals(12, state.reminderHour)
        assertEquals(0, state.reminderMinute)
        assertEquals(AppLanguage.EN, state.language)
    }



    @Test
    fun `saveReminder schedules reminder when enabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.saveReminder(true, 12, 0)

        coVerify {
            viewModel.saveReminder(true, 12, 0)
            reminderScheduler.schedule(12, 0)
        }
    }

    @Test
    fun `saveReminder cancels schedule reminder when disabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.saveReminder(false, 12, 0)

        coVerify {
            viewModel.saveReminder(false, 12, 0)
            reminderScheduler.cancel()
        }
    }



    @Test
    fun `signOut calls auth repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.signOut()

        coVerify {
            authRepository.signOut()
        }
    }
}