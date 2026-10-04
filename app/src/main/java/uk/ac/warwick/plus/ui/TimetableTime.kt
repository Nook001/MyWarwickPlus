package uk.ac.warwick.plus.ui

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import uk.ac.warwick.plus.data.EventEntity

fun conflictingEventIds(events: List<EventEntity>): Set<String> = buildSet {
    val timed = events.filter { !it.allDay && it.endMillis > it.startMillis }
    timed.forEachIndexed { index, event ->
        timed.drop(index + 1).forEach { other ->
            if (event.startMillis < other.endMillis && other.startMillis < event.endMillis) { add(event.id); add(other.id) }
        }
    }
}

val WarwickZone: ZoneId = ZoneId.of("Europe/London")
fun atWarwick(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(WarwickZone)
fun monday(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
fun dateLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK))
fun timeLabel(millis: Long): String = atWarwick(millis).format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))

fun eventsOnDate(events: List<EventEntity>, date: LocalDate): List<EventEntity> {
    val start = date.atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    return events.filter {
        if (it.startMillis == it.endMillis) it.startMillis in start until end
        else it.startMillis < end && it.endMillis > start
    }.sortedWith(compareBy<EventEntity> { it.startMillis }.thenBy { it.id })
}

data class ScheduleDay(val date: LocalDate, val events: List<EventEntity>)

/** Include the chosen date even when empty; other empty dates do not occupy the agenda. */
fun scheduleDays(events: List<EventEntity>, from: LocalDate): List<ScheduleDay> {
    val grouped = sortedMapOf<LocalDate, MutableList<EventEntity>>(from to mutableListOf())
    events.forEach { event ->
        var date = maxOf(from, atWarwick(event.startMillis).toLocalDate())
        val last = atWarwick(if (event.endMillis > event.startMillis) event.endMillis - 1 else event.startMillis).toLocalDate()
        while (date <= last) {
            grouped.getOrPut(date) { mutableListOf() }.add(event)
            date = date.plusDays(1)
        }
    }
    return grouped.map { (date, entries) -> ScheduleDay(date, entries.sortedWith(compareBy<EventEntity> { it.startMillis }.thenBy { it.id })) }
}

fun scheduleDateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow · ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK))}"
    else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE d MMM" else "EEE d MMM yyyy", Locale.UK))
}

data class ScheduleTime(val start: String, val end: String, val continuesBefore: Boolean, val continuesAfter: Boolean)

fun scheduleTime(event: EventEntity, date: LocalDate): ScheduleTime {
    val start = date.atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
    return ScheduleTime(start = if (event.startMillis < start) "00:00" else timeLabel(event.startMillis),
        end = if (event.endMillis >= end) "24:00" else timeLabel(event.endMillis),
        continuesBefore = event.startMillis < start, continuesAfter = event.endMillis > end)
}

// Material's date picker encodes a calendar date at UTC midnight, independent of Warwick DST.
fun pickerMillis(date: LocalDate): Long = date.toEpochDay() * 86_400_000L
fun pickerDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun nextClassLabel(event: EventEntity, now: Long): String = when {
    event.startMillis <= now -> "Happening now"
    event.startMillis - now < 60 * 60_000 -> "In ${(event.startMillis - now + 59_999) / 60_000} min"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate() -> "Later today"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate().plusDays(1) -> "Tomorrow"
    else -> atWarwick(event.startMillis).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK))
}
