package uk.ac.warwick.plus.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import uk.ac.warwick.plus.data.CourseworkEntity
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal enum class DeadlinePresentation { Home, List }

@Composable
internal fun DeadlineRow(entry: CourseworkEntity, now: Long, presentation: DeadlinePresentation,
    compactTop: Boolean = false, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    val due = atWarwick(entry.dueMillis)
    val today = atWarwick(now).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due.toLocalDate())
    val home = presentation == DeadlinePresentation.Home
    val passed = entry.dueMillis < now
    val pattern = if (home) {
        if (due.year > today.year) "d MMM yyyy" else "d MMM"
    } else if (due.year != today.year) "d MMM yyyy · HH:mm" else "d MMM · HH:mm"
    val date = due.format(DateTimeFormatter.ofPattern(pattern, Locale.UK))
    MetricListRow(onSelect, "View coursework details", modifier, compactTop, metric = {
        if (!home && (passed || days == 0L)) Text(if (passed) "Passed" else "Today",
            style = MaterialTheme.typography.bodySmall,
            color = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        else {
            Text(days.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
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
