package uk.ac.warwick.plus.ui

import androidx.compose.ui.res.pluralStringResource

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.platform.LocalFocusManager

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.data.*

@Composable
fun FeedContent(kind: FeedKind, state: FeedState, busy: Boolean, needsLogin: Boolean,
    onRefresh: () -> Unit, onMore: () -> Unit, onSelect: (FeedContentItem) -> Unit, onOpen: (String) -> Unit,
    onLogin: () -> Unit = {}) {
    var query by rememberSaveable(kind) { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val textById = rememberFeedText(state.entries)
    val filtered = remember(state.entries, query, textById) {
        val search = query.trim()
        state.entries.filter {
            search.isEmpty() || it.title.contains(search, true) ||
                (textById[it.id] ?: if (it.html) "" else it.text).contains(search, true) ||
                it.provider.contains(search, true) || it.moduleCode.contains(search, true)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("feed-list"), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
        item(contentType = "feed-header") {
            Text(when (kind) {
                FeedKind.MESSAGES -> stringResource(R.string.messages_feed_description)
                FeedKind.MODULES -> stringResource(R.string.modules_feed_description)
                FeedKind.LIBRARY -> stringResource(R.string.library_feed_description)
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (kind != FeedKind.LIBRARY) SearchField(query, { query = it }, stringResource(R.string.search_items, stringResource(kind.labelRes)),
                modifier = Modifier.padding(top = 12.dp)) { focus.clearFocus() }
        }
        if (needsLogin || state.message != null) item {
            DataRecoveryRow(state.lastSynced, state.message, state.loading, needsLogin, !busy,
                onLogin, onRefresh, state.olderPageFailed)
        }
        when {
            state.lastSynced == null -> item { DataEmptyState(if (state.loading) stringResource(R.string.loading_resource, stringResource(kind.labelRes)) else if (busy)
                stringResource(R.string.waiting_resource, stringResource(kind.labelRes)) else when (kind) {
                FeedKind.LIBRARY -> stringResource(R.string.library_not_loaded)
                else -> stringResource(R.string.resource_not_loaded, stringResource(kind.labelRes))
            }, action = if (!busy && !needsLogin && state.message == null) stringResource(AppActions.RETRY) else null, onAction = onRefresh) }
            state.entries.isEmpty() -> item { DataEmptyState(if (kind == FeedKind.LIBRARY) stringResource(R.string.no_library_items) else stringResource(R.string.no_resource_in_feed, stringResource(kind.labelRes)),
                state.description.takeIf { it.isNotBlank() }) }
            filtered.isEmpty() -> item { DataEmptyState(stringResource(R.string.no_matches), action = stringResource(AppActions.CLEAR_SEARCH), onAction = { query = ""; focus.clearFocus() }) }
            else -> items(filtered, key = { it.id }, contentType = { "feed-entry" }) { entry ->
                AppCard(onClick = { focus.clearFocus(); onSelect(entry) }, modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.legacyContent, actionLabel = stringResource(R.string.view_resource_details, stringResource(kind.labelRes))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val label = if (kind == FeedKind.MODULES) listOf(entry.moduleCode, entry.academicYear).filter { it.isNotBlank() }.joinToString(" · ") else entry.provider
                        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val text = textById[entry.id] ?: if (entry.html) "" else entry.text
                        if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (kind == FeedKind.MODULES) Text(pluralStringResource(R.plurals.announcement_count, entry.announcementCount, entry.announcementCount), style = MaterialTheme.typography.bodySmall)
                        if (entry.dateMillis != 0L) Text(fullDateTimeLabel(entry.dateMillis), style = MaterialTheme.typography.bodySmall)
                        if (kind == FeedKind.MESSAGES && state.webReadMillis != 0L && entry.dateMillis > state.webReadMillis)
                            Text(stringResource(R.string.unread_mywarwick), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (kind == FeedKind.MESSAGES && state.hasMore) item {
            OutlinedButton(onClick = onMore, enabled = !busy && !needsLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.load_older_messages)) }
        }
        if (kind == FeedKind.MESSAGES && state.entries.size >= 500) item { Text(stringResource(R.string.messages_cache_limit), style = MaterialTheme.typography.bodySmall) }
        val site = safeExternalUrl(state.url) ?: kind.websiteUrl()
        item { OutlinedButton(onClick = { onOpen(site) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_resource_website, stringResource(kind.labelRes))) } }
    }
}
