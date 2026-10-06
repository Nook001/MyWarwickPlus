package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStatusSheet(state: TimetableState, onLogin: () -> Unit, onRefresh: ((SyncResource) -> Unit)?, onDismiss: () -> Unit) {
    DetailsSheet(onDismiss, contentPadding = PaddingValues(20.dp), itemSpacing = 12.dp) {
        item { Text("Data status", style = MaterialTheme.typography.titleLarge) }
        if (state.needsLogin) item {
            DataRecoveryRow(null, null, false, true, !state.busy && !state.logoutFailed, onLogin, {})
        }
        items(SyncResource.entries, key = { it.name }) { resource ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(resource.label, style = MaterialTheme.typography.titleSmall)
                    Text(when {
                        state.updating(resource) -> "Updating…"
                        state.needsLogin && state.syncedAt(resource) != null -> "Saved data · sign in to update"
                        state.needsLogin -> "Sign in required"
                        resource.feed?.let { state.feed(it).olderPageFailed } == true && state.syncedAt(resource) != null -> "Older page failed · current messages kept"
                        state.issue(resource) != null && state.syncedAt(resource) != null -> "Update failed · previous data kept"
                        state.issue(resource) != null -> "Not loaded · update failed"
                        state.syncedAt(resource) == null -> "Not loaded"
                        else -> "Data available"
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.syncedAt(resource)?.let { time ->
                        Text("Last updated ${atWarwick(time).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.UK))} · Warwick time",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onRefresh != null) TextButton(onClick = { onRefresh(resource) },
                    enabled = !state.busy && !state.needsLogin && !state.logoutFailed) {
                    Text(if (state.issue(resource) != null) "Retry" else "Refresh")
                }
            }
        }
        item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") } }
    }
}
