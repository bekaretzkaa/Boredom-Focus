package com.example.boredomfocus.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.boredomfocus.core.settings.domain.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("hour", 21)
        val minute = intent.getIntExtra("minute", 0)

        notificationHelper.showReminder()
        reminderScheduler.scheduleNext(hour, minute)
    }
}