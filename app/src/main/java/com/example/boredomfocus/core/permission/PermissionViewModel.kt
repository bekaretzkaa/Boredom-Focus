package com.example.boredomfocus.core.permission

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)
    private val _uiState = MutableStateFlow(permissionManager.getPermissionStatus())
    val uiState = _uiState.asStateFlow()

    private val dndReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshPermissions()
        }
    }

    init {
        refreshPermissions()
        application.registerReceiver(
            dndReceiver,
            IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(dndReceiver)
    }

    fun refreshPermissions() {
        _uiState.value = permissionManager.getPermissionStatus()
    }

    fun getDndSettingsIntent() = permissionManager.getDndSettingsIntent()

    fun requiresRuntimeNotificationPermission() =
        permissionManager.requiresRuntimeNotificationPermission()
}