package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
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
    modifier: Modifier = Modifier, compactTop: Boolean = false, onSelect: () -> Unit) {
    val timing = deadlineTiming(entry.dueMillis, now)
    val home = presentation == DeadlinePresentation.Home
    val date = deadlineDateLabel(entry.dueMillis, now, includeTime = !home)
    MetricListRow(onSelect, stringResource(R.string.view_coursework_details), modifier, compactTop, metric = {
        if (!home && (timing.passed || timing.days == 0L)) Text(if (timing.passed) stringResource(R.string.deadline_passed) else stringResource(AppLabels.TODAY),
            style = MaterialTheme.typography.bodySmall,
            color = if (timing.passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        else {
            Text(timing.days.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.unit_days), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, softWrap = false)
        }
    }) {
        Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            maxLines = if (home) 1 else 2, overflow = TextOverflow.Ellipsis)
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
