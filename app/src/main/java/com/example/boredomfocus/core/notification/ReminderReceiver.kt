package com.example.boredomfocus.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
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