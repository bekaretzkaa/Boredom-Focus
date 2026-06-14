package com.example.boredomfocus.feature.detox.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DetoxTimerViewModel : ViewModel() {

    private val _time = MutableStateFlow("00:00")
    val time: StateFlow<String> = _time
    private val _progress = MutableStateFlow(1f)
    val progress: StateFlow<Float> = _progress

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

    private var timerJob: Job? = null

    private var durationMillis = 0L
    private var endTimeMillis = 0L

    val isRunning: Boolean
        get() = timerJob != null

    fun startTimer(totalSeconds: Int) {
        if(isRunning) return

        durationMillis = totalSeconds * 1000L
        endTimeMillis = SystemClock.elapsedRealtime() + durationMillis

        startTicker()
    }

    private fun startTicker() {
        timerJob = viewModelScope.launch {
            while (isActive) {

                val remainingMillis = (endTimeMillis - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0)

                _progress.value = remainingMillis.toFloat() / durationMillis

                val totalSeconds = remainingMillis / 1000

                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60

                _time.value = String.format("%02d:%02d", minutes, seconds)

                if(remainingMillis <= 0) {
                    _finished.value = true
                    break
                }

                delay(16)
            }

            timerJob = null
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}