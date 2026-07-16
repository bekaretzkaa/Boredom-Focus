package com.example.boredomfocus.app

import android.app.Application
import com.example.boredomfocus.core.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BoredomFocusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        NotificationChannels.create(this)
    }

}