package uk.ac.warwick.plus.reminders

import uk.ac.warwick.plus.data.CourseworkContentItem
import uk.ac.warwick.plus.data.EventContentItem

enum class ReminderKind { CLASS, DEADLINE }

data class Reminder(val kind: ReminderKind, val id: String, val triggerAt: Long, val targetAt: Long)

const val CLASS_LEAD_MILLIS = 10 * 60_000L
const val DEADLINE_LEAD_MILLIS = 24 * 3_600_000L

internal fun reminders(events: List<EventContentItem>, coursework: List<CourseworkContentItem>,
    classes: Boolean, deadlines: Boolean): List<Reminder> = buildList {
    if (classes) events.filter { !it.allDay && it.endMillis > it.startMillis }.forEach {
        add(Reminder(ReminderKind.CLASS, it.id, it.startMillis - CLASS_LEAD_MILLIS, it.startMillis))
    }
    if (deadlines) coursework.forEach {
        add(Reminder(ReminderKind.DEADLINE, it.id, it.dueMillis - DEADLINE_LEAD_MILLIS, it.dueMillis))
    }
}.sortedBy { it.triggerAt }

/** A late alarm still reminds about anything due since the last one, unless it has already begun. */
internal fun dueReminders(all: List<Reminder>, after: Long, now: Long): List<Reminder> =
    all.filter { it.triggerAt > after && it.triggerAt <= now && it.targetAt > now }

internal fun nextTrigger(all: List<Reminder>, now: Long): Long? = all.firstOrNull { it.triggerAt > now }?.triggerAt
