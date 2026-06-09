package com.example.boredomfocus.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StopwatchViewModel : ViewModel() {

    private val _time = MutableStateFlow("00:00")
    val time: StateFlow<String> = _time
    private val _progress = MutableStateFlow(1f)
    val progress: StateFlow<Float> = _progress

    private var stopwatchJob: Job? = null
    private var startTimeMillis = 0L

    val isRunning: Boolean
        get() = stopwatchJob != null

    fun start() {
        if(isRunning) return

        startTimeMillis = SystemClock.elapsedRealtime()

        stopwatchJob = viewModelScope.launch {
            while(isActive) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startTimeMillis

                updateTime(elapsedMillis)

                delay(100)
            }
        }
    }

    fun stop() {
        stopwatchJob?.cancel()
        stopwatchJob = null
    }

    private fun updateTime(elapsedMillis: Long) {
        val totalSeconds = elapsedMillis / 1000

        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        _time.value = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}