package com.example.boredomfocus.core.common

import android.content.Context
import com.example.boredomfocus.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    context: Context,
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val date = Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDate()

    val today = LocalDate.now(zoneId)
    val yesterday = today.minusDays(1)

    return when (date) {
        today -> context.getString(R.string.today)
        yesterday -> context.getString(R.string.yesterday)
        else -> {
            val formatter = DateTimeFormatter.ofPattern(
                "d MMMM",
                context.resources.configuration.locales[0]
            )
            date.format(formatter)
        }
    }
}

fun formatSeconds(totalSeconds: Long): String {
    return if (totalSeconds >= 3600) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        String.format("%2d:%02d:%02d", hours, minutes, seconds)
    } else {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        String.format("%02d:%02d", minutes, seconds)
    }
}

fun getCurrentMonthWeeksCount(
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
): Int {
    val currentMonth = YearMonth.now()

    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()

    val offset = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    return (offset + daysInMonth + 6) / 7
}

fun epochDayToDayOfWeekIndex(epochDay: Long): Int {
    return LocalDate.ofEpochDay(epochDay).dayOfWeek.value
}

fun epochDayToRussianWeekDay(epochDay: Long): String {
    return LocalDate.ofEpochDay(epochDay)
        .dayOfWeek
        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .uppercase(Locale.getDefault())
}

fun toRussianWeekDay(day: Int): String {
    return DayOfWeek.of(day)
        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .uppercase(Locale.getDefault())
}

fun getMonthName(yearMonth: String): String {
    return YearMonth.parse(yearMonth)
        .month
        .getDisplayName(
            TextStyle.FULL_STANDALONE,
            Locale.getDefault()
        )
}

fun getYearMonthByOffset(
    monthOffset: Int = 0,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    return YearMonth.now(zoneId)
        .minusMonths(monthOffset.toLong())
        .toString()
}

fun daysWord(n: Int): String {
    val lastTwo = n % 100
    val lastOne = n % 10

    return when {
        lastTwo in 11..14 -> "дней"
        lastOne == 1 -> "день"
        lastOne in 2..4 -> "дня"
        else -> "дней"
    }
}

fun epochMillisToTime(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    return Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .format(formatter)
}