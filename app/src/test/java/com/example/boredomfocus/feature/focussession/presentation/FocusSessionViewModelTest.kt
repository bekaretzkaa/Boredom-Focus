package com.example.boredomfocus.feature.focussession.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.example.boredomfocus.domain.model.FocusRecordPeriod
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import com.example.boredomfocus.rule.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class FocusSessionViewModelTest {

    private val context = mockk<Context>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val dailyStatsRepository = mockk<DailyStatsRepository>(relaxed = true)

    private lateinit var viewModel: FocusSessionViewModel

    @BeforeEach
    fun setup() {

        coEvery {
            sessionRepository.getAllTimeFocusRecord()
        } returns 3600

        coEvery {
            sessionRepository.getLastFocusTime()
        } returns 600

        coEvery {
            sessionRepository.getFocusRecordBetween()
        } returns FocusRecordPeriod(
            currentWeek = 1200,
            currentMonth = 3100
        )

        viewModel = FocusSessionViewModel(
            context,
            SavedStateHandle(),
            sessionRepository,
            dailyStatsRepository
        )

    }

    @Test
    fun `shouldPlayResultAnimation returns true on first call`() {
        val result = viewModel.shouldPlayResultAnimation()

        assertTrue(result)
    }

    @Test
    fun `shouldPlayResultAnimation return true only once`() {
        val firstResult = viewModel.shouldPlayResultAnimation()
        val secondResult = viewModel.shouldPlayResultAnimation()

        assertTrue(firstResult)
        assertFalse(secondResult)
    }


    @Test
    fun `shouldPlayDetoxCompletedAnimation`() {
        val firstResult = viewModel.shouldPlayDetoxCompletedAnimation()
        val secondResult = viewModel.shouldPlayDetoxCompletedAnimation()

        assertTrue(firstResult)
        assertFalse(secondResult)
    }

    @Test
    fun `shouldPlayDetoxInterruptedAnimation`() {
        val firstResult = viewModel.shouldPlayDetoxInterruptedAnimation()
        val secondResult = viewModel.shouldPlayDetoxInterruptedAnimation()

        assertTrue(firstResult)
        assertFalse(secondResult)
    }



    @Test
    fun `loadData updates UI state correctly`() {
        val state = viewModel.uiState.value.focusUiState

        assertEquals(3600, state.focusRecord)
        assertEquals(600, state.previousFocusSeconds)
        assertEquals(1200, state.weekFocusRecord)
        assertEquals(3100, state.monthFocusRecord)
    }

    @Test
    fun `loadData calls repository methods`() {

        coVerify {
            sessionRepository.getAllTimeFocusRecord()
            sessionRepository.getLastFocusTime()
            sessionRepository.getFocusRecordBetween()
        }
    }
}