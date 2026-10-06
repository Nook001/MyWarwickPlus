package uk.ac.warwick.plus.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import uk.ac.warwick.plus.data.EventEntity

fun classCountLabel(count: Int) = if (count == 1) "1 class" else "$count classes"

@Composable
fun SectionLabel(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DataEmptyState(text: String, detail: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    AppCard(Modifier.fillMaxWidth(), tone = CardTone.Quiet) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (!detail.isNullOrBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) { Text(action) }
        }
    }
}

@Composable
fun DataRecoveryRow(lastSynced: Long?, issue: String?, updating: Boolean, needsLogin: Boolean,
    enabled: Boolean, onLogin: () -> Unit, onRefresh: () -> Unit, olderPageFailed: Boolean = false) {
    if (updating || (!needsLogin && issue == null)) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(when {
            needsLogin && lastSynced != null -> "Saved data · sign in to update"
            needsLogin -> "Sign in to load this information"
            olderPageFailed -> "Older messages couldn't be loaded"
            lastSynced != null -> "Showing previous data"
            else -> "Couldn't load this information"
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        TextButton(onClick = if (needsLogin) onLogin else onRefresh, enabled = enabled) {
            Text(if (needsLogin) "Sign in" else "Retry")
        }
    }
}

@Composable
fun ResourceRecoveryRow(state: TimetableState, resource: SyncResource, onLogin: () -> Unit, onRefresh: () -> Unit) {
    DataRecoveryRow(state.syncedAt(resource), state.issue(resource), state.updating(resource), state.needsLogin,
        !state.busy && !state.signingOut && !state.logoutFailed, onLogin, onRefresh,
        resource.feed?.let { state.feed(it).olderPageFailed } == true)
}

@Composable
fun LocationLink(event: EventEntity) {
    val context = LocalContext.current
    val uri = Uri.parse(event.locationUrl)
    if (uri.scheme == "https" && uri.host != null) {
        OutlinedButton(onClick = { runCatching { CustomTabsIntent.Builder().build().launchUrl(context, uri) } },
            modifier = Modifier.fillMaxWidth()) { Text("Open location") }
    }
}
