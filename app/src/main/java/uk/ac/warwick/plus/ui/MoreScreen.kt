package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
    onFeed: (FeedKind) -> Unit, onOpen: (String) -> Unit, onSettings: () -> Unit = {},
    onResourceRefresh: ((SyncResource) -> Unit)? = null) {
    var confirmSignOut by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LazyColumn(Modifier.fillMaxSize().fadingTopEdge(listState).testTag("more-list"), state = listState,
        contentPadding = Spacing.compactPage,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AccountCard(state, onLogin, onResourceRefresh)
        }
        item {
            MeSection(stringResource(R.string.app_section)) {
                MeListRow(stringResource(AppLabels.SETTINGS), MeIcons.settings, onSettings, Modifier.testTag("appearance-settings"))
                ListDivider()
                MeListRow(stringResource(AppLabels.LIBRARY), ServiceIcons.library, { onFeed(FeedKind.LIBRARY) })
                ListDivider()
                MeListRow(stringResource(AppLabels.MODULES), ServiceIcons.moodle, { onFeed(FeedKind.MODULES) })
            }
        }
        item {
            MeGrid(stringResource(R.string.websites_section), WarwickService.entries.map { service ->
                MeAction(stringResource(service.shortLabelRes), service.icon, { onOpen(service.url) },
                    external = true, actionLabel = stringResource(R.string.open_in_browser, stringResource(service.labelRes)))
            })
        }
        item(key = "about") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppCard(Modifier.fillMaxWidth(), shape = AppShapes.tile) {
                    Column {
                        MeListRow(stringResource(R.string.privacy_information), null,
                            { onOpen("https://github.com/Nook001/MyWarwickPlus/blob/master/PRIVACY.md") }, external = true)
                        ListDivider()
                        MeListRow(stringResource(R.string.open_source_licenses), null,
                            { onOpen("https://github.com/Nook001/MyWarwickPlus/blob/master/THIRD_PARTY_NOTICES.md") }, external = true)
                        if (state.hasSavedData || state.signedIn || state.logoutFailed) {
                            ListDivider()
                            MeListRow(if (state.logoutFailed) stringResource(AppActions.RETRY_SIGN_OUT) else stringResource(AppActions.SIGN_OUT),
                                null, { confirmSignOut = true }, Modifier.testTag("sign-out"), colour = MaterialTheme.colorScheme.error,
                                showArrow = false, enabled = !state.signingOut)
                        }
                    }
                }
                Text(stringResource(R.string.version_detail, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(R.string.sign_out_question, stringResource(AppActions.SIGN_OUT))) },
        text = { Text(stringResource(R.string.sign_out_confirmation)) },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text(stringResource(AppActions.SIGN_OUT)) } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(AppActions.CANCEL)) } })
}

@Composable
private fun AccountCard(state: TimetableState, onLogin: () -> Unit,
    onResourceRefresh: ((SyncResource) -> Unit)?) {
    val context = LocalContext.current
    val clipboardLabel = stringResource(R.string.warwick_email)
    var copied by remember(state.email) { mutableStateOf(false) }
    val showSignIn = !state.logoutFailed && (state.needsLogin || !state.signedIn && !state.hasSavedData && !state.busy)
    LaunchedEffect(copied) { if (copied) { delay(2_000); copied = false } }
    AppCard(Modifier.fillMaxWidth().testTag("me-account"), shape = AppShapes.tile) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (state.logoutFailed) Text(state.globalMessage?.render().orEmpty(), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val initials = initials(state.name)
                val avatar = appCardColours(CardTone.Featured)
                if (initials.isNotEmpty()) Box(Modifier.size(44.dp).background(avatar.background, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = avatar.foreground)
                }
                Column(Modifier.weight(1f)) {
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
                }
            }
            if (showSignIn) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (state.needsLogin) Text(stringResource(R.string.sign_in_update), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = onLogin, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text(stringResource(AppActions.SIGN_IN)) }
                }
            }
            if (!state.needsLogin && state.account.message != null && onResourceRefresh != null)
                ResourceRecoveryRow(state.recovery(SyncResource.ACCOUNT), onLogin) { onResourceRefresh(SyncResource.ACCOUNT) }
        }
    }
}

@Composable
private fun MeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppCard(Modifier.fillMaxWidth(), shape = AppShapes.tile) { Column(content = content) }
    }
}

/** In-app pages end in a chevron and websites in the external mark, matching the tile destinations. */
@Composable
private fun MeListRow(label: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier,
    external: Boolean = false, colour: Color = MaterialTheme.colorScheme.onSurface, showArrow: Boolean = true,
    enabled: Boolean = true) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button,
        onClickLabel = if (external) stringResource(R.string.open_in_browser, label) else stringResource(R.string.open_item, label),
        onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = muted)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) colour else muted,
            modifier = Modifier.weight(1f))
        if (showArrow) Icon(if (external) MeIcons.external else DetailsChevron, contentDescription = null,
            modifier = Modifier.size(16.dp), tint = muted)
    }
}

private val initialsBoundary = Regex("\\s+")
internal fun initials(name: String): String {
    val words = name.trim().split(initialsBoundary).filter { it.isNotEmpty() }
    return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull())
        .joinToString("") { it.first().uppercase() }
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
            val preferredColumns = if (fontScale > 1.3f || maxWidth < 280.dp) 2 else 4
            val columns = minOf(preferredColumns, actions.size.coerceAtLeast(1))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                actions.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                        row.forEach { action ->
                            ActionTile(action.label, action.icon, action.onClick,
                                layout = ActionTileLayout.Grid, modifier = Modifier.weight(1f).testTag(action.tag),
                                destination = if (action.external) ActionDestination.External else ActionDestination.Internal,
                                actionLabel = action.actionLabel ?: stringResource(R.string.open_item, action.label))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
