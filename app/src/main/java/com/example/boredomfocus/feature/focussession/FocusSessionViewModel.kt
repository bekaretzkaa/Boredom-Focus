package com.example.boredomfocus.feature.focussession

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.core.permission.DndManager
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.model.SessionPhase
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import com.example.boredomfocus.data.repository.AddSessionRepositoryImpl
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    val detoxDuration: DetoxDuration = savedStateHandle["detoxDuration"] ?: DetoxDuration.FIVE_MINUTES
    val difficulty: Difficulty = savedStateHandle["difficulty"] ?: Difficulty.BEGINNER
    val focusOnly: Boolean = savedStateHandle["focusOnly"] ?: false

    private var service: FocusSessionService? = null
    private var isBound = false

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<FocusSessionEvent>(Channel.BUFFERED)
    val events: Flow<FocusSessionEvent> = _events.receiveAsFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as FocusSessionService.LocalBinder
            service = localBinder.getService()
            isBound = true

            service!!.initSession(difficulty, focusOnly)

            viewModelScope.launch {
                service!!.uiState.collect { _uiState.value = it }
            }
            viewModelScope.launch {
                service!!.events.collect { _events.send(it) }
            }

            if (focusOnly) {
                service!!.startFocusStopwatch()
            } else {
//                service!!.startDetoxTimer((detoxDuration.minutes * 60).toLong())

//                TEST
                service!!.startDetoxTimer(30)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            service = null
        }
    }

    init {
        loadData()
        val intent = Intent(context, FocusSessionService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCleared() {
        super.onCleared()
        if(isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    companion object {
        private const val RESULT_ANIMATION_PLAYED_KEY = "result_animation_played"
        private const val DETOX_COMPLETED_ANIMATION_PLAYED_KEY = "detox_completed_animation_played"
        private const val DETOX_INTERRUPTED_ANIMATION_PLAYED_KEY = "detox_interrupted_animation_played"
    }

    fun shouldPlayResultAnimation(): Boolean {
        val alreadyPlayed = savedStateHandle[RESULT_ANIMATION_PLAYED_KEY] ?: false

        return if (alreadyPlayed) {
            false
        } else {
            savedStateHandle[RESULT_ANIMATION_PLAYED_KEY] = true
            true
        }
    }
    fun shouldPlayDetoxCompletedAnimation(): Boolean {
        val alreadyPlayed = savedStateHandle[DETOX_COMPLETED_ANIMATION_PLAYED_KEY] ?: false

        return if (alreadyPlayed) {
            false
        } else {
            savedStateHandle[DETOX_COMPLETED_ANIMATION_PLAYED_KEY] = true
            true
        }
    }
    fun shouldPlayDetoxInterruptedAnimation(): Boolean {
        val alreadyPlayed = savedStateHandle[DETOX_INTERRUPTED_ANIMATION_PLAYED_KEY] ?: false

        return if (alreadyPlayed) {
            false
        } else {
            savedStateHandle[DETOX_INTERRUPTED_ANIMATION_PLAYED_KEY] = true
            true
        }
    }

    private fun sendEvent(event: FocusSessionEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    fun onInterruptDetoxClick() {
        service?.pauseDetoxTimer()
        sendEvent(FocusSessionEvent.NavigateToStopDetoxDialog)
    }
    fun onConfirmStopDetoxClick() {
        service?.stopDetoxTimer(completed = false)
    }
    fun onDeclineStopDetoxClick() {
        service?.resumeDetoxTimer()
    }
    fun onStartFocusClick() {
        sendEvent(FocusSessionEvent.NavigateToStopwatch)
        service?.startFocusStopwatch()
    }
    fun onDetoxCompletedHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }
    fun onRestartDetoxClick() {
        sendEvent(FocusSessionEvent.NavigateToDetoxTimer)
        service?.startDetoxTimer((detoxDuration.minutes * 60).toLong())
    }
    fun onDetoxInterruptedHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }
    fun onStopFocusClick() {
        service?.pauseFocusStopwatch()
        sendEvent(FocusSessionEvent.NavigateToStopFocusDialog)
    }
    fun onConfirmStopFocusClick() {
        viewModelScope.launch {
            service?.stopFocusStopwatch(completed = true)
            loadStreak()
        }
    }
    fun onDeclineStopFocusClick() {
        service?.resumeFocusStopwatch()
    }
    fun onFocusResultHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }

    private fun loadData() {
        viewModelScope.launch {
            val record = sessionRepository.getAllTimeFocusRecord()
            val previousFocus = sessionRepository.getLastFocusTime()
            val focusRecords = sessionRepository.getFocusRecordBetween()

            _uiState.update { state ->
                state.copy(
                    focusUiState = state.focusUiState.copy(
                        focusRecord = record ?: 0,
                        previousFocusSeconds = previousFocus,
                        weekFocusRecord = focusRecords.currentWeek,
                        monthFocusRecord = focusRecords.currentMonth,
                    )

//                    TEST
//                    focusUiState = state.focusUiState.copy(
//                        previousFocusSeconds = 5,
//                        weekFocusRecord = null,
//                        monthFocusRecord = null,
//                        focusRecord = 10
//                    )
                )
            }
        }
    }

    private suspend fun loadStreak() {
        val streak = dailyStatsRepository.getCurrentStreak(LocalDate.now().toEpochDay())
        _uiState.update {
            it.copy(
                streakCount = streak
            )
        }
    }
}