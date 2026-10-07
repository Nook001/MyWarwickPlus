package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStatusSheet(state: TimetableState, onLogin: () -> Unit, onRefresh: ((SyncResource) -> Unit)?, onDismiss: () -> Unit) {
    DetailsSheet(onDismiss, contentPadding = PaddingValues(20.dp), itemSpacing = 12.dp) {
        item { Text(stringResource(AppLabels.DATA_STATUS), style = MaterialTheme.typography.titleLarge) }
        state.globalMessage?.let { message -> item { Text(message.render(), style = MaterialTheme.typography.bodySmall) } }
        if (state.needsLogin) item {
            DataRecoveryRow(null, null, false, true, !state.busy && !state.logoutFailed, onLogin, {})
        }
        items(SyncResource.entries, key = { it.name }) { resource ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(resource.labelRes), style = MaterialTheme.typography.titleSmall)
                    Text(when {
                        state.updating(resource) -> stringResource(R.string.updating)
                        state.needsLogin && state.syncedAt(resource) != null -> stringResource(R.string.saved_sign_in)
                        state.needsLogin -> stringResource(R.string.sign_in_required)
                        resource.feed?.let { state.feed(it).olderPageFailed } == true && state.syncedAt(resource) != null -> stringResource(R.string.older_page_kept)
                        state.issue(resource) != null && state.syncedAt(resource) != null -> stringResource(R.string.update_failed_kept)
                        state.issue(resource) != null -> stringResource(R.string.not_loaded_failed)
                        state.syncedAt(resource) == null -> stringResource(R.string.not_loaded)
                        else -> stringResource(R.string.data_available)
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.syncedAt(resource)?.let { time ->
                        Text(stringResource(R.string.last_updated, updatedTimeLabel(time)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onRefresh != null) TextButton(onClick = { onRefresh(resource) },
                    enabled = !state.busy && !state.needsLogin && !state.logoutFailed) {
                    Text(if (state.issue(resource) != null) stringResource(AppActions.RETRY) else stringResource(R.string.action_refresh))
                }
            }
        }
        item { DetailClose(stringResource(AppActions.CLOSE), onDismiss) }
    }
}
