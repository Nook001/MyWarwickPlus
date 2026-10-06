package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Locale
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.EventContentItem
import uk.ac.warwick.plus.data.EventEntity

internal val CalendarPickerIcon = ImageVector.Builder("ChooseDate", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 21f); lineTo(4f, 21f); close()
        moveTo(4f, 10f); lineTo(20f, 10f)
        moveTo(8f, 2f); lineTo(8f, 7f); moveTo(16f, 2f); lineTo(16f, 7f)
        moveTo(8f, 13f); lineTo(10f, 13f); moveTo(14f, 13f); lineTo(16f, 13f)
        moveTo(8f, 17f); lineTo(10f, 17f); moveTo(14f, 17f); lineTo(16f, 17f)
    }
}.build()

@Composable
fun ScheduleContent(state: TimetableState, today: LocalDate, now: Long, from: LocalDate,
    listState: LazyListState, feedback: @Composable () -> Unit = {}, onSelect: (EventEntity) -> Unit) {
    // Preserve the existing entity-based entry point; the app uses the value-only overload below.
    ScheduleContent(state.schedulePage(), today, now, from, listState, feedback) { item ->
        onSelect(item as? EventEntity ?: EventEntity().apply {
            id = item.id; title = item.title; module = item.module; moduleName = item.moduleName
            location = item.location; locationUrl = item.locationUrl
            startMillis = item.startMillis; endMillis = item.endMillis
            allDay = item.allDay; academicWeek = item.academicWeek
        })
    }
}

@Composable
internal fun ScheduleContent(state: SchedulePageState, today: LocalDate, now: Long, from: LocalDate,
    listState: LazyListState, feedback: @Composable () -> Unit = {}, onSelect: (EventContentItem) -> Unit) {
    val days = remember(state.events, from) { scheduleDays(state.events, from) }
    val conflicts = remember(state.events) { conflictingEventIds(state.events) }
    val nextId = remember(state.events, now) {
        nextTimedClass(state.events, now)?.id
    }
    LazyColumn(Modifier.fillMaxSize().testTag("schedule-list"), state = listState,
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showFeedback) item { feedback() }
        if (state.lastSynced == null) item {
            DataEmptyState(if (state.busy) "Loading timetable…" else "Timetable hasn't loaded yet")
        } else items(days, key = { it.date.toEpochDay() }) { day ->
            Column(Modifier.fillMaxWidth().testTag("schedule-day-${day.date}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(scheduleDateLabel(day.date, today))
                AppCard(Modifier.fillMaxWidth()) {
                    if (day.events.isEmpty()) Text(if (day.date == today) "No classes today" else "No classes on this day",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                    else Column {
                        day.events.forEachIndexed { index, event ->
                            if (index > 0) ListDivider(inset = 0.dp)
                            val status = when {
                                !event.allDay && event.startMillis <= now && event.endMillis > now && day.date == today -> AppLabels.NOW
                                event.id == nextId && atWarwick(event.startMillis).toLocalDate() == day.date -> AppLabels.NEXT
                                else -> null
                            }
                            ScheduleClassRow(event, day.date, status, event.id in conflicts) { onSelect(event) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ScheduleClassRow(event: EventContentItem, date: LocalDate, status: String?, conflict: Boolean,
    compactTop: Boolean = false, onSelect: () -> Unit) {
    val identity = classIdentity(event)
    val time = scheduleTime(event, date)
    MetricListRow(onSelect, "View class details", Modifier.testTag("schedule-class-${date}-${event.id}"),
        compactTop = compactTop, metric = {
            Text(if (event.allDay) "All day" else time.start, style = if (event.allDay) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
            if (!event.allDay) Text(time.end, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false)
    }) {
        Text(identity.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (status != null) Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            if (identity.code.isNotBlank()) Text(identity.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!event.allDay && (time.continuesBefore || time.continuesAfter)) Text(
            listOfNotNull(if (time.continuesBefore) "Continues from previous day" else null,
                if (time.continuesAfter) "Continues tomorrow" else null).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (conflict) Text("Overlaps another class", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDateDialog(selected: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val picker = remember { DatePickerState(locale = Locale.UK, initialSelectedDateMillis = pickerMillis(selected), initialDisplayMode = DisplayMode.Input) }
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { onDate(pickerDate(it)) } }, enabled = picker.selectedDateMillis != null,
            modifier = Modifier.testTag("schedule-date-confirm")) { Text("Go to date") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppActions.CANCEL) } }) {
        DatePicker(picker, modifier = Modifier.verticalScroll(rememberScrollState()), title = { Text("Choose date", Modifier.padding(24.dp)) },
            headline = null, showModeToggle = true)
    }
}
