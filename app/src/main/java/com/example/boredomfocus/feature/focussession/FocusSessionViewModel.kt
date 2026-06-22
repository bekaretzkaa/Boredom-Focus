package com.example.boredomfocus.feature.focussession

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import com.example.boredomfocus.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    val detoxDuration: DetoxDuration = savedStateHandle["detoxDuration"] ?: DetoxDuration.FIVE_MINUTES
    val difficulty: Difficulty = savedStateHandle["difficulty"] ?: Difficulty.BEGINNER
    val focusOnly: Boolean = savedStateHandle["focusOnly"] ?: false

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<FocusSessionEvent>(Channel.BUFFERED)
    val events: Flow<FocusSessionEvent> = _events.receiveAsFlow()

    private fun sendEvent(event: FocusSessionEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    private var detoxJob: Job? = null
    private var detoxDurationMillis = 0L
    private var detoxEndTimeMillis = 0L
    private var pausedDetoxRemainingMillis: Long? = null
    val isDetoxRunning: Boolean
        get() = detoxJob?.isActive == true


    private var focusJob: Job? = null
    private var focusStartMillis = 0L
    private var pausedFocusElapsedMillis = 0L
    val isFocusRunning: Boolean
        get() = focusJob?.isActive == true

    override fun onCleared() {
        super.onCleared()
        detoxJob?.cancel()
        focusJob?.cancel()
    }

    init {
        loadData()

        if(focusOnly) {
            startFocusStopwatch()
            _uiState.update {
                it.copy(
                    selectedDetoxSeconds = 0
                )
            }
        } else {
//            startDetoxTimer((detoxDuration.minutes * 60).toLong())
            startDetoxTimer(10) // TODO
        }
    }


    fun onInterruptDetoxClick() {
        pauseDetoxTimer()
        sendEvent(FocusSessionEvent.NavigateToStopDetoxDialog)
    }
    fun onConfirmStopDetoxClick() {
        stopDetoxTimer()
    }
    fun onDeclineStopDetoxClick() {
        resumeDetoxTimer()
    }
    fun onStartFocusClick() {
        sendEvent(FocusSessionEvent.NavigateToStopwatch)
        startFocusStopwatch()
    }
    fun onDetoxCompletedHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }
    fun onRestartDetoxClick() {
        sendEvent(FocusSessionEvent.NavigateToDetoxTimer)
        startDetoxTimer((detoxDuration.minutes * 60).toLong())
    }
    fun onDetoxInterruptedHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }
    fun onStopFocusClick() {
        pauseFocusStopwatch()
        sendEvent(FocusSessionEvent.NavigateToStopFocusDialog)
    }
    fun onConfirmStopFocusClick() {
        stopFocusStopwatch()
    }
    fun onDeclineStopFocusClick() {
        resumeFocusStopwatch()
    }
    fun onFocusResultHomeClick() {
        sendEvent(FocusSessionEvent.NavigateHome)
    }

    private fun loadData() {
        viewModelScope.launch {
            val record = sessionRepository.getAllTimeFocusRecord()
            val previousFocus = sessionRepository.getLastFocusTime()
            val streak = dailyStatsRepository.getCurrentStreak(LocalDate.now().toEpochDay())
            _uiState.update {
                it.copy(
                    focusRecord = record ?: 0,
                    streakCount = streak,
                    previousFocusSeconds = previousFocus ?: 0
                )
            }
        }
    }

    private fun startDetoxTimer(totalSeconds: Long) {
        if(isDetoxRunning) return

        detoxDurationMillis = totalSeconds * 1000
        pausedDetoxRemainingMillis = null

        detoxEndTimeMillis = SystemClock.elapsedRealtime() + detoxDurationMillis

        _uiState.update {
            it.copy(
                selectedDetoxSeconds = totalSeconds,
                detoxElapsedSeconds = 0,
                detoxRemainingSeconds = totalSeconds,
                detoxProgress = 1f
            )
        }

        runDetoxTimer()
    }
    private fun pauseDetoxTimer() {
        if(!isDetoxRunning) return

        pausedDetoxRemainingMillis = (detoxEndTimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0)

        detoxJob?.cancel()
        detoxJob = null
    }
    private fun resumeDetoxTimer() {
        if(isDetoxRunning) return

        val remainingMillis = pausedDetoxRemainingMillis ?: return

        pausedDetoxRemainingMillis = null
        detoxEndTimeMillis = SystemClock.elapsedRealtime() + remainingMillis

        runDetoxTimer()
    }
    private fun stopDetoxTimer() {
        detoxJob?.cancel()
        detoxJob = null
        pausedDetoxRemainingMillis = null
    }
    private fun runDetoxTimer() {
        detoxJob = viewModelScope.launch {
            while(isActive) {
                val remainingMillis = (detoxEndTimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                val elapsedMillis = (detoxDurationMillis - remainingMillis).coerceAtLeast(0)

                val elapsedSeconds = elapsedMillis / 1000
                val remainingSeconds = remainingMillis / 1000

                _uiState.update {
                    it.copy(
                        detoxElapsedSeconds = elapsedSeconds,
                        detoxRemainingSeconds = remainingSeconds,
                        detoxProgress = remainingMillis.toFloat() / detoxDurationMillis
                    )
                }

                if(remainingMillis <= 0) {
                    sendEvent(FocusSessionEvent.NavigateToDetoxCompleted)
                    break
                }

                delay(16)
            }

            detoxJob = null

            if(pausedDetoxRemainingMillis == null) {
                _uiState.update {
                    it.copy(
                        detoxElapsedSeconds = detoxDurationMillis / 1000,
                        detoxRemainingSeconds = 0,
                        detoxProgress = 0f
                    )
                }
            }
        }
    }


    private fun startFocusStopwatch() {
        if(isFocusRunning) return

        pausedFocusElapsedMillis = 0L
        focusStartMillis = SystemClock.elapsedRealtime()

        _uiState.update {
            it.copy(
                focusSeconds = 0
            )
        }

        runFocusStopwatch()
    }
    private fun pauseFocusStopwatch() {
        if(!isFocusRunning) return

        val currentElapsedMillis = SystemClock.elapsedRealtime() - focusStartMillis
        pausedFocusElapsedMillis += currentElapsedMillis

        focusJob?.cancel()
        focusJob = null
    }
    private fun resumeFocusStopwatch() {
        if(isFocusRunning) return

        focusStartMillis = SystemClock.elapsedRealtime()

        runFocusStopwatch()
    }
    private fun stopFocusStopwatch() {
        focusJob?.cancel()
        focusJob = null
        pausedFocusElapsedMillis = 0L
    }
    private fun runFocusStopwatch() {
        focusJob = viewModelScope.launch {
            while(isActive) {
                val currentElapsedMillis = SystemClock.elapsedRealtime() - focusStartMillis
                val totalElapsedMillis = pausedFocusElapsedMillis + currentElapsedMillis

                val elapsedSeconds = totalElapsedMillis / 1000

                _uiState.update {
                    it.copy(
                        focusSeconds = elapsedSeconds
                    )
                }

                delay(1000)
            }
        }
    }
}