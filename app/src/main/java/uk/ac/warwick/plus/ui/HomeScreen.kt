package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*

@Composable
internal fun HomeContent(state: HomePageState, today: LocalDate, now: Long, onSelect: (EventContentItem) -> Unit,
    onCoursework: (CourseworkContentItem) -> Unit, onAllCoursework: () -> Unit, onPastCoursework: () -> Unit,
    onLogin: () -> Unit, onRecover: (SyncResource) -> Unit, onOpen: (String) -> Unit,
    onMessage: (FeedContentItem) -> Unit, onAllMessages: () -> Unit) {
    val next = remember(state.events, now) { currentOrNextClass(state.events, now) }
    val nextId = remember(state.events, now) { nextTimedClass(state.events, now)?.id }
    val agenda = remember(state.events, now) { homeAgenda(state.events, now) }
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
        item(key = "next") {
            when {
                state.lastSynced == null -> SectionCard(stringResource(AppLabels.NEXT), modifier = Modifier.testTag("home-next-section"), headingTag = "next", icon = ContentIcons.clock) {
                    SectionEmptyRow(if (state.busy) stringResource(R.string.loading_timetable) else stringResource(R.string.timetable_not_loaded))
                }
                next == null -> SectionCard(stringResource(AppLabels.NEXT), modifier = Modifier.testTag("home-next-section"), headingTag = "next", icon = ContentIcons.clock) { SectionEmptyRow(stringResource(R.string.no_upcoming_classes)) }
                else -> NextClassCard(next, now) { onSelect(next) }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                ResourceRecoveryRow(state.timetableRecovery, onLogin) { onRecover(SyncResource.TIMETABLE) }
            }
            Spacer(Modifier.height(12.dp))
            HomeQuickLinks(onOpen)
        }
        if (state.lastSynced != null && (agenda.day.events.isNotEmpty() || agenda.weekendMessage != null)) item(key = "agenda") {
            SectionCard(scheduleDateLabel(agenda.day.date, today).render(), modifier = Modifier.testTag("home-today-section"),
                headingTag = "today", icon = ContentIcons.calendar, trailing = {
                if (agenda.day.events.isNotEmpty()) Text(classCountLabel(agenda.day.events.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) {
                agenda.weekendMessage?.let { SectionEmptyRow(stringResource(it)) }
                agenda.day.events.forEachIndexed { index, event ->
                    if (index > 0) ListDivider()
                    val status = classStatus(event, now, nextId, agenda.day.date)
                        ?.takeUnless { it == AppLabels.NEXT }?.let { stringResource(it) }
                    ScheduleClassRow(event, agenda.day.date, status, event.id in conflicts,
                        compactTop = index == 0, density = ListRowDensity.Compact) { onSelect(event) }
                }
            }
        }
        item(key = "deadlines") {
            SectionCard(stringResource(AppLabels.DEADLINES), modifier = Modifier.testTag("home-deadlines-section"),
                headingTag = "deadlines", icon = ContentIcons.deadlines, trailing = {
                SectionAllAction(stringResource(R.string.view_all_tasks), onAllCoursework)
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
        if (state.messages.entries.isNotEmpty()) item(key = "messages") {
            HomeMessages(state.messages, state.messagesRecovery, today, onMessage, onAllMessages, onLogin) {
                onRecover(SyncResource.MESSAGES)
            }
        }
    }
}

@Composable
private fun NextClassCard(event: EventContentItem, now: Long, onSelect: () -> Unit) {
    val identity = classIdentity(event)
    val time = classTimeRange(event, includeWeekday = true)
    val date = atWarwick(event.startMillis).toLocalDate()
    val today = atWarwick(now).toLocalDate()
    val whenLabel = when {
        date <= today -> time
        date == today.plusDays(1) -> "${stringResource(R.string.tomorrow)} · $time"
        else -> "${weekdayDateLabel(date, includeYear = date.year != today.year)} · $time"
    }
    val colours = appCardColours(CardTone.Featured)
    AppCard(onClick = onSelect, tone = CardTone.Featured, shape = AppShapes.featured,
        modifier = Modifier.fillMaxWidth().testTag("next-class-card"), actionLabel = stringResource(R.string.view_class_details)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(ContentIcons.clock, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (isClassNow(event, now)) stringResource(AppLabels.NOW) else stringResource(AppLabels.NEXT), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium)
                Text(whenLabel,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).testTag("next-when"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(identity.name, style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp), fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-name"))
                if (identity.code.isNotBlank()) Text(identity.code, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp).alignByBaseline().testTag("next-code"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                LocationLabel(event.location, modifier = Modifier.weight(1f).testTag("next-location"),
                    colour = colours.foreground)
                DetailsArrow(Modifier.testTag("next-details-chevron"), size = 20.dp, tint = colours.foreground)
            }
        }
    }
}
