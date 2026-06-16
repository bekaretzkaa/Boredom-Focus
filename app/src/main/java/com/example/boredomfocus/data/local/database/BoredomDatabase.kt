package com.example.boredomfocus.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.boredomfocus.data.local.dao.DailyStatsDao
import com.example.boredomfocus.data.local.dao.SessionDao
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, DailyStatsEntity::class],
    version = 1
)
abstract class BoredomDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    abstract fun dailyStatsDao(): DailyStatsDao

}