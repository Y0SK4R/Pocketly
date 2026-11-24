package com.example.domain

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Timezone-safe calendar utilities compatible with Android API 24+ without external dependencies.
 * All day keys use strict "YYYY-MM-DD" format; month keys use "YYYY-MM".
 */
data class DayDate(val year: Int, val month: Int, val day: Int) : Comparable<DayDate> {
    override fun compareTo(other: DayDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toDayKey(): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun toMonthKey(): MonthKey = MonthKey(year, month)

    fun toIsoStartOfDay(): String = "${toDayKey()}T12:00:00"

    fun daysInMonth(): Int = DateUtils.daysInMonth(year, month)

    fun toEpochDay(): Long {
        // Howard Hinnant's proleptic Gregorian days-from-civil algorithm (exact & DST-free)
        val y = year - if (month <= 2) 1 else 0
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val m = month
        val doy = (153 * (m + if (m > 2) -3 else 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    fun plusDays(delta: Int): DayDate = DateUtils.fromEpochDay(toEpochDay() + delta)

    fun plusMonthsClamped(deltaMonths: Int): DayDate {
        val totalMonths = (year * 12 + (month - 1)) + deltaMonths
        val newYear = Math.floorDiv(totalMonths, 12)
        val newMonth = Math.floorMod(totalMonths, 12) + 1
        val maxDay = DateUtils.daysInMonth(newYear, newMonth)
        return DayDate(newYear, newMonth, minOf(day, maxDay))
    }

    fun plusYearsClamped(deltaYears: Int): DayDate {
        val newYear = year + deltaYears
        val maxDay = DateUtils.daysInMonth(newYear, month)
        return DayDate(newYear, month, minOf(day, maxDay))
    }
}

data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    override fun compareTo(other: MonthKey): Int {
        if (year != other.year) return year.compareTo(other.year)
        return month.compareTo(other.month)
    }

    fun toKey(): String = String.format(Locale.US, "%04d-%02d", year, month)

    fun startDate(): DayDate = DayDate(year, month, 1)

    fun endDateInclusive(): DayDate = DayDate(year, month, DateUtils.daysInMonth(year, month))

    fun nextMonthStart(): DayDate = plusMonths(1).startDate()

    fun plusMonths(delta: Int): MonthKey {
        val totalMonths = (year * 12 + (month - 1)) + delta
        val newYear = Math.floorDiv(totalMonths, 12)
        val newMonth = Math.floorMod(totalMonths, 12) + 1
        return MonthKey(newYear, newMonth)
    }

    fun shortLabel(): String = "${DateUtils.shortMonthName(month)} $year"

    fun shortMonthOnly(): String = DateUtils.shortMonthName(month)
}

object DateUtils {
    private val SHORT_MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun today(): DayDate {
        val cal = Calendar.getInstance()
        return DayDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun nowIso(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02dT%02d:%02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    fun parseDayKey(dayKey: String): DayDate? {
        val parts = dayKey.trim().take(10).split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        val maxDay = daysInMonth(y, m)
        if (d !in 1..maxDay) return null
        return DayDate(y, m, d)
    }

    fun fromEpochDay(epochDay: Long): DayDate {
        val z = epochDay + 719468L
        val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
        val m = (mp + if (mp < 10L) 3L else -9L).toInt()
        val year = (y + if (m <= 2) 1L else 0L).toInt()
        return DayDate(year, m, d)
    }

    fun daysBetween(start: DayDate, end: DayDate): Long =
        end.toEpochDay() - start.toEpochDay()

    fun shortMonthName(month: Int): String =
        SHORT_MONTHS.getOrElse(month - 1) { "Jan" }

    fun formatShortDate(dayKey: String): String {
        val date = parseDayKey(dayKey) ?: return dayKey
        return "${shortMonthName(date.month)} ${date.day}"
    }

    fun formatDayHeader(dayKey: String, today: DayDate = today()): String {
        val date = parseDayKey(dayKey) ?: return dayKey
        val diff = daysBetween(date, today)
        return when (diff) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> if (date.year == today.year) {
                "${shortMonthName(date.month)} ${date.day}"
            } else {
                "${shortMonthName(date.month)} ${date.day}, ${date.year}"
            }
        }
    }

    fun formatRelativeUpdated(dayKey: String, today: DayDate = today()): String {
        val date = parseDayKey(dayKey) ?: return "Updated recently"
        val diff = daysBetween(date, today)
        return when {
            diff <= 0L -> "Updated today"
            diff == 1L -> "Updated yesterday"
            diff < 30L -> "Updated ${diff}d ago"
            else -> "Updated ${shortMonthName(date.month)} ${date.day}"
        }
    }

    fun formatDueLabel(nextDueDayKey: String, today: DayDate = today()): Pair<String?, Boolean> {
        val due = parseDayKey(nextDueDayKey) ?: return null to false
        val diff = daysBetween(today, due)
        return when {
            diff < 0L -> "Overdue" to true
            diff == 0L -> "Due today" to false
            diff == 1L -> "Due tomorrow" to false
            diff <= 7L -> "Due in $diff days" to false
            else -> null to false
        }
    }

    fun isInMonth(dayKey: String, monthKey: MonthKey): Boolean {
        val date = parseDayKey(dayKey) ?: return false
        return date >= monthKey.startDate() && date < monthKey.nextMonthStart()
    }

    fun absDaysBetween(a: DayDate, b: DayDate): Long = abs(daysBetween(a, b))
}
