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
            MAX(focus_seconds) as bestFocus,
            AVG(focus_seconds) as averageFocus,
            COUNT(*) as totalSessions,
            SUM(CASE WHEN completed THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as completionRate
        FROM sessions
        WHERE date >= :fromTimeStamp AND date < :toTimeStamp
    """)
    fun getStatsSummaryBetween(fromTimeStamp: Long, toTimeStamp: Long): Flow<StatsSummary>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT :count")
    fun getLastSessions(count: Int) : Flow<List<SessionEntity>>

    @Query("SELECT MAX(focus_seconds) FROM sessions")
    suspend fun getAllTimeFocusRecord(): Long?

    @Query("SELECT MAX(focus_seconds) FROM sessions")
    fun getAllTimeFocusRecordFlow(): Flow<Long?>

    @Query("SELECT focus_seconds FROM sessions WHERE focus_seconds > 0 ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastFocusTime(): Long?

    @Query("SELECT MAX(focus_seconds) FROM sessions WHERE date >= :fromTimeStamp AND date < :toTimeStamp")
    suspend fun getFocusRecordBetween(fromTimeStamp: Long, toTimeStamp: Long): Long?

}