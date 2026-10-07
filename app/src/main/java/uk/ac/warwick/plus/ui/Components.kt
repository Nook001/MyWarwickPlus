package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.config.AppActions

@Composable
fun classCountLabel(count: Int) = androidx.compose.ui.res.pluralStringResource(R.plurals.class_count, count, count)

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
fun DataRecoveryRow(lastSynced: Long?, issue: UiText?, updating: Boolean, needsLogin: Boolean,
    enabled: Boolean, onLogin: () -> Unit, onRefresh: () -> Unit, olderPageFailed: Boolean = false) {
    if (updating || (!needsLogin && issue == null)) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(when {
            needsLogin && lastSynced != null -> stringResource(R.string.saved_sign_in)
            needsLogin -> stringResource(R.string.sign_in_load)
            olderPageFailed -> stringResource(R.string.older_messages_failed)
            lastSynced != null -> stringResource(R.string.showing_previous_data)
            else -> stringResource(R.string.information_load_failed)
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        TextButton(onClick = if (needsLogin) onLogin else onRefresh, enabled = enabled) {
            Text(if (needsLogin) stringResource(AppActions.SIGN_IN) else stringResource(AppActions.RETRY))
        }
    }
}

@Composable
internal fun ResourceRecoveryRow(state: RecoveryState, onLogin: () -> Unit, onRefresh: () -> Unit) {
    DataRecoveryRow(state.lastSynced, state.issue, state.updating, state.needsLogin, state.enabled,
        onLogin, onRefresh)
}
