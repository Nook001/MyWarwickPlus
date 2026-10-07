package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.config.AppActions

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
            Text(if (needsLogin) AppActions.SIGN_IN else AppActions.RETRY)
        }
    }
}

@Composable
internal fun ResourceRecoveryRow(state: RecoveryState, onLogin: () -> Unit, onRefresh: () -> Unit) {
    DataRecoveryRow(state.lastSynced, state.issue, state.updating, state.needsLogin, state.enabled,
        onLogin, onRefresh)
}
