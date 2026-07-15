package com.example.boredomfocus.core.permission

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        val status = permissionManager.getPermissionStatus()
        _uiState.value = status
    }

    fun getDndSettingsIntent() = permissionManager.getDndSettingsIntent()

    fun requiresRuntimeNotificationPermission() =
        permissionManager.requiresRuntimeNotificationPermission()


    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(postNotifications = granted)
        }
        viewModelScope.launch {
            delay(300)
            refreshPermissions()
        }
    }
}