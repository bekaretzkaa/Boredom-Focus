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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val _events = MutableSharedFlow<FocusSessionEvent>()
    val events = _events.asSharedFlow()

    private var detoxJob: Job? = null
    private var detoxDurationMillis = 0L
    private var detoxEndTimeMillis = 0L
    private var detoxStartTimeMillis = 0L
    val isDetoxRunning: Boolean
        get() = detoxJob != null


    private var focusJob: Job? = null
    private var focusStartMillis = 0L
    val isFocusRunning: Boolean
        get() = focusJob != null

    init {
//        startDetoxTimer(detoxDuration.minutes * 60)
        startDetoxTimer(10)
    }

    private fun startDetoxTimer(totalSeconds: Int) {
        if(isDetoxRunning) return

        detoxDurationMillis = totalSeconds * 1000L
        detoxEndTimeMillis = SystemClock.elapsedRealtime() + detoxDurationMillis
        detoxStartTimeMillis = SystemClock.elapsedRealtime()

        _uiState.update {
            it.copy(
                selectedDetoxSeconds = totalSeconds,
                detoxProgress = 1f,
                isDetoxFinished = false
            )
        }

        detoxJob = viewModelScope.launch {
            while(isActive) {
                val remainingMillis = (detoxEndTimeMillis - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0)
                val totalElapsedSeconds = (SystemClock.elapsedRealtime() - detoxStartTimeMillis) / 1000
                val totalRemainingSeconds = remainingMillis / 1000

                _uiState.update {
                    it.copy(
                        detoxElapsedTimeText = formatSeconds(totalElapsedSeconds),
                        detoxTimeText = formatSeconds(totalRemainingSeconds),
                        detoxProgress = remainingMillis.toFloat() / detoxDurationMillis
                    )
                }

                if(remainingMillis <= 0) {
                    _uiState.update {
                        it.copy(isDetoxFinished = true)
                    }

                    _events.emit(FocusSessionEvent.DetoxFinished)
                    break
                }

                delay(16)
            }
            detoxJob = null
        }
    }

    fun startFocusStopwatch() {
        if(isFocusRunning) return
        focusStartMillis = SystemClock.elapsedRealtime()

        focusJob = viewModelScope.launch {
            while(isActive) {
                val elapsedSeconds = (SystemClock.elapsedRealtime() - focusStartMillis) / 1000

                _uiState.update {
                    it.copy(
                        focusSeconds = elapsedSeconds,
                        focusTimeText = formatSeconds(elapsedSeconds)
                    )
                }

                delay(1000)
            }
        }
    }

    fun stopFocus() {
        focusJob?.cancel()
        focusJob = null

        _uiState.update {
            it.copy(isFocusStopped = true)
        }

        viewModelScope.launch {
            _events.emit(FocusSessionEvent.FocusStopped)
        }
    }

    fun stopDetox() {
        detoxJob?.cancel()
        detoxJob = null

        _uiState.update {
            it.copy(isDetoxFinished = true)
        }

        viewModelScope.launch {
            _events.emit(FocusSessionEvent.FocusStopped)
        }
    }

    override fun onCleared() {
        super.onCleared()
        detoxJob?.cancel()
        focusJob?.cancel()
    }

}