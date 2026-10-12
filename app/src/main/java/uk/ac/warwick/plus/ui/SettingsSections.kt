package uk.ac.warwick.plus.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.reminders.ReminderScheduler
import uk.ac.warwick.plus.reminders.ReminderSettings
import uk.ac.warwick.plus.ui.components.*
import uk.ac.warwick.plus.widget.HomeWidget

val LocalReminders = staticCompositionLocalOf<ReminderScheduler?> { null }

internal fun LazyListScope.reminderSettings() {
    item(key = "reminders") { ReminderSwitches() }
}

internal fun LazyListScope.widgetSettings() {
    item(key = "widgets") { WidgetEntries() }
}

internal fun LazyListScope.updateSettings() {
    item(key = "updates") {
        val checker = LocalUpdates.current ?: return@item
        val state by checker.state.collectAsStateWithLifecycle()
        SectionCard(stringResource(R.string.updates_section)) {
            SettingSwitch(stringResource(R.string.auto_update_check), stringResource(R.string.auto_update_check_detail),
                state.automatic, "auto-update-check", checker::setAutomatic)
        }
    }
}

@Composable
private fun WidgetEntries() {
    val context = LocalContext.current
    val canPin = remember(context) { HomeWidget.canPin(context) }
    SectionCard(stringResource(R.string.widgets_section)) {
        HomeWidget.entries.forEachIndexed { index, widget ->
            if (index > 0) ListDivider()
            val name = stringResource(widget.nameRes)
            val action = if (!canPin) Modifier else Modifier.clickable(role = Role.Button,
                onClickLabel = stringResource(R.string.widget_add_label, name)) { widget.pin(context) }
            SettingRow(name, stringResource(widget.detailRes), action.testTag("widget-${widget.key}")) {
                if (canPin) Text(stringResource(R.string.widget_add), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
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
    SectionCard(stringResource(R.string.reminders_section)) {
        SettingSwitch(stringResource(R.string.class_reminders), stringResource(R.string.class_reminders_detail),
            settings.classes, "class-reminders") { change(settings.copy(classes = it)) }
        ListDivider()
        SettingSwitch(stringResource(R.string.deadline_reminders), stringResource(R.string.deadline_reminders_detail),
            settings.deadlines, "deadline-reminders") { change(settings.copy(deadlines = it)) }
        if (denied) Text(stringResource(R.string.notifications_denied), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = Spacing.contentInset,
                end = Spacing.contentInset, bottom = 10.dp))
    }
}
