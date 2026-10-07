package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import uk.ac.warwick.plus.ui.components.*

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uk.ac.warwick.plus.BuildConfig
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

@Composable
fun MoreContent(state: TimetableState, onLogin: () -> Unit, onSignOut: () -> Unit,
    onFeed: (FeedKind) -> Unit, onOpen: (String) -> Unit, onProbe: (() -> Unit)?, onSettings: () -> Unit = {},
    onResourceRefresh: ((SyncResource) -> Unit)? = null) {
    var confirmSignOut by remember { mutableStateOf(false) }
    var showDataStatus by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("more-list"),
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AccountCard(state, onLogin, { confirmSignOut = true }, onResourceRefresh)
        }
        item {
            MeGrid(stringResource(R.string.app_section), buildList {
                add(MeAction(stringResource(AppLabels.SETTINGS), MeIcons.settings, onSettings, tag = "appearance-settings"))
                add(MeAction(stringResource(AppLabels.DATA_STATUS), MeIcons.data, { showDataStatus = true }))
                add(MeAction(stringResource(AppLabels.MESSAGES), MeIcons.messages, { onFeed(FeedKind.MESSAGES) }))
                add(MeAction(stringResource(AppLabels.LIBRARY), ServiceIcons.library, { onFeed(FeedKind.LIBRARY) }))
                add(MeAction(stringResource(AppLabels.MODULES), ServiceIcons.moodle, { onFeed(FeedKind.MODULES) }))
                if (onProbe != null) add(MeAction(stringResource(AppLabels.DEVELOPER_TOOLS), MeIcons.developer, onProbe))
            })
        }
        item {
            MeGrid(stringResource(R.string.websites_section), WarwickService.entries.map { service ->
                MeAction(stringResource(service.shortLabelRes), service.icon, { onOpen(service.url) },
                    external = true, actionLabel = stringResource(R.string.open_in_browser, stringResource(service.labelRes)))
            })
        }
        item { Text(stringResource(R.string.version_detail, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (showDataStatus) DataStatusSheet(state, onLogin, onResourceRefresh, { showDataStatus = false })
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(R.string.sign_out_question, stringResource(AppActions.SIGN_OUT))) },
        text = { Text(stringResource(R.string.sign_out_confirmation)) },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text(stringResource(AppActions.SIGN_OUT)) } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(AppActions.CANCEL)) } })
}

@Composable
private fun AccountCard(state: TimetableState, onLogin: () -> Unit, onSignOut: () -> Unit,
    onResourceRefresh: ((SyncResource) -> Unit)?) {
    val context = LocalContext.current
    val clipboardLabel = stringResource(R.string.warwick_email)
    var copied by remember(state.email) { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(2_000); copied = false } }
    AppCard(Modifier.fillMaxWidth().testTag("me-account"), shape = AppShapes.tile) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (state.logoutFailed) Text(state.globalMessage?.render().orEmpty(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.name.ifBlank { stringResource(R.string.warwick_account) }, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (state.accountCode.isNotBlank()) Text(state.accountCode, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(state.email.ifBlank { if (state.updating(SyncResource.ACCOUNT)) stringResource(R.string.loading_email) else stringResource(R.string.email_not_available) },
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("me-email"))
                IconButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(clipboardLabel, state.email))
                    copied = true
                }, enabled = state.email.isNotBlank(), modifier = Modifier.testTag("copy-email")) {
                    Icon(if (copied) MeIcons.check else MeIcons.copy, contentDescription = if (copied) stringResource(R.string.email_copied) else stringResource(R.string.copy_email),
                        modifier = Modifier.size(18.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (state.needsLogin && !state.logoutFailed) Text(stringResource(R.string.sign_in_update), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (!state.logoutFailed && (state.needsLogin || !state.signedIn && !state.hasSavedData && !state.busy))
                    TextButton(onClick = onLogin, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text(stringResource(AppActions.SIGN_IN)) }
                if (state.hasSavedData || state.signedIn || state.logoutFailed)
                    TextButton(onClick = onSignOut, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(if (state.logoutFailed) stringResource(AppActions.RETRY_SIGN_OUT) else stringResource(AppActions.SIGN_OUT), style = MaterialTheme.typography.bodySmall)
                    }
            }
            if (!state.needsLogin && state.account.message != null && onResourceRefresh != null)
                ResourceRecoveryRow(state.recovery(SyncResource.ACCOUNT), onLogin) { onResourceRefresh(SyncResource.ACCOUNT) }
        }
    }
}

private data class MeAction(val label: String, val icon: ImageVector, val onClick: () -> Unit,
    val external: Boolean = false, val tag: String = "", val actionLabel: String? = null)

@Composable
private fun MeGrid(title: String, actions: List<MeAction>) {
    val fontScale = LocalDensity.current.fontScale
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (fontScale > 1.3f || maxWidth < 280.dp) 2 else 3
            val size = (maxWidth - Spacing.grid * (columns - 1)) / columns
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                actions.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                        row.forEach { action ->
                            ActionTile(action.label, action.icon, action.onClick,
                                layout = ActionTileLayout.Grid, modifier = Modifier.weight(1f).testTag(action.tag),
                                minimumHeight = size, destination = if (action.external) ActionDestination.External else ActionDestination.Internal,
                                actionLabel = action.actionLabel ?: stringResource(R.string.open_item, action.label))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
