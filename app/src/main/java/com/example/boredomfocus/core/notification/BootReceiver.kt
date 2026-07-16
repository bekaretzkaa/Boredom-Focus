package com.example.boredomfocus.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = appSettingsRepository.getCurrentSettings()
                if (settings.reminderEnabled) {
                    reminderScheduler.schedule(settings.reminderHour, settings.reminderMinute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}