package com.example.boredomfocus.data.repository

import androidx.room.withTransaction
import com.example.boredomfocus.data.local.dao.DailyStatsDao
import com.example.boredomfocus.data.local.dao.SessionDao
import com.example.boredomfocus.data.local.database.BoredomDatabase
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.entity.SessionEntity
import com.example.boredomfocus.domain.repository.AddSessionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class AddSessionRepositoryImpl @Inject constructor(
    private val db: BoredomDatabase,
    private val sessionDao: SessionDao,
    private val dailyStatsDao: DailyStatsDao
) : AddSessionRepository {

    override suspend fun finishSession(
        detoxMinutes: Long,
        detoxSeconds: Long,
        focusSeconds: Long,
        completed: Boolean,
        isFocusOnly: Boolean,
        streakCounted: Boolean
    ) {
        val time = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()

        val today = Instant.ofEpochMilli(time)
            .atZone(zoneId)
            .toLocalDate()
            .toEpochDay()

        db.withTransaction {
            val session = SessionEntity(
                detoxMinutes = detoxMinutes,
                detoxSeconds = detoxSeconds,
                focusSeconds = focusSeconds,
                date = time,
                completed = completed,
                isFocusOnly = isFocusOnly
            )

            sessionDao.insertSession(session)

            dailyStatsDao.insertDailyStats(
                DailyStatsEntity(
                    date = today,
                    totalDetoxMinutes = 0,
                    totalFocusSeconds = 0,
                    sessionCount = 0,
                    streakCounted = false
                )
            )

            dailyStatsDao.updateDailyStats(
                date = today,
                detoxMinutes = if(completed) detoxMinutes else 0,
                focusSeconds = focusSeconds,
                streakCounted = streakCounted
            )
        }
    }
}