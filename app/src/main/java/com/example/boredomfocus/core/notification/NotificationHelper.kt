package com.example.boredomfocus.core.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.boredomfocus.R
import com.example.boredomfocus.core.notification.NotificationChannels.CHANNEL_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun showReminder() {

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannels.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Время детокс-сессии")
            .setContentText("Хорошее время начать сессию!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(1001, notification)
    }
}