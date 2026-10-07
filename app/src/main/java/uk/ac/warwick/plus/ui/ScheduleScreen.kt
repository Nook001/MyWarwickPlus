package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
internal fun ScheduleContent(state: SchedulePageState, today: LocalDate, now: Long, from: LocalDate,
    listState: LazyListState, feedback: @Composable () -> Unit = {}, onSelect: (EventContentItem) -> Unit) {
    val days = remember(state.events, from) { scheduleDays(state.events, from) }
    val conflicts = state.conflicts
    val nextId = remember(state.events, now) {
        nextTimedClass(state.events, now)?.id
    }
    LazyColumn(Modifier.fillMaxSize().testTag("schedule-list"), state = listState,
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.showFeedback) item { feedback() }
        if (state.lastSynced == null) item {
            DataEmptyState(if (state.busy) stringResource(R.string.loading_timetable) else stringResource(R.string.timetable_not_loaded))
        } else items(days, key = { it.date.toEpochDay() }, contentType = { "schedule-day" }) { day ->
            Column(Modifier.fillMaxWidth().testTag("schedule-day-${day.date}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(scheduleDateLabel(day.date, today).render())
                AppCard(Modifier.fillMaxWidth()) {
                    if (day.events.isEmpty()) Text(if (day.date == today) stringResource(R.string.no_classes_today) else stringResource(R.string.no_classes_on_day),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                    else Column {
                        day.events.forEachIndexed { index, event ->
                            if (index > 0) ListDivider(inset = 0.dp)
                            val status = classStatus(event, now, nextId, day.date)?.let { stringResource(it) }
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
    MetricListRow(onSelect, stringResource(R.string.view_class_details), Modifier.testTag("schedule-class-${date}-${event.id}"),
        compactTop = compactTop, metric = {
            Text(if (event.allDay) stringResource(R.string.all_day) else time.start, style = if (event.allDay) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
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
                if (time.continuesAfter) stringResource(R.string.continues_after) else null).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (conflict) Text(stringResource(R.string.class_overlap), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDateDialog(selected: LocalDate, onDismiss: () -> Unit, onDate: (LocalDate) -> Unit) {
    val picker = rememberSaveable(saver = listSaver<DatePickerState, Any>(
        save = { listOf(it.selectedDateMillis ?: Long.MIN_VALUE, it.displayedMonthMillis, it.displayMode == DisplayMode.Input) },
        restore = { DatePickerState(locale = Locale.UK,
            initialSelectedDateMillis = (it[0] as Long).takeUnless { date -> date == Long.MIN_VALUE },
            initialDisplayedMonthMillis = it[1] as Long,
            initialDisplayMode = if (it[2] as Boolean) DisplayMode.Input else DisplayMode.Picker) }
    )) { DatePickerState(locale = Locale.UK, initialSelectedDateMillis = pickerMillis(selected), initialDisplayMode = DisplayMode.Input) }
    DatePickerDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { picker.selectedDateMillis?.let { onDate(pickerDate(it)) } }, enabled = picker.selectedDateMillis != null,
            modifier = Modifier.testTag("schedule-date-confirm")) { Text(stringResource(R.string.go_to_date)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(AppActions.CANCEL)) } }) {
        DatePicker(picker, modifier = Modifier.verticalScroll(rememberScrollState()), title = { Text(stringResource(R.string.choose_date), Modifier.padding(24.dp)) },
            headline = null, showModeToggle = true)
    }
}
