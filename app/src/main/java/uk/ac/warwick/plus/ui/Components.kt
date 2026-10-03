package uk.ac.warwick.plus.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.EventEntity
import java.time.format.DateTimeFormatter
import java.util.Locale

object Spacing {
    val page = 20.dp
    val section = 20.dp
    val item = 12.dp
}

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
fun EmptyCard(title: String, detail: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EventRow(event: EventEntity, conflict: Boolean = false, onSelect: () -> Unit) {
    Card(onClick = onSelect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.width(52.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (event.allDay) "All day" else timeLabel(event.startMillis), fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                if (!event.allDay) Text(timeLabel(event.endMillis), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (event.module.isNotBlank()) Text(event.module, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(event.moduleName.ifBlank { event.title }, style = MaterialTheme.typography.titleSmall)
                if (event.moduleName.isNotBlank() && event.moduleName != event.title) Text(event.title,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conflict) Text("Overlaps another class", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
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

@Composable
fun SyncNote(state: TimetableState, now: Long) {
    val saved = state.lastSynced
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (saved == null) "Waiting for your first timetable update"
            else if (now - saved > 24 * 60 * 60_000L) "Saved timetable · check for updates"
            else "Saved on this device · available offline",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (saved != null) Text("Last updated ${atWarwick(saved).format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK))}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
