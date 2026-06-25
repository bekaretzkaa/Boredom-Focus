package com.example.boredomfocus.data.repository

import com.example.boredomfocus.core.common.getCalendarMonthRange
import com.example.boredomfocus.core.common.getCalendarWeekRange
import com.example.boredomfocus.data.local.dao.SessionDao
import com.example.boredomfocus.data.local.entity.SessionEntity
import com.example.boredomfocus.domain.model.FocusRecordPeriod
import com.example.boredomfocus.domain.repository.SessionRepository
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun insertSession(session: SessionEntity) {
        sessionDao.insertSession(session)
    }

    override fun getStatsSummaryBetween(
        fromTimeStamp: Long,
        toTimeStamp: Long
    ): Flow<StatsSummary> {
        return sessionDao.getStatsSummaryBetween(fromTimeStamp, toTimeStamp)
    }

    override fun getLastSessions(count: Int): Flow<List<SessionEntity>> {
        return sessionDao.getLastSessions(count)
    }

    override suspend fun getAllTimeFocusRecord(): Long? {
        return sessionDao.getAllTimeFocusRecord()
    }

    override fun getAllTimeFocusRecordFlow(): Flow<Long?> {
        return sessionDao.getAllTimeFocusRecordFlow()
    }

    override suspend fun getLastFocusTime(): Long? {
        return sessionDao.getLastFocusTime()
    }

    override suspend fun getFocusRecordBetween(): FocusRecordPeriod {
        val currentWeek = getCalendarWeekRange(0)
        val previousWeek = getCalendarWeekRange(-1)
        val currentMonth = getCalendarMonthRange(0)
        val previousMonth = getCalendarMonthRange(-1)

        return FocusRecordPeriod(
            currentWeek = sessionDao.getFocusRecordBetween(currentWeek.startMillis, currentWeek.endMillis) ?: 0L,
            previousWeek = sessionDao.getFocusRecordBetween(previousWeek.startMillis, previousWeek.endMillis) ?: 0L,
            currentMonth = sessionDao.getFocusRecordBetween(currentMonth.startMillis, currentMonth.endMillis) ?: 0L,
            previousMonth = sessionDao.getFocusRecordBetween(previousMonth.startMillis, previousMonth.endMillis) ?: 0L
        )
    }
}