package com.example.boredomfocus.core.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeekRangeMillis(
    val startMillis: Long,
    val endMillis: Long
)

data class WeekRangeDays(
    val startDay: Long,
    val endDay: Long
)

fun getCalendarWeekRange(
    weekOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
) : WeekRangeMillis {
    val startOfThisWeek = LocalDate.now(zoneId)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset)
        .atStartOfDay(zoneId)

    val startOfNextWeek = startOfThisWeek.plusWeeks(1)

    return WeekRangeMillis(
        startMillis = startOfThisWeek.toInstant().toEpochMilli(),
        endMillis = startOfNextWeek.toInstant().toEpochMilli()
    )
}

fun getCalendarWeekRangeDay(
    weekOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
) : WeekRangeDays {
    val startOfThisWeek = LocalDate.now(zoneId)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset)

    val startOfNextWeek = startOfThisWeek.plusWeeks(1)

    return WeekRangeDays(
        startDay = startOfThisWeek.toEpochDay(),
        endDay = startOfNextWeek.toEpochDay()
    )
}