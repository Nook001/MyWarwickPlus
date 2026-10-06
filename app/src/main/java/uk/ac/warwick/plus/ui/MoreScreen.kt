package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.content.ClipData
import android.content.ClipboardManager
import kotlinx.coroutines.delay
import uk.ac.warwick.plus.BuildConfig
import uk.ac.warwick.plus.data.FeedKind

data class ServiceLink(val label: String, val url: String, val homeIcon: ImageVector? = null, val homeLabel: String = label)
val Services = listOf(
    ServiceLink("Moodle", "https://moodle.warwick.ac.uk/", ServiceIcons.moodle),
    ServiceLink("Email", "https://warwick.ac.uk/mymail", ServiceIcons.email),
    ServiceLink("Tabula", "https://tabula.warwick.ac.uk/", ServiceIcons.tabula),
    ServiceLink("Library account", "https://warwick.ac.uk/services/library/account", ServiceIcons.library, "Library"),
    ServiceLink("Academic support", "https://warwick.ac.uk/academic-support/"),
    ServiceLink("Wellbeing and Student Support", "https://warwick.ac.uk/wellbeing/students/"),
    ServiceLink("Safety and emergency support", "https://warwick.ac.uk/students/safety-and-support/emergency-support/"),
    ServiceLink("MyWarwick help", "https://warwick.ac.uk/mw-support")
)
internal val HomeServices = Services.filter { it.homeIcon != null }

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
            MeGrid("App", buildList {
                add(MeAction("Settings", MeIcons.settings, onSettings, tag = "appearance-settings"))
                add(MeAction("Data status", MeIcons.data, { showDataStatus = true }))
                add(MeAction("Messages", MeIcons.messages, { onFeed(FeedKind.MESSAGES) }))
                add(MeAction("Library", ServiceIcons.library, { onFeed(FeedKind.LIBRARY) }))
                add(MeAction("Modules", ServiceIcons.moodle, { onFeed(FeedKind.MODULES) }))
                if (onProbe != null) add(MeAction("Developer tools", MeIcons.developer, onProbe))
            })
        }
        item {
            MeGrid("Websites", Services.map { service ->
                val label = when (service.label) {
                    "Wellbeing and Student Support" -> "Wellbeing"
                    "Safety and emergency support" -> "Safety"
                    "MyWarwick help" -> "Help"
                    else -> service.label
                }
                val icon = service.homeIcon ?: when (service.label) {
                    "Academic support" -> ServiceIcons.moodle
                    "Wellbeing and Student Support" -> MeIcons.wellbeing
                    "Safety and emergency support" -> MeIcons.safety
                    else -> MeIcons.help
                }
                MeAction(label, icon, { onOpen(service.url) }, external = true, actionLabel = "Open ${service.label} in browser")
            })
        }
        item { Text("MyWarwick+ ${BuildConfig.VERSION_NAME} · Independent student app", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (showDataStatus) DataStatusSheet(state, onLogin, onResourceRefresh, { showDataStatus = false })
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out?") },
        text = { Text("This removes this app's sign-in session and saved data. Your system browser sessions are kept.") },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } })
}

@Composable
private fun AccountCard(state: TimetableState, onLogin: () -> Unit, onSignOut: () -> Unit,
    onResourceRefresh: ((SyncResource) -> Unit)?) {
    val context = LocalContext.current
    var copied by remember(state.email) { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(2_000); copied = false } }
    AppCard(Modifier.fillMaxWidth().testTag("me-account"), shape = AppShapes.tile) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.name.ifBlank { "Your Warwick account" }, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (state.accountCode.isNotBlank()) Text(state.accountCode, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(state.email.ifBlank { if (state.updating(SyncResource.ACCOUNT)) "Loading email…" else "Email not available" },
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("me-email"))
                IconButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Warwick email", state.email))
                    copied = true
                }, enabled = state.email.isNotBlank(), modifier = Modifier.testTag("copy-email")) {
                    Icon(if (copied) MeIcons.check else MeIcons.copy, contentDescription = if (copied) "Email copied" else "Copy email",
                        modifier = Modifier.size(18.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (state.needsLogin && !state.logoutFailed) Text("Sign in to update", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (!state.logoutFailed && (state.needsLogin || !state.signedIn && !state.hasSavedData && !state.busy))
                    TextButton(onClick = onLogin, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Sign in") }
                if (state.hasSavedData || state.signedIn || state.logoutFailed)
                    TextButton(onClick = onSignOut, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(if (state.logoutFailed) "Retry sign-out" else "Sign out", style = MaterialTheme.typography.bodySmall)
                    }
            }
            if (!state.needsLogin && state.account.message != null && onResourceRefresh != null)
                ResourceRecoveryRow(state, SyncResource.ACCOUNT, onLogin) { onResourceRefresh(SyncResource.ACCOUNT) }
        }
    }
}

private data class MeAction(val label: String, val icon: ImageVector, val onClick: () -> Unit,
    val external: Boolean = false, val tag: String = "", val actionLabel: String = "Open $label")

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
                                actionLabel = action.actionLabel)
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
