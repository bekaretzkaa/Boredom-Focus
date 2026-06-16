package com.example.boredomfocus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: Long,
    @ColumnInfo("total_detox_minutes")
    val totalDetoxMinutes: Long,
    @ColumnInfo("total_focus_seconds")
    val totalFocusSeconds: Long,
    @ColumnInfo("session_count")
    val sessionCount: Int,
    @ColumnInfo("streak_counted")
    val streakCounted: Boolean
)
