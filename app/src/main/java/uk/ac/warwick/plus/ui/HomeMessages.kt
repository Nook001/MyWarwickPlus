package uk.ac.warwick.plus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedContentItem
import uk.ac.warwick.plus.ui.components.*

@Composable
internal fun HomeMessages(state: FeedState, recovery: RecoveryState, today: LocalDate,
    onSelect: (FeedContentItem) -> Unit, onAll: () -> Unit, onLogin: () -> Unit, onRefresh: () -> Unit) {
    val recent = remember(state.entries) {
        state.entries.sortedWith(compareByDescending<FeedContentItem> { it.dateMillis }
            .thenBy { it.position }.thenBy { it.id }).take(2)
    }
    // Only convert the visible preview; the full feed shares the account-scoped text cache.
    val textById = rememberFeedText(recent)
    SectionCard(stringResource(AppLabels.MESSAGES), modifier = Modifier.testTag("home-messages-section"),
        icon = MeIcons.messages, tone = CardTone.Quiet, trailing = {
            SectionAllAction(stringResource(R.string.view_all_messages), onAll)
        }) {
        recent.forEachIndexed { index, entry ->
            if (index > 0) ListDivider()
            Row(Modifier.fillMaxWidth().testTag("home-message-${entry.id}")
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.view_resource_details,
                    stringResource(AppLabels.MESSAGES)), onClick = { onSelect(entry) })
                .heightIn(min = if (index == 0) Spacing.sectionFirstRowHeight else 56.dp)
                .padding(start = 12.dp, end = 12.dp, top = if (index == 0) 0.dp else 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val preview = textById[entry.id] ?: if (entry.html) "" else entry.text
                    if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.provider, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (entry.dateMillis != 0L) {
                            val date = atWarwick(entry.dateMillis).toLocalDate()
                            Text(if (date == today) timeLabel(entry.dateMillis) else shortDateLabel(date, date.year != today.year),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                DetailsArrow()
            }
        }
        Box(Modifier.padding(horizontal = 12.dp)) { ResourceRecoveryRow(recovery, onLogin, onRefresh) }
    }
}
