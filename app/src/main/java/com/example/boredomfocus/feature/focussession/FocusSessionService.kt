package com.example.boredomfocus.feature.focussession

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.boredomfocus.R
import com.example.boredomfocus.core.permission.DndManager
import com.example.boredomfocus.core.settings.domain.model.Difficulty
import com.example.boredomfocus.core.settings.domain.model.SessionPhase
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import com.example.boredomfocus.domain.repository.AddSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@AndroidEntryPoint
class FocusSessionService : Service() {

    @Inject lateinit var dndManager: DndManager
    @Inject lateinit var settingsRepository: AppSettingsRepository
    @Inject lateinit var addSessionRepository: AddSessionRepository


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): FocusSessionService = this@FocusSessionService
    }

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    private val _events = Channel<FocusSessionEvent>(Channel.BUFFERED)
    val events: Flow<FocusSessionEvent> = _events.receiveAsFlow()

    private var difficulty: Difficulty = Difficulty.BEGINNER
    private var focusOnly: Boolean = false

    private var sessionEndedGracefully = false



    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification("Сессия запускается..."))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }


    fun initSession(difficulty: Difficulty, focusOnly: Boolean) {
        this.difficulty = difficulty
        this.focusOnly = focusOnly
    }

    // PUBLIC Detox functions
    fun startDetoxTimer(totalSeconds: Long) {
        if(isDetoxRunning) return

        detoxDurationMillis = totalSeconds * 1000
        pausedDetoxRemainingMillis = null
        detoxEndTimeMillis = SystemClock.elapsedRealtime() + detoxDurationMillis
        sessionEndedGracefully = false

        _uiState.update { state ->
            state.copy(
                detoxUiState = state.detoxUiState.copy(
                    selectedDetoxSeconds = totalSeconds,
                    detoxElapsedSeconds = 0,
                    detoxRemainingSeconds = totalSeconds,
                    detoxProgress = 1f
                )
            )
        }

        serviceScope.launch {
            settingsRepository.setSessionRunning(true)
            dndManager.onSessionStart(difficulty, SessionPhase.DETOX)
            runDetoxTimer()
        }
    }
    fun pauseDetoxTimer() {
        if(!isDetoxRunning) return
        pausedDetoxRemainingMillis = (detoxEndTimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        detoxJob?.cancel()
        detoxJob = null
    }
    fun resumeDetoxTimer() {
        if(isDetoxRunning) return
        val remainingMillis = pausedDetoxRemainingMillis ?: return
        pausedDetoxRemainingMillis = null
        detoxEndTimeMillis = SystemClock.elapsedRealtime() + remainingMillis
        runDetoxTimer()
    }
    fun stopDetoxTimer(completed: Boolean) {
        serviceScope.launch {
            dndManager.onSessionEnd()
            settingsRepository.setSessionRunning(false)

            addSessionRepository.finishSession(
                detoxMinutes = uiState.value.detoxUiState.selectedDetoxSeconds / 60,
                detoxSeconds = uiState.value.detoxUiState.detoxElapsedSeconds,
                focusSeconds = 0,
                completed = completed,
                isFocusOnly = focusOnly,
                streakCounted = false
            )
        }

        sessionEndedGracefully = true
        detoxJob?.cancel()
        detoxJob = null
        pausedDetoxRemainingMillis = null
        maybeStopSelf()
    }

    // PUBLIC Focus functions
    fun startFocusStopwatch() {
        if(isFocusRunning) return

        pausedFocusElapsedMillis = 0L
        focusStartMillis = SystemClock.elapsedRealtime()
        sessionEndedGracefully = false

        _uiState.update { state ->
            state.copy(
                focusUiState = state.focusUiState.copy(
                    focusSeconds = 0
                )
            )
        }
        serviceScope.launch {
            if(focusOnly) {
                dndManager.onSessionStart(difficulty, SessionPhase.FOCUS)
            } else {
                dndManager.onPhaseChanged(difficulty, SessionPhase.FOCUS)
            }
            settingsRepository.setSessionRunning(true)
            runFocusStopwatch()
        }
    }
    fun pauseFocusStopwatch() {
        if(!isFocusRunning) return
        val currentElapsedMillis = SystemClock.elapsedRealtime() - focusStartMillis
        pausedFocusElapsedMillis += currentElapsedMillis
        focusJob?.cancel()
        focusJob = null
    }
    fun resumeFocusStopwatch() {
        if(isFocusRunning) return
        focusStartMillis = SystemClock.elapsedRealtime()
        runFocusStopwatch()
    }
    fun stopFocusStopwatch(completed: Boolean) {
        serviceScope.launch {
            dndManager.onSessionEnd()
            settingsRepository.setSessionRunning(false)

            addSessionRepository.finishSession(
                detoxMinutes = if (focusOnly) 0 else uiState.value.detoxUiState.selectedDetoxSeconds / 60,
                detoxSeconds = if (focusOnly) 0 else uiState.value.detoxUiState.detoxElapsedSeconds,
                focusSeconds = uiState.value.focusUiState.focusSeconds,
                completed = completed,
                isFocusOnly = focusOnly,
                streakCounted = completed
            )
        }
        sessionEndedGracefully = true
        focusJob?.cancel()
        focusJob = null
        pausedFocusElapsedMillis = 0L
        maybeStopSelf()
    }



    // PRIVATE part

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



    private fun runDetoxTimer() {
        detoxJob = serviceScope.launch {
            while(isActive) {
                val remainingMillis = (detoxEndTimeMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                val elapsedMillis = (detoxDurationMillis - remainingMillis).coerceAtLeast(0)

                _uiState.update { state ->
                    state.copy(
                        detoxUiState = state.detoxUiState.copy(
                            detoxElapsedSeconds = elapsedMillis / 1000,
                            detoxRemainingSeconds = remainingMillis / 1000,
                            detoxProgress = remainingMillis.toFloat() / detoxDurationMillis
                        )
                    )
                }

                updateNotification("Детокс: осталось ${formatTime(remainingMillis)}")

                if(remainingMillis <= 0) {
                    _events.send(FocusSessionEvent.NavigateToDetoxCompleted)
                    break
                }
                delay(16)
            }
            detoxJob = null
        }
    }

    private fun runFocusStopwatch() {
        focusJob = serviceScope.launch {
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

                updateNotification("Фокус: ${formatTime(totalElapsedMillis)}")
                delay(1000)
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun maybeStopSelf() {
        if (!isDetoxRunning && !isFocusRunning) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }


    // NOTIFICATION part
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Сессия детокса/фокуса", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Сессия активна")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "session_channel"
    }


    // EMERGENCY shutdown
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (sessionEndedGracefully) {
            super.onTaskRemoved(rootIntent)
            return
        }

        serviceScope.launch {
            try {
                withTimeout(5000) {
                    dndManager.onSessionEnd()
                    settingsRepository.setSessionRunning(false)
                }
            } catch (e: Exception) {
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}