package com.example.boredomfocus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.boredomfocus.data.local.entity.SessionEntity
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Query("""
    SELECT
        COALESCE(MAX(focus_seconds), 0) as bestFocus,

        COALESCE(
            AVG(CASE WHEN focus_seconds > 0 THEN focus_seconds END),
            0
        ) as averageFocus,

        COUNT(*) as totalSessions,

        COALESCE(
            SUM(CASE WHEN completed = 1 AND is_focus_only = 0 THEN 1 ELSE 0 END) 
            * 100.0 / NULLIF(SUM(CASE WHEN is_focus_only = 0 THEN 1 ELSE 0 END), 0),
            0
        ) as completionRate

    FROM sessions
    WHERE date >= :fromTimeStamp AND date < :toTimeStamp
""")
    fun getStatsSummaryBetween(
        fromTimeStamp: Long,
        toTimeStamp: Long
    ): Flow<StatsSummary>

    @Query("SELECT * FROM sessions WHERE date >= :fromTimeStamp AND date < :toTimeStamp ORDER BY date DESC LIMIT 10")
    fun getLastSessions(fromTimeStamp: Long, toTimeStamp: Long) : Flow<List<SessionEntity>>

    @Query("SELECT MAX(focus_seconds) FROM sessions")
    suspend fun getAllTimeFocusRecord(): Long?

    @Query("SELECT MAX(focus_seconds) FROM sessions")
    fun getAllTimeFocusRecordFlow(): Flow<Long?>

    @Query("SELECT focus_seconds FROM sessions WHERE focus_seconds > 0 ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastFocusTime(): Long?

    @Query("SELECT MAX(focus_seconds) FROM sessions WHERE date >= :fromTimeStamp AND date < :toTimeStamp")
    suspend fun getFocusRecordBetween(fromTimeStamp: Long, toTimeStamp: Long): Long?

}