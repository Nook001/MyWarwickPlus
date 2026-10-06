package uk.ac.warwick.plus.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val WarwickZone: ZoneId = ZoneId.of("Europe/London")
fun atWarwick(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(WarwickZone)

/** Immutable formatters are shared across rows; the phone locale and zone never select the format. */
private object StudentDateFormats {
    private fun format(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.UK)
    val clock = format("HH:mm")
    val longDate = format("EEEE, d MMMM")
    val shortDate = format("d MMM")
    val shortDateYear = format("d MMM yyyy")
    val weekdayDate = format("EEE d MMM")
    val weekdayDateYear = format("EEE d MMM yyyy")
    val relativeDate = format("EEE, d MMM")
    val shortDateTime = format("d MMM · HH:mm")
    val dateTimeYear = format("d MMM yyyy · HH:mm")
    val fullDateTime = format("EEE d MMM yyyy · HH:mm")
}

fun dateLabel(date: LocalDate): String = date.format(StudentDateFormats.longDate)
fun timeLabel(millis: Long): String = atWarwick(millis).format(StudentDateFormats.clock)
internal fun shortDateLabel(date: LocalDate, includeYear: Boolean = false): String =
    date.format(if (includeYear) StudentDateFormats.shortDateYear else StudentDateFormats.shortDate)
internal fun weekdayDateLabel(date: LocalDate, includeYear: Boolean = false): String =
    date.format(if (includeYear) StudentDateFormats.weekdayDateYear else StudentDateFormats.weekdayDate)
internal fun relativeDateLabel(date: LocalDate): String = date.format(StudentDateFormats.relativeDate)
internal fun fullDateTimeLabel(millis: Long): String = atWarwick(millis).format(StudentDateFormats.fullDateTime)
internal fun updatedTimeLabel(millis: Long): String = atWarwick(millis).format(StudentDateFormats.dateTimeYear)

internal data class DeadlineTiming(val days: Long, val passed: Boolean)
internal fun deadlineTiming(due: Long, now: Long) = DeadlineTiming(
    ChronoUnit.DAYS.between(atWarwick(now).toLocalDate(), atWarwick(due).toLocalDate()), due < now)

internal fun deadlineDateLabel(due: Long, now: Long, includeTime: Boolean): String {
    val date = atWarwick(due)
    val currentYear = atWarwick(now).year
    return if (includeTime) date.format(if (date.year != currentYear)
        StudentDateFormats.dateTimeYear else StudentDateFormats.shortDateTime)
    else shortDateLabel(date.toLocalDate(), includeYear = date.year > currentYear)
}
