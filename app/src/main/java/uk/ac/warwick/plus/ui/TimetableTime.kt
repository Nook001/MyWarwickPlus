package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import java.time.*
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.EventContentItem

private val EventOrder = compareBy<EventContentItem> { it.startMillis }.thenBy { it.id }

internal fun isClassNow(event: EventContentItem, now: Long) =
    !event.allDay && event.startMillis <= now && event.endMillis > now

internal fun classStatus(event: EventContentItem, now: Long, nextId: String?, date: LocalDate): Int? = when {
    isClassNow(event, now) && date == atWarwick(now).toLocalDate() -> AppLabels.NOW
    event.id == nextId && date == atWarwick(event.startMillis).toLocalDate() -> AppLabels.NEXT
    else -> null
}

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
    }.sortedWith(EventOrder)
}

data class ScheduleDay(val date: LocalDate, val events: List<EventContentItem>)

internal data class HomeAgenda(val day: ScheduleDay, val weekendMessage: Int? = null)

/** Today and a finished day have the same transition; no separate empty-tomorrow state. */
internal fun homeAgenda(events: List<EventContentItem>, now: Long): HomeAgenda {
    val today = atWarwick(now).toLocalDate()
    val todayEvents = eventsOnDate(events, today)
    val remaining = todayEvents.filter {
        if (it.endMillis > it.startMillis) it.endMillis > now
        else it.allDay || it.startMillis >= now
    }
    if (remaining.isNotEmpty()) return HomeAgenda(ScheduleDay(today, remaining))
    if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) {
        return HomeAgenda(ScheduleDay(today, emptyList()),
            if (todayEvents.isEmpty()) R.string.enjoy_weekend else R.string.well_done_weekend)
    }
    val tomorrow = today.plusDays(1)
    return HomeAgenda(ScheduleDay(tomorrow, eventsOnDate(events, tomorrow)))
}

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
    return grouped.map { (date, entries) -> ScheduleDay(date, entries.sortedWith(EventOrder)) }
}

fun scheduleDateLabel(date: LocalDate, today: LocalDate): UiText = when (date) {
    today -> text(AppLabels.TODAY)
    today.plusDays(1) -> text(R.string.tomorrow_date, shortDateLabel(date))
    else -> UiText.Literal(weekdayDateLabel(date, includeYear = date.year != today.year))
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
    .asSequence()
    .filter { !it.allDay && it.startMillis > now }
    .minWithOrNull(EventOrder)

fun currentOrNextClass(events: List<EventContentItem>, now: Long): EventContentItem? = events
    .asSequence().filter { isClassNow(it, now) }
    .minWithOrNull(EventOrder)
    ?: nextTimedClass(events, now)

fun nextClassLabel(event: EventContentItem, now: Long): UiText = when {
    event.startMillis <= now -> text(R.string.happening_now)
    event.startMillis - now < 60 * 60_000 -> text(R.string.in_minutes, (event.startMillis - now + 59_999) / 60_000)
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate() -> text(R.string.later_today)
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate().plusDays(1) -> text(R.string.tomorrow)
    else -> UiText.Literal(relativeDateLabel(atWarwick(event.startMillis).toLocalDate()))
}
