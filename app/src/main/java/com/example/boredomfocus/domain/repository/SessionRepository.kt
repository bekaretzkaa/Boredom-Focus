package com.example.boredomfocus.domain.repository

import com.example.boredomfocus.data.local.entity.SessionEntity
import com.example.boredomfocus.feature.statistics.presentation.model.StatsSummary
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    suspend fun insertSession(session: SessionEntity)

    fun getStatsSummaryBetween(fromTimeStamp: Long, toTimeStamp: Long): Flow<StatsSummary>

    fun getLastSessions(count: Int) : Flow<List<SessionEntity>>

    fun getAllTimeFocusRecord(): Flow<Long?>

}