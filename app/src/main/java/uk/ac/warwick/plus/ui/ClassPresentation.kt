package uk.ac.warwick.plus.ui

import java.time.LocalDate
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.EventContentItem

internal data class ClassIdentity(val name: String, val code: String)

internal fun classIdentity(event: EventContentItem, fallback: String): ClassIdentity {
    val name = event.moduleName.ifBlank { event.title.ifBlank { fallback } }
    val code = if (event.moduleName.isNotBlank() && event.title != name) event.title
        else event.module.takeUnless { it == name }.orEmpty()
    return ClassIdentity(name, code)
}

/** Code and location on one line, as the widget and reminders show them. */
internal fun classSummary(identity: ClassIdentity, event: EventContentItem): String =
    listOf(identity.code, event.location).filter { it.isNotBlank() }.joinToString(" · ")

/** Next card and widget heading: Now, Next today, Tomorrow, then the weekday date. */
internal fun nextClassHeading(event: EventContentItem, today: LocalDate, now: Long): UiText {
    val date = atWarwick(event.startMillis).toLocalDate()
    return when {
        isClassNow(event, now) -> text(AppLabels.NOW)
        date <= today -> text(AppLabels.NEXT)
        date == today.plusDays(1) -> text(R.string.tomorrow)
        else -> UiText.Literal(weekdayDateLabel(date, includeYear = date.year != today.year))
    }
}

internal fun classTimeRange(event: EventContentItem, includeWeekday: Boolean, allDay: String): String {
    if (event.allDay) return allDay
    val start = atWarwick(event.startMillis).toLocalDate()
    val end = atWarwick(event.endMillis).toLocalDate()
    val endDay = if (end == start) "" else if (includeWeekday) "${weekdayDateLabel(end)} " else "${dateLabel(end)} · "
    return "${timeLabel(event.startMillis)} – $endDay${timeLabel(event.endMillis)}"
}

private val firstNameBoundary = Regex("\\s+")
internal fun greeting(name: String, now: Long): UiText {
    val firstName = name.trim().split(firstNameBoundary, limit = 2).firstOrNull().orEmpty()
    val greeting = when (atWarwick(now).hour) {
        in 0..11 -> R.string.good_morning
        in 12..17 -> R.string.good_afternoon
        else -> R.string.good_evening
    }
    return if (firstName.isNotBlank()) text(R.string.greeting_name, text(greeting), firstName) else text(greeting)
}
