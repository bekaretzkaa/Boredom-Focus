package com.example.boredomfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.boredomfocus.core.common.RangeDays
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.model.MonthStatsResult
import com.example.boredomfocus.data.local.model.MonthWeekStatsResult
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {

    @Query("SELECT * FROM daily_stats WHERE date = :day")
    suspend fun getDailyStats(day: Long): DailyStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStats(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE date >= :startDay AND date < :endDay ORDER BY date ASC")
    fun getDailyStatsBetween(startDay: Long, endDay: Long): Flow<List<DailyStatsEntity>>

    @Query("""
    SELECT
        ((date - :monthStartDay) / 7) AS weekIndex,
        (:monthStartDay + (((date - :monthStartDay) / 7) * 7)) AS weekStartDay,
        MIN(:monthStartDay + ((((date - :monthStartDay) / 7) + 1) * 7), :monthEndDay) AS weekEndDay,
        SUM(total_detox_minutes) AS totalDetoxMinutes,
        SUM(total_focus_seconds) AS totalFocusSeconds,
        SUM(session_count) AS sessionCount
    FROM daily_stats
    WHERE date >= :monthStartDay AND date < :monthEndDay
    GROUP BY weekIndex
    ORDER BY weekStartDay ASC
""")
    fun getMonthStatsByWeeks(
        monthStartDay: Long,
        monthEndDay: Long
    ): Flow<List<MonthWeekStatsResult>>

    @Query("""
    SELECT
        strftime('%Y-%m', datetime(date * 86400, 'unixepoch')) AS yearMonth,

        CAST(
            strftime(
                '%s',
                date(datetime(date * 86400, 'unixepoch'), 'start of month')
            ) / 86400 AS INTEGER
        ) AS monthStartDay,

        SUM(total_detox_minutes) AS totalDetoxMinutes,
        SUM(total_focus_seconds) AS totalFocusSeconds,
        SUM(session_count) AS sessionCount

    FROM daily_stats

    WHERE date >= :startMonthDay
      AND date < :endMonthDay

    GROUP BY yearMonth
    ORDER BY monthStartDay ASC
""")
    fun getStatsByMonths(
        startMonthDay: Long,
        endMonthDay: Long
    ): Flow<List<MonthStatsResult>>

    @Query("SELECT SUM(total_focus_seconds) FROM daily_stats WHERE date >= :startDay AND date < :endDay")
    fun getFocusStatsBetween(startDay: Long, endDay: Long): Flow<Long>

    @Query("SELECT MIN(date) FROM daily_stats")
    suspend fun getFirstDate(): Int?
}