package com.example.boredomfocus.feature.focussession

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.formatSeconds
import com.example.boredomfocus.core.permission.DndManager
import com.example.boredomfocus.core.settings.domain.model.DetoxDuration
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.model.SessionPhase
import com.example.boredomfocus.data.repository.AddSessionRepositoryImpl
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
    private val savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository,
    private val addSessionRepository: AddSessionRepositoryImpl,
    private val dndManager: DndManager
) : ViewModel() {

    val detoxDuration: DetoxDuration = savedStateHandle["detoxDuration"] ?: DetoxDuration.FIVE_MINUTES
    val difficulty: Difficulty = savedStateHandle["difficulty"] ?: Difficulty.BEGINNER
    val focusOnly: Boolean = savedStateHandle["focusOnly"] ?: false

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<FocusSessionEvent>(Channel.BUFFERED)
    val events: Flow<FocusSessionEvent> = _events.receiveAsFlow()

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
            _uiState.update { state ->
                state.copy(
                    detoxUiState = state.detoxUiState.copy(
                        selectedDetoxSeconds = 0
                    )
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    detoxUiState = state.detoxUiState.copy(
                        selectedDetoxSeconds = detoxDuration.minutes.toLong() * 60
                    )
                )
            }
//            startDetoxTimer((detoxDuration.minutes * 60).toLong())
            startDetoxTimer(40) // TODO
        }
    }


    private fun finishFailedSession() {
        viewModelScope.launch {
            addSessionRepository.finishSession(
                detoxMinutes = uiState.value.detoxUiState.selectedDetoxSeconds / 60,
                detoxSeconds = uiState.value.detoxUiState.detoxElapsedSeconds,
                focusSeconds = 0,
                completed = false,
                isFocusOnly = focusOnly,
                streakCounted = false
            )
        }
    }
    private suspend fun finishCompletedSession() {
        dndManager.onSessionEnd()

        if(focusOnly) {
            addSessionRepository.finishSession(
                detoxMinutes = 0,
                detoxSeconds = 0,
                focusSeconds = uiState.value.focusUiState.focusSeconds,
                completed = true,
                isFocusOnly = true,
                streakCounted = true
            )
        } else {
            addSessionRepository.finishSession(
                detoxMinutes = uiState.value.detoxUiState.selectedDetoxSeconds / 60,
                detoxSeconds = uiState.value.detoxUiState.selectedDetoxSeconds,
                focusSeconds = uiState.value.focusUiState.focusSeconds,
                completed = true,
                isFocusOnly = false,
                streakCounted = true
            )
        }
    }

    fun onInterruptDetoxClick() {
        pauseDetoxTimer()
        sendEvent(FocusSessionEvent.NavigateToStopDetoxDialog)
    }
    fun onConfirmStopDetoxClick() {
        finishFailedSession()
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
        viewModelScope.launch {
            finishCompletedSession()
            sendEvent(FocusSessionEvent.NavigateHome)
        }
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
        viewModelScope.launch {
            finishCompletedSession()
            loadStreak()
            stopFocusStopwatch()
        }
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

    private fun startDetoxTimer(totalSeconds: Long) {
        if(isDetoxRunning) return

        detoxDurationMillis = totalSeconds * 1000
        pausedDetoxRemainingMillis = null

        detoxEndTimeMillis = SystemClock.elapsedRealtime() + detoxDurationMillis

        _uiState.update { state ->
            state.copy(
                detoxUiState = state.detoxUiState.copy(
                    detoxElapsedSeconds = 0,
                    detoxRemainingSeconds = totalSeconds,
                    detoxProgress = 1f
                )
            )
        }

        viewModelScope.launch {
            dndManager.onSessionStart(difficulty, SessionPhase.DETOX)
            runDetoxTimer()
        }
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
        viewModelScope.launch {
            dndManager.onSessionEnd()
        }

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

                _uiState.update { state ->
                    state.copy(
                        detoxUiState = state.detoxUiState.copy(
                            detoxElapsedSeconds = elapsedSeconds,
                            detoxRemainingSeconds = remainingSeconds,
                            detoxProgress = remainingMillis.toFloat() / detoxDurationMillis
                        )
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
                _uiState.update { state ->
                    state.copy(
                        detoxUiState = state.detoxUiState.copy(
                            detoxElapsedSeconds = detoxDurationMillis / 1000,
                            detoxRemainingSeconds = 0,
                            detoxProgress = 0f
                        )
                    )
                }
            }
        }
    }


    private fun startFocusStopwatch() {
        if(isFocusRunning) return

        pausedFocusElapsedMillis = 0L
        focusStartMillis = SystemClock.elapsedRealtime()

        _uiState.update { state ->
            state.copy(
                focusUiState = state.focusUiState.copy(
                    focusSeconds = 0
                )
            )
        }
        viewModelScope.launch {
            dndManager.onPhaseChanged(difficulty, SessionPhase.FOCUS)
            runFocusStopwatch()
        }
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
        viewModelScope.launch {
            dndManager.onSessionEnd()
        }

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

                _uiState.update { state ->
                    state.copy(
                        focusUiState = state.focusUiState.copy(
                            focusSeconds = elapsedSeconds

//                            TEST
//                            focusSeconds = 4000
                        )
                    )
                }

                delay(1000)
            }
        }
    }
}