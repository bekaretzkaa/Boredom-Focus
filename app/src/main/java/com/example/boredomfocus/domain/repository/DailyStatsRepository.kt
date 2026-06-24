package com.example.boredomfocus.domain.repository

import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.data.local.model.MonthStatsResult
import com.example.boredomfocus.data.local.model.MonthWeekStatsResult
import kotlinx.coroutines.flow.Flow

interface DailyStatsRepository {

    suspend fun getDailyStats(day: Long): DailyStatsEntity?

    suspend fun getLastStatsDate(): Long?

    suspend fun ensureStatsUntilToday(fromDate: Long)

    fun getDailyStatsBetween(startDay: Long, endDay: Long): Flow<List<DailyStatsEntity>>

    fun getMonthStatsByWeeks(monthStartDay: Long, monthEndDay: Long): Flow<List<MonthWeekStatsResult>>

    fun getStatsByMonths(startMonthDay: Long, endMonthDay: Long): Flow<List<MonthStatsResult>>

    fun getFocusStatsBetween(startDay: Long, endDay: Long): Flow<Long>

    suspend fun getFirstDate(): Int?

    suspend fun getCurrentStreak(todayEpochDay: Long): Int

    fun getSessionCountBetween(startDay: Long, endDay: Long): Flow<Int>

}