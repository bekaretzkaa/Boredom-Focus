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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyStats(stats: DailyStatsEntity)

    @Query("""
        UPDATE daily_stats
        SET
            total_detox_minutes = total_detox_minutes + :detoxMinutes,
            total_focus_seconds = total_focus_seconds + :focusSeconds,
            session_count = session_count + 1,
            streak_counted = streak_counted OR :streakCounted
        WHERE date = :date
    """)
    suspend fun updateDailyStats(
        date: Long,
        detoxMinutes: Long,
        focusSeconds: Long,
        streakCounted: Boolean
    )

    @Query("SELECT MAX(date) FROM daily_stats")
    suspend fun getLastStatsDate(): Long?

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

    @Query("""
    WITH RECURSIVE
    last_day(day) AS (
        SELECT MAX(date)
        FROM daily_stats
        WHERE date <= :todayEpochDay
          AND streak_counted = 1
    ),
    streak(day) AS (
        SELECT day
        FROM last_day
        WHERE day IS NOT NULL
          AND day >= :todayEpochDay - 1

        UNION ALL

        SELECT streak.day - 1
        FROM streak
        WHERE EXISTS (
            SELECT 1
            FROM daily_stats
            WHERE date = streak.day - 1
              AND streak_counted = 1
        )
    )
    SELECT COUNT(*) FROM streak
""")
    suspend fun getCurrentStreak(todayEpochDay: Long): Int

    @Query("SELECT SUM(session_count) FROM daily_stats WHERE date >= :startDay AND date < :endDay")
    fun getSessionCountBetween(startDay: Long, endDay: Long): Flow<Int>

    @Query("""
    SELECT COUNT(*)
    FROM daily_stats
    WHERE date <= :todayEpochDay
      AND date > COALESCE(
          (
              SELECT MAX(date)
              FROM daily_stats
              WHERE date <= :todayEpochDay
                AND session_count > 0
          ),
          (
              SELECT MIN(date) - 1
              FROM daily_stats
              WHERE date <= :todayEpochDay
          )
      )
      AND session_count = 0
""")
    fun getDaysWithoutSession(todayEpochDay: Long): Flow<Int>
}