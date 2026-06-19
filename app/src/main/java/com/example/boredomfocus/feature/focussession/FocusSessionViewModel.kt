package com.example.boredomfocus.feature.focussession

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.boredomfocus.core.common.formatSeconds
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
    private val sessionRepository: SessionRepository,
    private val dailyStatsRepository: DailyStatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FocusSessionEvent>()
    val events = _events.asSharedFlow()

    private var detoxJob: Job? = null
    private var detoxDurationMillis = 0L
    private var detoxEndTimeMillis = 0L
    val isDetoxRunning: Boolean
        get() = detoxJob != null


    private var focusJob: Job? = null
    private var focusStartMillist = 0L
    val isFocusRunning: Boolean
        get() = focusJob != null

    fun startDetoxTimer(totalSeconds: Int) {
        if(isDetoxRunning) return

        detoxDurationMillis = totalSeconds * 1000L
        detoxEndTimeMillis = SystemClock.elapsedRealtime() + detoxDurationMillis

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

                val totalRemainingSeconds = remainingMillis / 1000

                _uiState.update {
                    it.copy(
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
        focusStartMillist = SystemClock.elapsedRealtime()

        focusJob = viewModelScope.launch {
            while(isActive) {
                val elapsedSeconds = (SystemClock.elapsedRealtime() - focusStartMillist) / 1000

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

    override fun onCleared() {
        super.onCleared()
        detoxJob?.cancel()
        focusJob?.cancel()
    }

}