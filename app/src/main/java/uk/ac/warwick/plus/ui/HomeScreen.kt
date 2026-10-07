package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*

@Composable
internal fun HomeContent(state: HomePageState, today: LocalDate, now: Long, onSelect: (EventContentItem) -> Unit,
    onCoursework: (CourseworkContentItem) -> Unit, onAllCoursework: () -> Unit, onPastCoursework: () -> Unit,
    onLogin: () -> Unit, onRecover: (SyncResource) -> Unit, onOpen: (String) -> Unit) {
    val next = remember(state.events, now) { currentOrNextClass(state.events, now) }
    val nextId = remember(state.events, now) { nextTimedClass(state.events, now)?.id }
    val todayEvents = remember(state.events, today) { eventsOnDate(state.events, today) }
    val conflicts = state.conflicts
    val upcoming = remember(state.coursework.entries, now) {
        state.coursework.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }.take(3)
    }
    val recentPast = remember(state.coursework.entries, now) {
        val recentStart = atWarwick(now).minusDays(7).toInstant().toEpochMilli()
        state.coursework.entries.count { it.dueMillis in recentStart until now }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"),
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(Spacing.homeSection)) {
        item {
            when {
                state.lastSynced == null -> SectionCard(stringResource(AppLabels.NEXT), modifier = Modifier.testTag("home-next-section"), headingTag = "next") {
                    SectionEmptyRow(if (state.busy) stringResource(R.string.loading_timetable) else stringResource(R.string.timetable_not_loaded))
                }
                next == null -> SectionCard(stringResource(AppLabels.NEXT), modifier = Modifier.testTag("home-next-section"), headingTag = "next") { SectionEmptyRow(stringResource(R.string.no_upcoming_classes)) }
                else -> NextClassCard(next, now) { onSelect(next) }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                ResourceRecoveryRow(state.timetableRecovery, onLogin) { onRecover(SyncResource.TIMETABLE) }
            }
            Spacer(Modifier.height(12.dp))
            HomeQuickLinks(onOpen)
        }
        if (state.lastSynced != null) item {
            SectionCard(stringResource(AppLabels.TODAY), modifier = Modifier.testTag("home-today-section"), headingTag = "today", trailing = {
                Text(classCountLabel(todayEvents.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) {
                if (todayEvents.isEmpty()) SectionEmptyRow(stringResource(R.string.no_classes_today), Modifier.testTag("today-empty"))
                else todayEvents.forEachIndexed { index, event ->
                    if (index > 0) ListDivider()
                    val status = classStatus(event, now, nextId, today)?.let { stringResource(it) }
                    ScheduleClassRow(event, today, status, event.id in conflicts, compactTop = index == 0) { onSelect(event) }
                }
            }
        }
        item {
            SectionCard(stringResource(AppLabels.DEADLINES), modifier = Modifier.testTag("home-deadlines-section"), headingTag = "deadlines", trailing = {
                Row(Modifier.widthIn(min = 48.dp).heightIn(min = 28.dp)
                    .clickable(role = Role.Button, onClickLabel = "View all coursework", onClick = onAllCoursework)
                    .padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End) {
                    Text(stringResource(R.string.action_all), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }) {
                when {
                    state.coursework.lastSynced == null -> SectionEmptyRow(if (state.busy) stringResource(R.string.loading_coursework) else stringResource(R.string.coursework_not_loaded))
                    upcoming.isEmpty() -> SectionEmptyRow(stringResource(R.string.no_deadlines_in_feed))
                    else -> upcoming.forEachIndexed { index, entry ->
                        if (index > 0) ListDivider()
                        DeadlineRow(entry, now, DeadlinePresentation.Home, compactTop = index == 0, modifier = Modifier.testTag("home-deadline-${entry.id}")) { onCoursework(entry) }
                    }
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    ResourceRecoveryRow(state.courseworkRecovery, onLogin) { onRecover(SyncResource.COURSEWORK) }
                }
                if (state.coursework.lastSynced != null && recentPast > 0) TextButton(onClick = onPastCoursework,
                    modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(stringResource(R.string.recently_passed, recentPast), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(event: EventContentItem, now: Long, onSelect: () -> Unit) {
    val identity = classIdentity(event)
    val time = classTimeRange(event, includeWeekday = true)
    val colours = appCardColours(CardTone.Featured)
    AppCard(onClick = onSelect, tone = CardTone.Featured, shape = AppShapes.featured,
        modifier = Modifier.fillMaxWidth().testTag("next-class-card"), actionLabel = stringResource(R.string.view_class_details)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isClassNow(event, now)) stringResource(AppLabels.NOW) else stringResource(AppLabels.NEXT), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                Text(if (event.startMillis <= now) time else "${nextClassLabel(event, now).render()} · $time",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-when"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(identity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-name"))
                if (identity.code.isNotBlank()) Text(identity.code, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp).alignByBaseline().testTag("next-code"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(event.location.ifBlank { stringResource(R.string.location_not_provided) }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("next-location"))
                DetailsArrow(Modifier.testTag("next-details-chevron"), size = 20.dp, tint = colours.foreground)
            }
        }
    }
}
