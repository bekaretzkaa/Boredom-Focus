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

    override fun getLastSessions(
        fromTimeStamp: Long,
        toTimeStamp: Long
    ): Flow<List<SessionEntity>> {
        return sessionDao.getLastSessions(fromTimeStamp, toTimeStamp)
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
        val currentMonth = getCalendarMonthRange(0)

        return FocusRecordPeriod(
            currentWeek = sessionDao.getFocusRecordBetween(currentWeek.startMillis, currentWeek.endMillis),
            currentMonth = sessionDao.getFocusRecordBetween(currentMonth.startMillis, currentMonth.endMillis),
        )
    }
}