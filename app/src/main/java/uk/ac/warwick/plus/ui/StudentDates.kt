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

/** Shared immutable formatters. Times are always Warwick time; only the app's UI language picks the wording. */
private class DateFormats(private val locale: Locale, private val chinese: Boolean) {
    private fun format(english: String, simplified: String) = DateTimeFormatter.ofPattern(if (chinese) simplified else english, locale)
    val clock = format("HH:mm", "HH:mm")
    val longDate = format("EEEE, d MMMM", "M月d日 EEEE")
    val shortDate = format("d MMM", "M月d日")
    val shortDateYear = format("d MMM yyyy", "yyyy年M月d日")
    val weekdayDate = format("EEE d MMM", "M月d日 EEE")
    val weekdayDateYear = format("EEE d MMM yyyy", "yyyy年M月d日 EEE")
    val relativeDate = format("EEE, d MMM", "M月d日 EEE")
    val shortDateTime = format("d MMM · HH:mm", "M月d日 · HH:mm")
    val dateTimeYear = format("d MMM yyyy · HH:mm", "yyyy年M月d日 · HH:mm")
    val fullDateTime = format("EEE d MMM yyyy · HH:mm", "yyyy年M月d日 EEE · HH:mm")
}

private val englishFormats = DateFormats(Locale.UK, chinese = false)
private val chineseFormats by lazy { DateFormats(Locale.SIMPLIFIED_CHINESE, chinese = true) }
private val StudentDateFormats get() = if (Locale.getDefault().language == "zh") chineseFormats else englishFormats

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
