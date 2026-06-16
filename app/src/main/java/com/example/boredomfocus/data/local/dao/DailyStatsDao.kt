package com.example.boredomfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {

    @Query("SELECT * FROM daily_stats WHERE date = :day")
    suspend fun getDailyStats(day: Long): DailyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStats(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date >= :startDay AND date < :endDay ORDER BY date ASC")
    fun getDailyStatsBetween(startDay: Long, endDay: Long): Flow<List<DailyStatsEntity>>

}