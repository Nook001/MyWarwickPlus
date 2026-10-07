package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import uk.ac.warwick.plus.data.EventContentItem

internal data class ClassIdentity(val name: String, val code: String)

internal fun classIdentity(event: EventContentItem): ClassIdentity {
    val name = event.moduleName.ifBlank { event.title.ifBlank { "Class" } }
    val code = if (event.moduleName.isNotBlank() && event.title != name) event.title
        else event.module.takeUnless { it == name }.orEmpty()
    return ClassIdentity(name, code)
}

internal fun classTimeRange(event: EventContentItem, includeWeekday: Boolean): String {
    if (event.allDay) return "All day"
    val start = atWarwick(event.startMillis).toLocalDate()
    val end = atWarwick(event.endMillis).toLocalDate()
    val endDay = if (end == start) "" else if (includeWeekday) "${weekdayDateLabel(end)} " else "${dateLabel(end)} · "
    return "${timeLabel(event.startMillis)} – $endDay${timeLabel(event.endMillis)}"
}

internal fun classDetailTime(event: EventContentItem): String =
    "${dateLabel(atWarwick(event.startMillis).toLocalDate())}\n${classTimeRange(event, includeWeekday = false)} · Warwick time"

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
