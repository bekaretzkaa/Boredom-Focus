package com.example.boredomfocus.permission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PermissionViewModel(application: Application): AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)

    private val _permissionStatus = MutableLiveData<PermissionStatus>()
    val permissionStatus: LiveData<PermissionStatus> = _permissionStatus

    fun refreshPermissions() {
        _permissionStatus.value = permissionManager.getPermissionStatus()
    }


    fun isPostNotificationsGranted() = permissionManager.isPostNotificationsGranted()
    fun isDoNotDisturbGranted() = permissionManager.isDoNotDisturbGranted()
    fun getDndSettingsIntent() = permissionManager.getDndSettingsIntent()
    fun requiresRuntimeNotificationPermission() = permissionManager.requiresRuntimeNotificationPermission()
}