package uk.ac.warwick.plus.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.CourseworkContentItem

internal enum class DeadlinePresentation { Home, List }

@Composable
internal fun DeadlineRow(entry: CourseworkContentItem, now: Long, presentation: DeadlinePresentation,
    compactTop: Boolean = false, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    val timing = deadlineTiming(entry.dueMillis, now)
    val home = presentation == DeadlinePresentation.Home
    val date = deadlineDateLabel(entry.dueMillis, now, includeTime = !home)
    MetricListRow(onSelect, "View coursework details", modifier, compactTop, metric = {
        if (!home && (timing.passed || timing.days == 0L)) Text(if (timing.passed) "Passed" else AppLabels.TODAY,
            style = MaterialTheme.typography.bodySmall,
            color = if (timing.passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        else {
            Text(timing.days.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Text("days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false)
        }
    }) {
        Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            maxLines = if (home) 1 else 2, overflow = TextOverflow.Ellipsis)
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
