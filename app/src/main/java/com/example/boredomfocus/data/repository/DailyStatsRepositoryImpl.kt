package com.example.boredomfocus.data.repository

import com.example.boredomfocus.data.local.dao.DailyStatsDao
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.model.MonthStatsResult
import com.example.boredomfocus.data.local.model.MonthWeekStatsResult
import com.example.boredomfocus.domain.repository.DailyStatsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DailyStatsRepositoryImpl @Inject constructor(
    private val dailyStatsDao: DailyStatsDao
) : DailyStatsRepository {

    override suspend fun getDailyStats(day: Long): DailyStatsEntity? {
        return dailyStatsDao.getDailyStats(day)
    }

    override suspend fun getLastStatsDate(): Long? {
        return dailyStatsDao.getLastStatsDate()
    }

    override suspend fun ensureStatsUntilToday(fromDate: Long) {
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

        for(date in fromDate..today) {
            dailyStatsDao.insertDailyStats(
                DailyStatsEntity(
                    date = date,
                    totalDetoxMinutes = 0,
                    totalFocusSeconds = 0,
                    sessionCount = 0,
                    streakCounted = false
                )
            )
        }
    }

    override fun getDailyStatsBetween(
        startDay: Long,
        endDay: Long
    ): Flow<List<DailyStatsEntity>> {
        return dailyStatsDao.getDailyStatsBetween(startDay, endDay)
    }

    override fun getMonthStatsByWeeks(
        monthStartDay: Long,
        monthEndDay: Long
    ): Flow<List<MonthWeekStatsResult>> {
        return dailyStatsDao.getMonthStatsByWeeks(monthStartDay, monthEndDay)
    }

    override fun getStatsByMonths(
        startMonthDay: Long,
        endMonthDay: Long
    ): Flow<List<MonthStatsResult>> {
        return dailyStatsDao.getStatsByMonths(startMonthDay, endMonthDay)
    }

    override fun getFocusStatsBetween(
        startDay: Long,
        endDay: Long
    ): Flow<Long> {
        return dailyStatsDao.getFocusStatsBetween(startDay, endDay)
    }

    override suspend fun getFirstDate(): Int? {
        return dailyStatsDao.getFirstDate()
    }

    override suspend fun getCurrentStreak(todayEpochDay: Long): Int {
        return dailyStatsDao.getCurrentStreak(todayEpochDay)
    }

    override fun getSessionCountBetween(
        startDay: Long,
        endDay: Long
    ): Flow<Int> {
        return dailyStatsDao.getSessionCountBetween(startDay, endDay)
    }

    override fun getDaysWithoutSession(todayEpochDay: Long): Flow<Int> {
        return dailyStatsDao.getDaysWithoutSession(todayEpochDay)
    }
}