package uk.ac.warwick.plus.ui

import android.text.Html
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
import uk.ac.warwick.plus.data.*

fun feedText(entry: FeedEntry): String = if (entry.html) Html.fromHtml(entry.text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\uFFFC", "").trim() else entry.text

@Composable
fun FeedContent(kind: FeedKind, state: FeedState, busy: Boolean, needsLogin: Boolean,
    onRefresh: () -> Unit, onMore: () -> Unit, onSelect: (FeedEntry) -> Unit, onOpen: (String) -> Unit,
    onLogin: () -> Unit = {}) {
    var query by rememberSaveable(kind) { mutableStateOf("") }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val filtered = remember(state.entries, query) { state.entries.filter {
        query.isBlank() || listOf(it.title, feedText(it), it.provider, it.moduleCode).any { field -> field.contains(query.trim(), true) }
    } }
    LazyColumn(Modifier.fillMaxSize().testTag("feed-list"), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
        item {
            Text(when (kind) {
                FeedKind.MESSAGES -> "Messages from MyWarwick. Viewing here doesn't mark messages read on the website."
                FeedKind.MODULES -> "Modules returned by MyWarwick. Open Moodle for learning materials and announcements."
                FeedKind.LIBRARY -> "Your Library account summary from MyWarwick."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (kind != FeedKind.LIBRARY) SearchField(query, { query = it }, "Search ${kind.label.lowercase()}",
                modifier = Modifier.padding(top = 12.dp)) { focus.clearFocus() }
        }
        if (needsLogin || state.message != null) item {
            DataRecoveryRow(state.lastSynced, state.message, state.loading, needsLogin, !busy,
                onLogin, onRefresh, state.olderPageFailed)
        }
        when {
            state.lastSynced == null -> item { DataEmptyState(if (state.loading) "Loading ${kind.label.lowercase()}…" else if (busy)
                "Waiting to load ${kind.label.lowercase()}…" else when (kind) {
                FeedKind.LIBRARY -> "Your Library summary hasn't loaded yet"
                else -> "${kind.label} haven't loaded yet"
            }, action = if (!busy && !needsLogin && state.message == null) "Retry" else null, onAction = onRefresh) }
            state.entries.isEmpty() -> item { DataEmptyState(if (kind == FeedKind.LIBRARY) "No Library items returned" else "No ${kind.label.lowercase()} in this feed",
                state.description.takeIf { it.isNotBlank() }) }
            filtered.isEmpty() -> item { DataEmptyState("No matches", action = "Clear search", onAction = { query = ""; focus.clearFocus() }) }
            else -> items(filtered, key = { it.id }) { entry ->
                AppCard(onClick = { focus.clearFocus(); onSelect(entry) }, modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.legacyContent, actionLabel = "View ${kind.label.lowercase()} details") {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val label = if (kind == FeedKind.MODULES) listOf(entry.moduleCode, entry.academicYear).filter { it.isNotBlank() }.joinToString(" · ") else entry.provider
                        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val text = feedText(entry)
                        if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (kind == FeedKind.MODULES) Text("${entry.announcementCount} announcements returned", style = MaterialTheme.typography.bodySmall)
                        if (entry.dateMillis != 0L) Text(deadlineTime(entry.dateMillis), style = MaterialTheme.typography.bodySmall)
                        if (kind == FeedKind.MESSAGES && state.webReadMillis != 0L && entry.dateMillis > state.webReadMillis)
                            Text("Unread on MyWarwick", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (kind == FeedKind.MESSAGES && state.hasMore) item {
            OutlinedButton(onClick = onMore, enabled = !busy && !needsLogin, modifier = Modifier.fillMaxWidth()) { Text("Load older messages") }
        }
        if (kind == FeedKind.MESSAGES && state.entries.size >= 500) item { Text("500 messages saved. Open MyWarwick for more history.", style = MaterialTheme.typography.bodySmall) }
        val site = safeExternalUrl(state.url) ?: when (kind) {
            FeedKind.LIBRARY -> "https://warwick.ac.uk/services/library/account"
            FeedKind.MODULES -> "https://moodle.warwick.ac.uk/"
            FeedKind.MESSAGES -> "https://my.warwick.ac.uk/alerts"
        }
        item { OutlinedButton(onClick = { onOpen(site) }, modifier = Modifier.fillMaxWidth()) { Text("Open ${kind.label.lowercase()} website") } }
    }
}
