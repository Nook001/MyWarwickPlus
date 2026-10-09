package uk.ac.warwick.plus.ui

import androidx.compose.ui.res.pluralStringResource
import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.data.*

@Composable
fun FeedContent(kind: FeedKind, state: FeedState, busy: Boolean, needsLogin: Boolean,
    onRefresh: () -> Unit, onMore: () -> Unit, onSelect: (FeedContentItem) -> Unit, onOpen: (String) -> Unit,
    onLogin: () -> Unit = {}, today: LocalDate = atWarwick(System.currentTimeMillis()).toLocalDate()) {
    var query by rememberSaveable(kind) { mutableStateOf("") }
    var source by rememberSaveable(kind) { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val messages = kind == FeedKind.MESSAGES
    val textById = rememberFeedText(state.entries)
    val sources = remember(state.entries, source) {
        (state.entries.map { it.provider }.filter { it.isNotBlank() } + listOfNotNull(source.takeIf { it.isNotBlank() }))
            .distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    val filtered = remember(kind, state.entries, query, source, textById) {
        val search = query.trim()
        state.entries.filter {
            (!messages || source.isEmpty() || it.provider == source) &&
                (search.isEmpty() || it.title.contains(search, true) ||
                (textById[it.id] ?: if (it.html) "" else it.text).contains(search, true) ||
                it.provider.contains(search, true) || it.moduleCode.contains(search, true))
        }
    }
    val sourceColours = rememberSourceColours(sources)
    val groups = remember(messages, filtered, today) {
        if (!messages) emptyList() else filtered.partition {
            it.dateMillis != 0L && atWarwick(it.dateMillis).toLocalDate() > today.minusDays(7)
        }.let { (recent, earlier) -> listOf(R.string.last_seven_days to recent, R.string.earlier to earlier).filter { it.second.isNotEmpty() } }
    }
    // Messages rows join into grouped cards, so their gaps are explicit instead of list spacing.
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize().fadingTopEdge(listState).testTag("feed-list"), state = listState,
        contentPadding = if (messages) Spacing.compactPage else PaddingValues(Spacing.page),
        verticalArrangement = if (messages) Arrangement.Top else Arrangement.spacedBy(Spacing.item)) {
        item(contentType = "feed-header") {
            Column(Modifier.padding(bottom = if (messages) 4.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!messages) Text(stringResource(if (kind == FeedKind.LIBRARY) R.string.library_feed_description
                    else R.string.modules_feed_description), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (kind != FeedKind.LIBRARY) SearchField(query, { query = it }, stringResource(R.string.search_items, stringResource(kind.labelRes)),
                    modifier = Modifier.padding(top = if (messages) 0.dp else 4.dp), compact = messages) { focus.clearFocus() }
                if (messages && sources.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("") + sources).forEach { provider ->
                        FilterChip(selected = source == provider, onClick = { source = provider; focus.clearFocus() },
                            label = { Text(if (provider.isEmpty()) stringResource(R.string.all_sources) else provider,
                                style = InboxTypography.preview) })
                    }
                }
            }
        }
        if (needsLogin || state.message != null) item {
            Box(Modifier.padding(bottom = if (messages) 8.dp else 0.dp)) {
                DataRecoveryRow(state.lastSynced, state.message, state.loading, needsLogin, !busy,
                    onLogin, onRefresh, state.olderPageFailed)
            }
        }
        when {
            state.lastSynced == null -> item { DataEmptyState(if (state.loading) stringResource(R.string.loading_resource, stringResource(kind.labelRes)) else if (busy)
                stringResource(R.string.waiting_resource, stringResource(kind.labelRes)) else when (kind) {
                FeedKind.LIBRARY -> stringResource(R.string.library_not_loaded)
                else -> stringResource(R.string.resource_not_loaded, stringResource(kind.labelRes))
            }, action = if (!busy && !needsLogin && state.message == null) stringResource(AppActions.RETRY) else null, onAction = onRefresh) }
            state.entries.isEmpty() -> item { DataEmptyState(if (kind == FeedKind.LIBRARY) stringResource(R.string.no_library_items) else stringResource(R.string.no_resource_in_feed, stringResource(kind.labelRes)),
                state.description.takeIf { it.isNotBlank() }) }
            filtered.isEmpty() -> item { DataEmptyState(stringResource(R.string.no_matches),
                action = stringResource(if (messages && source.isNotEmpty()) AppActions.CLEAR_FILTERS else AppActions.CLEAR_SEARCH),
                onAction = { query = ""; source = ""; focus.clearFocus() }) }
            messages -> groups.forEach { (label, group) ->
                item(key = "group-$label", contentType = "feed-group") {
                    Box(Modifier.padding(top = 8.dp, bottom = 6.dp)) { SectionLabel(stringResource(label)) }
                }
                itemsIndexed(group, key = { _, entry -> entry.id }, contentType = { _, _ -> "feed-entry" }) { index, entry ->
                    GroupedListItem(first = index == 0, last = index == group.lastIndex) {
                        MessageRow(entry, textById[entry.id] ?: if (entry.html) "" else entry.text, today,
                            sourceColours[entry.provider], state.webReadMillis != 0L && entry.dateMillis > state.webReadMillis) {
                            focus.clearFocus(); onSelect(entry)
                        }
                    }
                }
            }
            else -> items(filtered, key = { it.id }, contentType = { "feed-entry" }) { entry ->
                AppCard(onClick = { focus.clearFocus(); onSelect(entry) }, modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.feedContent, actionLabel = stringResource(R.string.view_resource_details, stringResource(kind.labelRes))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val label = if (kind == FeedKind.MODULES) listOf(entry.moduleCode, entry.academicYear).filter { it.isNotBlank() }.joinToString(" · ") else entry.provider
                        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val text = if (kind == FeedKind.MODULES) stringResource(R.string.module_site_hint)
                            else textById[entry.id] ?: if (entry.html) "" else entry.text
                        if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (kind == FeedKind.MODULES) Text(pluralStringResource(R.plurals.announcement_count, entry.announcementCount, entry.announcementCount), style = MaterialTheme.typography.bodySmall)
                        if (entry.dateMillis != 0L) Text(fullDateTimeLabel(entry.dateMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        val footer = Modifier.fillMaxWidth().padding(top = if (messages) 8.dp else 0.dp)
        if (kind == FeedKind.MESSAGES && state.hasMore) item {
            OutlinedButton(onClick = onMore, enabled = !busy && !needsLogin, modifier = footer.padding(top = 4.dp)) {
                Text(stringResource(R.string.load_older_messages), style = InboxTypography.preview)
            }
        }
        if (kind == FeedKind.MESSAGES && state.entries.size >= 500) item { Text(stringResource(R.string.messages_cache_limit), style = MaterialTheme.typography.bodySmall, modifier = footer) }
        val site = safeExternalUrl(state.url) ?: kind.websiteUrl()
        item { OutlinedButton(onClick = { onOpen(site) }, modifier = footer) {
            Text(stringResource(R.string.open_resource_website, stringResource(kind.labelRes)),
                style = if (messages) InboxTypography.preview else MaterialTheme.typography.labelLarge)
        } }
    }
}

/** Sources share the module hues so a provider keeps one colour across the filter and the list. */
@Composable
private fun rememberSourceColours(sources: List<String>): Map<String, Color> {
    val onLight = appCardColours(CardTone.Normal).background.luminance() > .5f
    return remember(sources, onLight) { sources.withIndex().associate { (index, name) -> name to moduleColour(index, onLight) } }
}

@Composable
private fun MessageRow(entry: FeedContentItem, preview: String, today: LocalDate, sourceColour: Color?,
    unread: Boolean, onSelect: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.view_resource_details,
        stringResource(FeedKind.MESSAGES.labelRes)), onClick = onSelect).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (sourceColour != null) Box(Modifier.size(7.dp).background(sourceColour, CircleShape))
            Text(entry.provider, modifier = Modifier.weight(1f), style = InboxTypography.metadata,
                color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (entry.dateMillis != 0L) {
                val date = atWarwick(entry.dateMillis).toLocalDate()
                Text(if (date == today) timeLabel(entry.dateMillis) else shortDateLabel(date, includeYear = date.year != today.year),
                    style = InboxTypography.metadata, color = scheme.onSurfaceVariant)
            }
        }
        Text(entry.title, style = InboxTypography.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (preview.isNotBlank()) Text(preview, style = InboxTypography.preview, color = scheme.onSurfaceVariant,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (unread) Text(stringResource(R.string.unread_mywarwick), style = InboxTypography.metadata, color = scheme.primary)
    }
}
