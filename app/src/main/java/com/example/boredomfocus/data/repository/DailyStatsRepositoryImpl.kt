package com.example.boredomfocus.data.repository

import com.example.boredomfocus.data.local.dao.DailyStatsDao
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DailyStatsRepositoryImpl @Inject constructor(
    private val dailyStatsDao: DailyStatsDao
) : DailyStatsRepository {

    override suspend fun getDailyStats(day: Long): DailyStatsEntity? {
        return dailyStatsDao.getDailyStats(day)
    }

    override suspend fun upsertDailyStats(stats: DailyStatsEntity) {
        dailyStatsDao.upsertDailyStats(stats)
    }

    override fun getDailyStatsBetween(
        startDay: Long,
        endDay: Long
    ): Flow<List<DailyStatsEntity>> {
        return dailyStatsDao.getDailyStatsBetween(startDay, endDay)
    }
}