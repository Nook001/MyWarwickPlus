package uk.ac.warwick.plus.ui

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import uk.ac.warwick.plus.data.EventEntity

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

fun nextClassLabel(event: EventEntity, now: Long): String = when {
    event.startMillis <= now -> "Happening now"
    event.startMillis - now < 60 * 60_000 -> "In ${(event.startMillis - now + 59_999) / 60_000} min"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate() -> "Later today"
    atWarwick(event.startMillis).toLocalDate() == atWarwick(now).toLocalDate().plusDays(1) -> "Tomorrow"
    else -> atWarwick(event.startMillis).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.UK))
}
