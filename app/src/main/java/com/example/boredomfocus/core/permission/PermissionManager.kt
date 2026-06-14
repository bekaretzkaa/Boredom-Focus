package com.example.boredomfocus.core.permission

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class PermissionStatus(
    val postNotifications: Boolean,
    val doNotDisturb: Boolean
) {
    val allGranted: Boolean get() = postNotifications && doNotDisturb
}

class PermissionManager(private val context: Context) {

    fun isPostNotificationsGranted(): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    fun isDoNotDisturbGranted(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun getPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            isPostNotificationsGranted(),
            isDoNotDisturbGranted()
        )
    }


    fun getDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    fun requiresRuntimeNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

}