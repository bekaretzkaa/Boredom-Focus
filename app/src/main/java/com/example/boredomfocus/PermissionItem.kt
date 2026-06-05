package com.example.boredomfocus


data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val iconBackground: Int,
    var isGranted: Boolean = false,
)
