package uk.ac.warwick.plus.ui

import android.text.Html
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onRefresh: () -> Unit, onMore: () -> Unit, onSelect: (FeedEntry) -> Unit, onOpen: (String) -> Unit) {
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
            if (kind != FeedKind.LIBRARY) OutlinedTextField(query, { query = it }, label = { Text("Search ${kind.label.lowercase()}") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
        when {
            state.lastSynced == null -> item { EmptyCard(if (state.loading) "Loading ${kind.label.lowercase()}…" else when (kind) {
                FeedKind.LIBRARY -> "Your Library summary hasn't loaded yet"
                else -> "${kind.label} haven't loaded yet"
            }, "Refresh to try again.") }
            state.entries.isEmpty() -> item { EmptyCard(if (kind == FeedKind.LIBRARY) "No Library items returned" else "No ${kind.label.lowercase()} returned",
                state.description.ifBlank { "This saved feed is empty. Check the source service for the full record." }) }
            filtered.isEmpty() -> item { EmptyCard("No matches", "Try another search or clear the search field.") }
            else -> items(filtered, key = { it.id }) { entry ->
                Card(onClick = { focus.clearFocus(); onSelect(entry) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = cardBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetails(kind: FeedKind, entry: FeedEntry, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.testTag("feed-details"), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text(kind.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            item { Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
            if (entry.moduleCode.isNotBlank()) item { Text("${entry.moduleCode} · ${entry.academicYear}") }
            if (entry.provider.isNotBlank()) item { Text(entry.provider) }
            if (entry.dateMillis != 0L) item { Text("${deadlineTime(entry.dateMillis)} · Warwick time") }
            if (entry.text.isNotBlank()) item { Text(feedText(entry)) }
            if (kind == FeedKind.MODULES) item { Text("${entry.announcementCount} announcements · ${entry.evaluationCount} evaluations returned. View their contents on the module site.") }
            safeExternalUrl(entry.url)?.let { url -> item {
                OutlinedButton(onClick = { onOpen(url) }, modifier = Modifier.fillMaxWidth()) { Text(if (kind == FeedKind.MODULES) "Open module in Moodle" else "Open source website") }
                Text(java.net.URI(url).host, style = MaterialTheme.typography.bodySmall)
            } }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close service details") } }
        }
    }
}
