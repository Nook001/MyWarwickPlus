package uk.ac.warwick.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeContent(state: TimetableState, today: LocalDate, now: Long, onSelect: (EventEntity) -> Unit,
    onCoursework: (CourseworkEntity) -> Unit, onAllCoursework: () -> Unit, onPastCoursework: () -> Unit,
    onLogin: () -> Unit, onRecover: (SyncResource) -> Unit, onOpen: (String) -> Unit) {
    val next = currentOrNextClass(state.events, now)
    val nextId = nextTimedClass(state.events, now)?.id
    val todayEvents = eventsOnDate(state.events, today)
    val conflicts = remember(state.events) { conflictingEventIds(state.events) }
    val upcoming = state.coursework.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }.take(3)
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"),
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(Spacing.homeSection)) {
        item {
            when {
                state.lastSynced == null -> SectionCard("Next", modifier = Modifier.testTag("home-next-section"), headingTag = "next") {
                    SectionEmptyRow(if (state.busy) "Loading timetable…" else "Timetable hasn't loaded yet")
                }
                next == null -> SectionCard("Next", modifier = Modifier.testTag("home-next-section"), headingTag = "next") { SectionEmptyRow("No upcoming classes") }
                else -> NextClassCard(next, now) { onSelect(next) }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                ResourceRecoveryRow(state, SyncResource.TIMETABLE, onLogin) { onRecover(SyncResource.TIMETABLE) }
            }
            Spacer(Modifier.height(12.dp))
            HomeQuickLinks(onOpen)
        }
        if (state.lastSynced != null) item {
            SectionCard("Today", modifier = Modifier.testTag("home-today-section"), headingTag = "today", trailing = {
                Text(classCountLabel(todayEvents.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) {
                if (todayEvents.isEmpty()) SectionEmptyRow("No classes today", Modifier.testTag("today-empty"))
                else todayEvents.forEachIndexed { index, event ->
                    if (index > 0) ListDivider()
                    val status = when {
                        !event.allDay && event.startMillis <= now && event.endMillis > now -> "Now"
                        event.id == nextId -> "Next"
                        else -> null
                    }
                    ScheduleClassRow(event, today, status, event.id in conflicts, compactTop = index == 0) { onSelect(event) }
                }
            }
        }
        item {
            SectionCard("Deadlines", modifier = Modifier.testTag("home-deadlines-section"), headingTag = "deadlines", trailing = {
                Row(Modifier.widthIn(min = 48.dp).heightIn(min = 28.dp)
                    .clickable(role = Role.Button, onClickLabel = "View all coursework", onClick = onAllCoursework)
                    .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End) {
                    Text("All", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }) {
                when {
                    state.coursework.lastSynced == null -> SectionEmptyRow(if (state.busy) "Loading coursework…" else "Coursework hasn't loaded yet")
                    upcoming.isEmpty() -> SectionEmptyRow("No upcoming deadlines in this feed")
                    else -> upcoming.forEachIndexed { index, entry ->
                        if (index > 0) ListDivider()
                        DeadlineRow(entry, now, DeadlinePresentation.Home, compactTop = index == 0, modifier = Modifier.testTag("home-deadline-${entry.id}")) { onCoursework(entry) }
                    }
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    ResourceRecoveryRow(state, SyncResource.COURSEWORK, onLogin) { onRecover(SyncResource.COURSEWORK) }
                }
                val recentStart = atWarwick(now).minusDays(7).toInstant().toEpochMilli()
                val recentPast = state.coursework.entries.count { it.dueMillis in recentStart until now }
                if (state.coursework.lastSynced != null && recentPast > 0) TextButton(onClick = onPastCoursework,
                    modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Recently passed · $recentPast", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(event: EventEntity, now: Long, onSelect: () -> Unit) {
    val name = event.moduleName.ifBlank { event.title }
    val code = if (event.moduleName.isNotBlank() && event.title != name) event.title
        else event.module.takeUnless { it == name }.orEmpty()
    val endDate = atWarwick(event.endMillis).toLocalDate()
    val end = if (endDate != atWarwick(event.startMillis).toLocalDate())
        "${endDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))} ${timeLabel(event.endMillis)}"
        else timeLabel(event.endMillis)
    val time = if (event.allDay) "All day" else "${timeLabel(event.startMillis)} – $end"
    val colours = appCardColours(CardTone.Featured)
    AppCard(onClick = onSelect, tone = CardTone.Featured, shape = AppShapes.featured,
        modifier = Modifier.fillMaxWidth().testTag("next-class-card"), actionLabel = "View class details") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (event.startMillis <= now) "Now" else "Next", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                Text(if (event.startMillis <= now) time else "${nextClassLabel(event, now)} · $time",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-when"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-name"))
                if (code.isNotBlank()) Text(code, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp).alignByBaseline().testTag("next-code"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(event.location.ifBlank { "Location not provided" }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("next-location"))
                DetailsArrow(Modifier.testTag("next-details-chevron"), size = 20.dp, tint = colours.foreground)
            }
        }
    }
}
