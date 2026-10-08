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

internal data class TimelineBlock(val event: EventContentItem, val start: Float, val end: Float, val lane: Int)

/** Positions are fractions of the visible hour range; overlapping classes take separate lanes. */
internal data class DayTimeline(val startHour: Int, val endHour: Int, val blocks: List<TimelineBlock>,
    val lanes: Int, val now: Float?) {
    fun position(hour: Int) = (hour - startHour).toFloat() / (endHour - startHour)
}

internal fun dayTimeline(events: List<EventContentItem>, date: LocalDate, now: Long): DayTimeline? {
    val dayStart = date.atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    // Wall-clock hours keep DST days aligned with the printed times.
    fun hour(millis: Long) = when {
        millis <= dayStart -> 0f
        millis >= dayEnd -> 24f
        else -> atWarwick(millis).toLocalTime().toSecondOfDay() / 3600f
    }
    val timed = eventsOnDate(events, date).filter { !it.allDay && it.endMillis > it.startMillis }
    if (timed.isEmpty()) return null
    val nowHour = if (atWarwick(now).toLocalDate() == date) hour(now) else null
    // Waking hours widen the range to show the current time; night-time stays pinned to an edge.
    val visibleNow = nowHour?.takeIf { it in 7f..22f }
    val startHour = minOf(9, timed.minOf { hour(it.startMillis) }.toInt(), visibleNow?.toInt() ?: 24)
    val endHour = maxOf(18, kotlin.math.ceil(maxOf(timed.maxOf { hour(it.endMillis) }, visibleNow ?: 0f)).toInt())
    val span = (endHour - startHour).toFloat()
    val laneEnds = mutableListOf<Long>()
    val blocks = timed.map { event ->
        val lane = laneEnds.indexOfFirst { it <= event.startMillis }.takeIf { it >= 0 }
            ?: laneEnds.size.also { laneEnds += 0L }
        laneEnds[lane] = event.endMillis
        TimelineBlock(event, (hour(event.startMillis) - startHour) / span, (hour(event.endMillis) - startHour) / span, lane)
    }
    val current = nowHour?.let { ((it - startHour) / span).coerceIn(0f, 1f) }
    return DayTimeline(startHour, endHour, blocks, laneEnds.size, current)
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

fun nextClassLabel(event: EventContentItem, now: Long): UiText {
    val today = atWarwick(now).toLocalDate()
    val date = atWarwick(event.startMillis).toLocalDate()
    val minutes = (event.startMillis - now + 59_999) / 60_000
    return when {
        event.startMillis <= now -> ((event.endMillis - now + 59_999) / 60_000).let {
            if (it < 60) text(R.string.minutes_left, it) else text(R.string.hours_left, it / 60, it % 60)
        }
        minutes < 60 -> text(R.string.in_minutes, minutes)
        date == today -> text(R.string.in_hours, minutes / 60, minutes % 60)
        date == today.plusDays(1) -> text(R.string.tomorrow)
        else -> UiText.Literal(relativeDateLabel(date))
    }
}
