package com.example.boredomfocus.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class RangeMillis(
    val startMillis: Long,
    val endMillis: Long
)

data class RangeDays(
    val startDay: Long,
    val endDay: Long
)

fun getCalendarWeekRange(
    weekOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
) : RangeMillis {
    val startOfThisWeek = LocalDate.now(zoneId)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset)
        .atStartOfDay(zoneId)

    val startOfNextWeek = startOfThisWeek.plusWeeks(1)

    return RangeMillis(
        startMillis = startOfThisWeek.toInstant().toEpochMilli(),
        endMillis = startOfNextWeek.toInstant().toEpochMilli()
    )
}

fun getCalendarWeekRangeDay(
    weekOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
) : RangeDays {
    val startOfThisWeek = LocalDate.now(zoneId)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset)

    val startOfNextWeek = startOfThisWeek.plusWeeks(1)

    return RangeDays(
        startDay = startOfThisWeek.toEpochDay(),
        endDay = startOfNextWeek.toEpochDay()
    )
}

fun getCalendarMonthRange(
    monthOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
): RangeMillis {
    val startOfMonth = LocalDate.now(zoneId)
        .withDayOfMonth(1)
        .plusMonths(monthOffset)
        .atStartOfDay(zoneId)

    val startOfNextMonth = startOfMonth.plusMonths(1)

    return RangeMillis(
        startMillis = startOfMonth.toInstant().toEpochMilli(),
        endMillis = startOfNextMonth.toInstant().toEpochMilli()
    )
}

fun getCalendarMonthRangeDay(
    monthOffset: Long = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
): RangeDays {
    val startOfMonth = LocalDate.now(zoneId)
        .withDayOfMonth(1)
        .plusMonths(monthOffset)

    val startOfNextMonth = startOfMonth.plusMonths(1)

    return RangeDays(
        startDay = startOfMonth.toEpochDay(),
        endDay = startOfNextMonth.toEpochDay()
    )
}

fun getLastThreeCalendarMonthsRangeDay(
    zoneId: ZoneId = ZoneId.systemDefault()
): RangeDays {
    val startOfCurrentMonth = LocalDate.now(zoneId)
        .withDayOfMonth(1)

    val startOfThreeMonthsAgo = startOfCurrentMonth.minusMonths(2)

    val startOfNextMonth = startOfCurrentMonth.plusMonths(1)

    return RangeDays(
        startDay = startOfThreeMonthsAgo.toEpochDay(),
        endDay = startOfNextMonth.toEpochDay()
    )
}

fun formatDateFromEpochMillis(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDate()

    val today = LocalDate.now(zoneId)
    val yesterday = today.minusDays(1)

    return when (date) {
        today -> "Сегодня"
        yesterday -> "Вчера"
        else -> {
            val formatter = DateTimeFormatter
                .ofPattern("d MMMM", Locale("ru"))

            date.format(formatter)
        }
    }
}