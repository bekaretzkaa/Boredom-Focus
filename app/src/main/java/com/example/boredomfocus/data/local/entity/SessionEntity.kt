package com.example.boredomfocus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions"
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo("detox_minutes")
    val detoxMinutes: Long,
    @ColumnInfo("focus_seconds")
    val focusSeconds: Long,
    val date: Long,
    val completed: Boolean,
    @ColumnInfo("is_focus_only")
    val isFocusOnly: Boolean
)
