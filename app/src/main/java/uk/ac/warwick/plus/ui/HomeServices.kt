package uk.ac.warwick.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.components.*

@Composable
internal fun HomeServiceSummaries(services: Map<ServiceKind, ServiceState>, recovery: Map<ServiceKind, RecoveryState>,
    now: Long, onLogin: () -> Unit, onRecover: (SyncResource) -> Unit, onOpen: (String) -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val busState = services[ServiceKind.BUSES] ?: ServiceState()
    var showBuses by remember(busState.summaries) { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = fontScale > 1.3f || maxWidth < 280.dp
        val buses: @Composable (Modifier) -> Unit = { modifier ->
            ServiceSummaryCard(ServiceKind.BUSES, services[ServiceKind.BUSES] ?: ServiceState(),
                recovery.getValue(ServiceKind.BUSES), now, modifier, onLogin, { onRecover(SyncResource.BUSES) }, onOpen, { showBuses = true })
        }
        val print: @Composable (Modifier) -> Unit = { modifier ->
            ServiceSummaryCard(ServiceKind.PRINT, services[ServiceKind.PRINT] ?: ServiceState(),
                recovery.getValue(ServiceKind.PRINT), now, modifier, onLogin, { onRecover(SyncResource.PRINT) }, onOpen)
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            buses(Modifier.fillMaxWidth()); print(Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            buses(Modifier.weight(1f).fillMaxHeight()); print(Modifier.weight(1f).fillMaxHeight())
        }
    }
    if (showBuses) DetailsSheet({ showBuses = false }, contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp) {
        item { DetailEyebrow(stringResource(AppLabels.BUSES)) }
        busState.lastSynced?.let { updated -> item {
            Text(stringResource(R.string.bus_updated_at, updatedTimeLabel(updated)), style = MaterialTheme.typography.labelSmall)
        } }
        busState.summaries.forEach { entry -> item(key = entry.id) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.callout, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(entry.text, style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { DetailClose(stringResource(R.string.close_service_details)) { showBuses = false } }
    }
}

private val busRouteText = Regex("^(.+?) from (.+?) to (.+)$")

@Composable
private fun ServiceSummaryCard(kind: ServiceKind, state: ServiceState, recovery: RecoveryState, now: Long,
    modifier: Modifier, onLogin: () -> Unit, onRefresh: () -> Unit, onOpen: (String) -> Unit,
    onBuses: () -> Unit = {}) {
    val isBus = kind == ServiceKind.BUSES
    val label = stringResource(if (isBus) AppLabels.BUSES else AppLabels.PRINT)
    val destination = if (isBus) "" else state.url
    val actionLabel = stringResource(R.string.open_in_browser, label)
    val busActionLabel = stringResource(R.string.view_resource_details, label)
    SectionCard(label, modifier = modifier.testTag("home-${kind.tile}").then(
        if (isBus && state.summaries.isNotEmpty()) Modifier.clickable(role = Role.Button, onClickLabel = busActionLabel, onClick = onBuses)
        else if (destination.isBlank()) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = actionLabel) { onOpen(destination) }),
        icon = if (isBus) ContentIcons.bus else ContentIcons.print) {
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                state.lastSynced == null -> Text(stringResource(if (recovery.updating) R.string.loading_resource else R.string.service_not_loaded, label),
                    style = HomeTypography.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.summaries.isEmpty() -> Text(stringResource(if (isBus) R.string.no_bus_times else R.string.print_unavailable),
                    style = HomeTypography.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> state.summaries.take(if (isBus) 2 else 1).forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (entry.callout.isNotBlank()) Text(entry.callout, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val route = if (isBus) busRouteText.matchEntire(entry.text)?.groupValues else null
                        if (entry.text.isNotBlank()) Text(if (route == null) entry.text else "${route[1]} → ${route[3]}",
                            style = HomeTypography.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (route != null) Text(route[2], style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (isBus && state.lastSynced != null) {
                // The UI clock ticks every 30s and may precede a just-completed request.
                val age = now - state.lastSynced
                val old = age < -60_000 || age >= kind.refreshMillis || state.message != null || recovery.needsLogin
                Text(stringResource(if (old) R.string.bus_cached_at else R.string.bus_updated_at,
                    deadlineDateLabel(state.lastSynced, now, includeTime = true)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!isBus && destination.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.print_account), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(MeIcons.external, contentDescription = null, modifier = Modifier.size(12.dp))
            }
            ResourceRecoveryRow(recovery, onLogin, onRefresh)
        }
    }
}

internal fun upcomingCampusEvents(events: List<CampusEvent>, now: Long): List<CampusEvent> =
    events.filter { it.endMillis > now || it.startMillis >= now }
        .sortedWith(compareBy({ it.startMillis }, { it.id })).take(3)

internal fun campusEventTime(event: CampusEvent, now: Long): String {
    val start = atWarwick(event.startMillis).toLocalDate()
    val end = atWarwick(if (event.allDay && event.endMillis > event.startMillis) event.endMillis - 1 else event.endMillis).toLocalDate()
    val date = shortDateLabel(start, includeYear = start.year != atWarwick(now).year)
    if (event.allDay) return if (start == end) date else "$date – ${shortDateLabel(end, end.year != start.year)}"
    return if (start == end) "$date · ${timeLabel(event.startMillis)} – ${timeLabel(event.endMillis)}"
    else "${fullDateTimeLabel(event.startMillis)} – ${fullDateTimeLabel(event.endMillis)}"
}

@Composable
internal fun HomeCampusEvents(state: ServiceState, recovery: RecoveryState, now: Long,
    onSelect: (CampusEvent) -> Unit, onLogin: () -> Unit, onRefresh: () -> Unit) {
    val events = remember(state.events, now) { upcomingCampusEvents(state.events, now) }
    SectionCard(stringResource(AppLabels.EVENTS), icon = ContentIcons.calendar,
        tone = CardTone.Quiet, modifier = Modifier.testTag("home-campus-events")) {
        if (state.lastSynced == null) SectionEmptyRow(stringResource(
            if (recovery.updating) R.string.loading_resource else R.string.service_not_loaded, stringResource(AppLabels.EVENTS)))
        else if (events.isEmpty()) SectionEmptyRow(stringResource(R.string.no_campus_events))
        events.forEachIndexed { index, event ->
            if (index > 0) ListDivider()
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button,
                onClickLabel = stringResource(R.string.view_resource_details, stringResource(AppLabels.EVENTS)), onClick = { onSelect(event) })
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(event.title, style = HomeTypography.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(campusEventTime(event, now) + if (event.allDay) " · ${stringResource(R.string.all_day)}" else "",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (event.location.isNotBlank()) LocationLabel(event.location, style = MaterialTheme.typography.labelSmall)
                }
                Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.padding(horizontal = 12.dp)) { ResourceRecoveryRow(recovery, onLogin, onRefresh) }
    }
}

@Composable
internal fun CampusEventDetails(event: CampusEvent, onDismiss: () -> Unit, onOpen: ((String) -> Unit)?) {
    val cache = LocalFeedTextCache.current ?: remember { FeedTextCache() }
    val text by produceState<Pair<String, String>?>(null, event.description, cache) {
        value = event.description to withContext(Dispatchers.Default) { cache.plain(event.description, html = true) }
    }
    val plain = text?.takeIf { it.first == event.description }?.second.orEmpty()
    DetailsSheet(onDismiss, contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp) {
        item { DetailHeader(stringResource(AppLabels.EVENTS), event.title) }
        item { DetailField(stringResource(R.string.when_label),
            if (event.allDay) campusEventTime(event, System.currentTimeMillis()) + " · " + stringResource(R.string.all_day)
            else fullDateTimeLabel(event.startMillis) + " – " + fullDateTimeLabel(event.endMillis)) }
        if (event.location.isNotBlank()) item { DetailField(stringResource(R.string.location), event.location) }
        if (event.source.isNotBlank()) item { Text(event.source, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (plain.isNotBlank()) item { Text(plain, style = MaterialTheme.typography.bodyMedium) }
        if (event.url.isNotBlank()) item { ExternalLinkButton(event.url, stringResource(R.string.open_source_website), onOpen) }
        item { DetailClose(stringResource(R.string.close_service_details), onDismiss) }
    }
}
