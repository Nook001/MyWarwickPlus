package uk.ac.warwick.plus.ui

import java.time.*
import uk.ac.warwick.plus.data.EventContentItem

fun conflictingEventIds(events: List<EventContentItem>): Set<String> = buildSet {
    val timed = events.filter { !it.allDay && it.endMillis > it.startMillis }
    timed.forEachIndexed { index, event ->
        for (otherIndex in index + 1 until timed.size) {
            val other = timed[otherIndex]
            if (event.startMillis < other.endMillis && other.startMillis < event.endMillis) { add(event.id); add(other.id) }
        }
    }
}

fun eventsOnDate(events: List<EventContentItem>, date: LocalDate): List<EventContentItem> {
    val start = date.atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    return events.filter {
        if (it.startMillis == it.endMillis) it.startMillis in start until end
        else it.startMillis < end && it.endMillis > start
    }.sortedWith(compareBy<EventContentItem> { it.startMillis }.thenBy { it.id })
}

data class ScheduleDay(val date: LocalDate, val events: List<EventContentItem>)

/** Include the chosen date even when empty; other empty dates do not occupy the agenda. */
fun scheduleDays(events: List<EventContentItem>, from: LocalDate): List<ScheduleDay> {
    val grouped = sortedMapOf<LocalDate, MutableList<EventContentItem>>(from to mutableListOf())
    events.forEach { event ->
        var date = maxOf(from, atWarwick(event.startMillis).toLocalDate())
        val last = atWarwick(if (event.endMillis > event.startMillis) event.endMillis - 1 else event.startMillis).toLocalDate()
        while (date <= last) {
            grouped.getOrPut(date) { mutableListOf() }.add(event)
            date = date.plusDays(1)
        }
    }
    return grouped.map { (date, entries) -> ScheduleDay(date, entries.sortedWith(compareBy<EventContentItem> { it.startMillis }.thenBy { it.id })) }
}

fun scheduleDateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow · ${shortDateLabel(date)}"
    else -> weekdayDateLabel(date, includeYear = date.year != today.year)
}

data class ScheduleTime(val start: String, val end: String, val continuesBefore: Boolean, val continuesAfter: Boolean)

fun scheduleTime(event: EventContentItem, date: LocalDate): ScheduleTime {
    val start = date.atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    return ScheduleTime(start = if (event.startMillis < start) "00:00" else timeLabel(event.startMillis),
        end = if (event.endMillis >= end) "24:00" else timeLabel(event.endMillis),
        continuesBefore = event.startMillis < start, continuesAfter = event.endMillis > end)
}

// Material's date picker encodes a calendar date at UTC midnight, independent of Warwick DST.
fun pickerMillis(date: LocalDate): Long = date.toEpochDay() * 86_400_000L
fun pickerDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun nextTimedClass(events: List<EventContentItem>, now: Long): EventContentItem? = events
    .filter { !it.allDay && it.startMillis > now }
    .minWithOrNull(compareBy<EventContentItem> { it.startMillis }.thenBy { it.id })

fun currentOrNextClass(events: List<EventContentItem>, now: Long): EventContentItem? = events
    .filter { !it.allDay && it.startMillis <= now && it.endMillis > now }
    .minWithOrNull(compareBy<EventContentItem> { it.startMillis }.thenBy { it.id })
    ?: nextTimedClass(events, now)

fun nextClassLabel(event: EventContentItem, now: Long): String = when {
    event.startMillis <= now -> "Happening now"
    event.startMillis - now < 60 * 60_000 -> "In ${(event.startMillis - now + 59_999) / 60_000} min"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate() -> "Later today"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate().plusDays(1) -> "Tomorrow"
    else -> relativeDateLabel(atWarwick(event.startMillis).toLocalDate())
}
