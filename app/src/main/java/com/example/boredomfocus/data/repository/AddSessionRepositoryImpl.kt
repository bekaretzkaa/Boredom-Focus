package com.example.boredomfocus.data.repository

import androidx.room.withTransaction
import com.example.boredomfocus.data.local.dao.DailyStatsDao
import com.example.boredomfocus.data.local.dao.SessionDao
import com.example.boredomfocus.data.local.database.BoredomDatabase
import com.example.boredomfocus.data.local.entity.SessionEntity
import com.example.boredomfocus.domain.repository.AddSessionRepository
import java.time.LocalDate
import javax.inject.Inject

class AddSessionRepositoryImpl @Inject constructor(
    private val db: BoredomDatabase,
    private val sessionDao: SessionDao,
    private val dailyStatsDao: DailyStatsDao
) : AddSessionRepository {

    override suspend fun finishSession(
        detoxMinutes: Long,
        focusSeconds: Long,
        completed: Boolean,
        isFocusOnly: Boolean,
        streakCounted: Boolean
    ) {
        val time = System.currentTimeMillis()
        val today = LocalDate.now().toEpochDay()

        db.withTransaction {
            val session = SessionEntity(
                detoxMinutes = detoxMinutes,
                focusSeconds = focusSeconds,
                date = time,
                completed = completed,
                isFocusOnly = isFocusOnly
            )

            sessionDao.insertSession(session)

            dailyStatsDao.updateDailyStats(
                date = today,
                detoxMinutes = detoxMinutes,
                focusSeconds = focusSeconds,
                streakCounted = streakCounted
            )
        }
    }
}