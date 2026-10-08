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
    val scheme = MaterialTheme.colorScheme
    val urgency = if (timing.passed) null else deadlineColour(timing.days, scheme.surfaceContainerLow)
    val emphasis = urgency ?: scheme.primary
    MetricListRow(onSelect, stringResource(R.string.view_coursework_details), modifier, compactTop,
        density = if (home) ListRowDensity.Compact else ListRowDensity.Standard, metric = {
        if (!home && (timing.passed || timing.days == 0L)) Text(if (timing.passed) stringResource(R.string.deadline_passed) else stringResource(AppLabels.TODAY),
            style = MaterialTheme.typography.bodySmall,
            color = if (timing.passed) scheme.onSurfaceVariant else emphasis)
        else {
            Text(timing.days.toString(), style = if (home) HomeTypography.metric else MaterialTheme.typography.bodyMedium,
                fontWeight = if (home) FontWeight.Bold else FontWeight.SemiBold,
                color = emphasis, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.unit_days), style = if (home) HomeTypography.metricSecondary else MaterialTheme.typography.bodySmall,
                color = urgency ?: scheme.onSurfaceVariant,
                maxLines = 1, softWrap = false)
        }
    }) {
        Text(entry.title, style = if (home) HomeTypography.content else MaterialTheme.typography.bodyMedium,
            fontWeight = if (home) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = if (home) 1 else 2, overflow = TextOverflow.Ellipsis)
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
