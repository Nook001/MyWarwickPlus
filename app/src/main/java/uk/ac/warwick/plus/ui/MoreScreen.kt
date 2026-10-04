package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.BuildConfig
import uk.ac.warwick.plus.data.FeedKind

data class ServiceLink(val label: String, val url: String)
val Services = listOf(
    ServiceLink("Moodle", "https://moodle.warwick.ac.uk/"),
    ServiceLink("Email", "https://warwick.ac.uk/mymail"),
    ServiceLink("Tabula", "https://tabula.warwick.ac.uk/"),
    ServiceLink("Library account", "https://warwick.ac.uk/services/library/account"),
    ServiceLink("Academic support", "https://warwick.ac.uk/academic-support/"),
    ServiceLink("Wellbeing and Student Support", "https://warwick.ac.uk/wellbeing/students/"),
    ServiceLink("Safety and emergency support", "https://warwick.ac.uk/students/safety-and-support/emergency-support/"),
    ServiceLink("MyWarwick help", "https://warwick.ac.uk/mw-support")
)

@Composable
fun MoreContent(state: TimetableState, onLogin: () -> Unit, onSignOut: () -> Unit,
    onFeed: (FeedKind) -> Unit, onOpen: (String) -> Unit, onProbe: (() -> Unit)?, onSettings: () -> Unit = {}) {
    var confirmSignOut by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("more-list"), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
        item {
            SectionLabel("ACCOUNT")
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.name.ifBlank { "Your Warwick account" }, style = MaterialTheme.typography.titleLarge)
                    if (state.accountCode.isNotBlank()) Text(state.accountCode, style = MaterialTheme.typography.bodyMedium)
                    Text(when {
                        state.logoutFailed -> "Sign-out needs to be retried"
                        state.needsLogin -> "Sign in to update saved data"
                        state.signedIn -> "Signed in"
                        state.hasSavedData -> "Saved account · session not yet verified"
                        else -> "Not signed in"
                    }, style = MaterialTheme.typography.bodySmall)
                    if (!state.logoutFailed && (state.needsLogin || !state.signedIn)) Button(onClick = onLogin) { Text("Sign in with Warwick") }
                    if (state.hasSavedData || state.signedIn || state.logoutFailed) OutlinedButton(onClick = { confirmSignOut = true }) {
                        Text(if (state.logoutFailed) "Retry sign-out" else "Sign out of this app")
                    }
                }
            }
        }
        item {
            TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("appearance-settings")) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            SectionLabel("YOUR SERVICES")
            FeedKind.entries.forEach { kind ->
                TextButton(onClick = { onFeed(kind) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(kind.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            SectionLabel("QUICK LINKS")
            Text("These services open in your browser and may require a separate sign-in.", style = MaterialTheme.typography.bodySmall)
            Services.forEach { service ->
                TextButton(onClick = { onOpen(service.url) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(service.label) }
            }
        }
        if (onProbe != null) item { TextButton(onClick = onProbe) { Text("Developer tools") } }
        item { Text("MyWarwick+ ${BuildConfig.VERSION_NAME}\nAn independent student app", style = MaterialTheme.typography.bodySmall) }
    }
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out of this app?") },
        text = { Text("This removes this app's sign-in session and saved data. Your system browser sessions are kept.") },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } })
}
