package uk.ac.warwick.plus.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.reminders.ReminderScheduler
import uk.ac.warwick.plus.reminders.ReminderSettings
import uk.ac.warwick.plus.ui.components.*

val LocalReminders = staticCompositionLocalOf<ReminderScheduler?> { null }

internal fun LazyListScope.reminderSettings() {
    item(key = "reminders-label") { if (LocalReminders.current != null) SectionLabel(stringResource(R.string.reminders_section)) }
    item(key = "reminders") { ReminderSwitches() }
}

internal fun LazyListScope.updateSettings() {
    item(key = "updates-label") { if (LocalUpdates.current != null) SectionLabel(stringResource(R.string.updates_section)) }
    item(key = "updates") {
        val checker = LocalUpdates.current ?: return@item
        val state by checker.state.collectAsStateWithLifecycle()
        SettingSwitch(stringResource(R.string.auto_update_check), stringResource(R.string.auto_update_check_detail),
            state.automatic, "auto-update-check", checker::setAutomatic)
    }
}

@Composable
private fun ReminderSwitches() {
    val reminders = LocalReminders.current ?: return
    val settings by reminders.settings.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<ReminderSettings?>(null) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.takeIf { granted }?.let(reminders::update)
        denied = !granted
        pending = null
    }
    // Turning a reminder on asks for notifications first; turning one off never needs permission.
    val change: (ReminderSettings) -> Unit = { next ->
        val enabling = next.classes && !settings.classes || next.deadlines && !settings.deadlines
        if (enabling && !reminders.canNotify()) {
            pending = next
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            denied = false
            reminders.update(next)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingSwitch(stringResource(R.string.class_reminders), stringResource(R.string.class_reminders_detail),
            settings.classes, "class-reminders") { change(settings.copy(classes = it)) }
        SettingSwitch(stringResource(R.string.deadline_reminders), stringResource(R.string.deadline_reminders_detail),
            settings.deadlines, "deadline-reminders") { change(settings.copy(deadlines = it)) }
        if (denied) Text(stringResource(R.string.notifications_denied), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
    }
}
