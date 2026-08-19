package com.example.boredomfocus.feature.home.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.boredomfocus.core.appconfig.domain.repository.AppSettingsRepository
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeViewModelTest {

    private val dailyStatsRepository = mockk<DailyStatsRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val appSettingsRepository = mockk<AppSettingsRepository>(relaxed = true)

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            savedStateHandle = SavedStateHandle(),
            dailyStatsRepository = dailyStatsRepository,
            sessionRepository = sessionRepository,
            appSettingsRepository = appSettingsRepository,
        )
    }

    @Test
    fun `shouldPlayHomeAnimation returns true only once`() {
        val viewModel = createViewModel()

        val firstResult = viewModel.shouldPlayHomeAnimation()
        val secondResult = viewModel.shouldPlayHomeAnimation()

        assertTrue(firstResult)
        assertFalse(secondResult)
    }


    @Test
    fun `init ensures stats from first launch date when no previous data`() {
        coEvery {
            appSettingsRepository.ensureFirstLaunchDateExists()
        } returns 100L

        coEvery {
            dailyStatsRepository.getLastStatsDate()
        } returns null

        val viewModel = createViewModel()

        coVerify {
            dailyStatsRepository.ensureStatsUntilToday(100L)
        }
    }


    @Test
    fun `uiState contains correct data`() = runTest {
        every {
            dailyStatsRepository.getDailyStatsBetween(any(), any())
        } returns flowOf(emptyList())

        every {
            dailyStatsRepository.getSessionCountBetween(any(), any())
        } returns flowOf(10)

        every {
            sessionRepository.getAllTimeFocusRecordFlow()
        } returns flowOf(3000L)

        coEvery {
            dailyStatsRepository.getCurrentStreak(any())
        } returns 5

        val viewModel = createViewModel()

        val state = viewModel.uiState
            .filter { !it.isLoading }
            .first()

        assertEquals(10, state.sessionCount)
        assertEquals(3000L, state.focusRecord)
        assertEquals(5, state.streakCount)
    }

}