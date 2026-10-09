package uk.ac.warwick.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.ui.components.*
import uk.ac.warwick.plus.update.UpdateChecker
import uk.ac.warwick.plus.update.UpdateResult

val LocalUpdates = staticCompositionLocalOf<UpdateChecker?> { null }

/** Me row: opens the release page when a newer version is known, otherwise checks now. */
@Composable
internal fun UpdateRow(onOpen: (String) -> Unit) {
    val checker = LocalUpdates.current ?: return
    val state by checker.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val available = state.available
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (available != null) stringResource(R.string.update_available, available.version)
        else stringResource(R.string.check_for_updates)
    val status = when {
        state.checking -> stringResource(R.string.checking_updates)
        available != null -> null
        state.result == UpdateResult.UP_TO_DATE -> stringResource(R.string.up_to_date)
        state.result == UpdateResult.FAILED -> stringResource(R.string.update_check_failed)
        else -> null
    }
    Row(Modifier.fillMaxWidth().clickable(enabled = !state.checking, role = Role.Button, onClickLabel = label) {
        if (available != null) onOpen(available.url) else scope.launch { checker.check() }
    }.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp).testTag("check-updates"),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(MeIcons.update, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = if (available != null) MaterialTheme.colorScheme.primary else muted)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            fontWeight = if (available != null) FontWeight.SemiBold else null)
        if (status != null) Text(status, style = MaterialTheme.typography.bodySmall,
            color = if (state.result == UpdateResult.FAILED && !state.checking) MaterialTheme.colorScheme.error else muted)
        if (available != null) Icon(MeIcons.external, contentDescription = null, modifier = Modifier.size(16.dp), tint = muted)
    }
}

/** One snackbar per newly discovered version; the Me row keeps showing it afterwards. */
@Composable
internal fun UpdateNotice(snackbar: SnackbarHostState, onOpen: (String) -> Unit) {
    val checker = LocalUpdates.current ?: return
    val state by checker.state.collectAsStateWithLifecycle()
    val available = state.available ?: return
    if (available.version == state.notifiedVersion) return
    val message = stringResource(R.string.update_available_notice, available.version)
    val action = stringResource(R.string.view_update)
    val open by rememberUpdatedState(onOpen)
    LaunchedEffect(available.version) {
        checker.markNotified(available.version)
        if (snackbar.showSnackbar(message, actionLabel = action, withDismissAction = true,
                duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) open(available.url)
    }
}

@Composable
internal fun SettingSwitch(title: String, detail: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    AppCard(shape = AppShapes.featured) {
        Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange).testTag(tag).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked, onCheckedChange = null)
        }
    }
}