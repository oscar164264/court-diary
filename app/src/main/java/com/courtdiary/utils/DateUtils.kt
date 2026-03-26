package com.courtdiary.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy")

/** Convert epoch-millis to a LocalDate in the device's default timezone. */
fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** Convert a LocalDate to the start-of-day epoch millis in the device's default timezone. */
fun LocalDate.toStartOfDayMillis(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Convert a LocalDate to the end-of-day epoch millis (23:59:59.999). */
fun LocalDate.toEndOfDayMillis(): Long =
    this.atTime(23, 59, 59, 999_000_000)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Format epoch-millis as "dd MMM yyyy" for display. */
fun Long.toDisplayDate(): String =
    this.toLocalDate().format(DISPLAY_FORMATTER)

/** Format a nullable Long, returning "—" when null. */
fun Long?.toDisplayDateOrDash(): String =
    this?.toDisplayDate() ?: "—"

/** Returns true when the date represented by this Long is within [daysAhead] days from today. */
fun Long.isWithinDays(daysAhead: Long): Boolean {
    val date = this.toLocalDate()
    val today = LocalDate.now()
    return !date.isBefore(today) && !date.isAfter(today.plusDays(daysAhead))
}

/** Returns true when the date is today or in the future. */
fun Long.isFutureOrToday(): Boolean = !this.toLocalDate().isBefore(LocalDate.now())

/** Returns true when the date is strictly in the past. */
fun Long.isPast(): Boolean = this.toLocalDate().isBefore(LocalDate.now())
