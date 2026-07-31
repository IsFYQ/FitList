package com.example.healthcheckin.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateTimeUtil {

    private val localDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun zoneId(): ZoneId = ZoneId.systemDefault()

    fun nowEpochMillis(): Long = System.currentTimeMillis()

    fun todayLocalDate(): LocalDate = LocalDate.now(zoneId())

    fun todayLocalDateString(): String = todayLocalDate().format(localDateFormatter)

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId()).toLocalDate()

    fun toLocalDateString(epochMillis: Long): String =
        toLocalDate(epochMillis).format(localDateFormatter)

    fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId()).toLocalDateTime()

    fun tzOffsetMinutes(): Int =
        zoneId().rules.getOffset(Instant.now()).totalSeconds / 60

    fun localDateToEpochRange(localDate: LocalDate): LongRange {
        val start = localDate.atStartOfDay(zoneId()).toInstant().toEpochMilli()
        val end = localDate.atTime(LocalTime.MAX).atZone(zoneId()).toInstant().toEpochMilli()
        return start..end
    }

    fun localDateStringToEpochStart(localDateString: String): Long {
        val date = LocalDate.parse(localDateString, localDateFormatter)
        return date.atStartOfDay(zoneId()).toInstant().toEpochMilli()
    }

    fun ageYears(birthYearMonth: String, referenceDate: LocalDate = todayLocalDate()): Int {
        val parts = birthYearMonth.split("-")
        require(parts.size == 2) { "birthYearMonth must be YYYY-MM" }
        val birthYear = parts[0].toInt()
        val birthMonth = parts[1].toInt()
        var age = referenceDate.year - birthYear
        if (referenceDate.monthValue < birthMonth) {
            age -= 1
        }
        return age
    }

    fun isFutureDate(localDate: LocalDate): Boolean =
        localDate.isAfter(todayLocalDate())

    fun isFutureDateString(localDateString: String): Boolean =
        isFutureDate(LocalDate.parse(localDateString, localDateFormatter))

    fun backfillMinDate(registeredLocalDate: String): LocalDate {
        val registered = LocalDate.parse(registeredLocalDate, localDateFormatter)
        val oneYearAgo = todayLocalDate().minusDays(ValidationConstants.BACKFILL_MAX_DAYS.toLong())
        return maxOf(registered, oneYearAgo)
    }

    fun backfillMinDateString(registeredLocalDate: String): String =
        backfillMinDate(registeredLocalDate).format(localDateFormatter)

    fun daysBetween(start: LocalDate, end: LocalDate): Long =
        ChronoUnit.DAYS.between(start, end)

    fun mealSlotMidpointTime(mealSlot: MealSlot): LocalTime = when (mealSlot) {
        MealSlot.BREAKFAST -> LocalTime.of(8, 0)
        MealSlot.LUNCH -> LocalTime.of(12, 0)
        MealSlot.DINNER -> LocalTime.of(19, 0)
        MealSlot.SNACK -> LocalTime.of(15, 30)
    }

    fun combineDateAndTime(localDate: LocalDate, time: LocalTime): Long =
        localDate.atTime(time).atZone(zoneId()).toInstant().toEpochMilli()

    fun parseLocalDate(value: String): LocalDate =
        LocalDate.parse(value, localDateFormatter)

    fun formatLocalDate(date: LocalDate): String =
        date.format(localDateFormatter)

    fun formatDashboardDate(localDateString: String): String {
        val date = if (localDateString.isBlank()) {
            todayLocalDate()
        } else {
            parseLocalDate(localDateString)
        }
        val dayOfWeek = when (date.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            7 -> "周日"
            else -> ""
        }
        return "${date.monthValue}月${date.dayOfMonth}日 $dayOfWeek"
    }
}
