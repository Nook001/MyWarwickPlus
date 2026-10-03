package uk.ac.warwick.plus.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.safeCourseworkUrl
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun deadlineLabel(due: Long, now: Long): String {
    if (due < now) return "Deadline passed"
    val days = ChronoUnit.DAYS.between(atWarwick(now).toLocalDate(), atWarwick(due).toLocalDate())
    return when (days) {
        0L -> "Due today"; 1L -> "Due tomorrow"; else -> "Due in $days days"
    }
}

fun deadlineTime(due: Long): String = atWarwick(due).format(DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.UK))

@Composable
fun CourseworkRow(entry: CourseworkEntity, now: Long, onSelect: () -> Unit) {
    Card(onClick = onSelect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(deadlineLabel(entry.dueMillis, now), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(deadlineTime(entry.dueMillis), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CourseworkContent(state: CourseworkState, now: Long, busy: Boolean, onRefresh: () -> Unit,
    onSelect: (CourseworkEntity) -> Unit) {
    val upcoming = state.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }
    val past = state.entries.filter { it.dueMillis < now }.sortedByDescending { it.dueMillis }
    LazyColumn(Modifier.fillMaxSize().testTag("coursework-list"), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
        item {
            Text("Deadlines from MyWarwick", style = MaterialTheme.typography.titleMedium)
            Text("Times shown in Warwick time. This feed covers a limited period and doesn't report submission status.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.message?.let { message -> item {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRefresh, enabled = !busy) { Text("Retry coursework") }
        } }
        when {
            state.lastSynced == null -> item {
                EmptyCard(if (busy) "Loading coursework…" else "Coursework hasn't loaded yet", "Refresh to retrieve deadlines from MyWarwick.")
            }
            state.entries.isEmpty() -> item {
                EmptyCard("No deadlines returned", "MyWarwick hasn't returned any coursework in this feed. Check the source service for the full record.")
            }
            else -> {
                item { SectionLabel("UPCOMING", upcoming.size.toString()) }
                if (upcoming.isEmpty()) item { Text("No future deadlines in this saved feed.") }
                items(upcoming, key = { it.id }) { entry -> CourseworkRow(entry, now) { onSelect(entry) } }
                if (past.isNotEmpty()) {
                    item { SectionLabel("PAST DEADLINES", past.size.toString()) }
                    items(past, key = { it.id }) { entry -> CourseworkRow(entry, now) { onSelect(entry) } }
                }
            }
        }
        item { CourseworkSyncNote(state) }
    }
}

@Composable
fun CourseworkSyncNote(state: CourseworkState) {
    state.lastSynced?.let { saved ->
        Text("Saved on this device · updated ${atWarwick(saved).format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK))}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkDetails(entry: CourseworkEntity, onDismiss: () -> Unit, onOpen: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    var linkFailed by remember(entry.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.testTag("coursework-details"), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text("COURSEWORK DETAILS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            item { Text("Deadline\n${deadlineTime(entry.dueMillis)} · Warwick time") }
            if (entry.description.isNotBlank()) item { Text(entry.description, style = MaterialTheme.typography.bodyLarge) }
            item { Text("Submission status isn't provided by this feed. Check the source service for instructions and your submission record.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            safeCourseworkUrl(entry.url)?.let { url -> item {
                OutlinedButton(onClick = {
                    linkFailed = runCatching {
                        if (onOpen != null) onOpen(url)
                        else CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    }.isFailure
                }, modifier = Modifier.fillMaxWidth()) { Text("Open source service") }
                Text("The browser may ask you to sign in separately.", style = MaterialTheme.typography.bodySmall)
                if (linkFailed) Text("Couldn't open a browser. Try again.")
            } }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close coursework details") } }
        }
    }
}
