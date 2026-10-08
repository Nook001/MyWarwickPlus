package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    val heroDate = next?.let { atWarwick(it.startMillis).toLocalDate() }
    val timeline = remember(state.events, heroDate, now) { heroDate?.let { dayTimeline(state.events, it, now) } }
    val agendaEvents = remember(agenda, next) { agenda.day.events.filter { it.id != next?.id } }
    val featuredColourOf = rememberModuleColours(state.events, appCardColours(CardTone.Featured).background.luminance() > .5f)
    val colourOf = rememberModuleColours(state.events, appCardColours(CardTone.Normal).background.luminance() > .5f)
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
                else -> NextClassCard(next, timeline, today, now, featuredColourOf) { onSelect(next) }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                ResourceRecoveryRow(state.timetableRecovery, onLogin) { onRecover(SyncResource.TIMETABLE) }
            }
            Spacer(Modifier.height(12.dp))
            HomeQuickLinks(onOpen)
        }
        if (state.lastSynced != null && (agendaEvents.isNotEmpty() || agenda.weekendMessage != null)) item(key = "agenda") {
            val title = if (agenda.day.date == today && heroDate == today) stringResource(R.string.later_today)
                else scheduleDateLabel(agenda.day.date, today).render()
            SectionCard(title, modifier = Modifier.testTag("home-today-section"),
                headingTag = "today", icon = ContentIcons.calendar, trailing = {
                if (agendaEvents.isNotEmpty()) Text(classCountLabel(agendaEvents.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) {
                agenda.weekendMessage?.let { SectionEmptyRow(stringResource(it)) }
                agendaEvents.forEachIndexed { index, event ->
                    if (index > 0) ListDivider()
                    val status = classStatus(event, now, nextId, agenda.day.date)
                        ?.takeUnless { it == AppLabels.NEXT }?.let { stringResource(it) }
                    ScheduleClassRow(event, agenda.day.date, status, event.id in conflicts,
                        compactTop = index == 0, density = ListRowDensity.Compact, accent = colourOf(event)) { onSelect(event) }
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
