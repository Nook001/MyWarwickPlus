package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import java.net.URI
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetails(kind: FeedKind, entry: FeedContentItem, onOpen: ((String) -> Unit)?, onDismiss: () -> Unit) {
    val plainText = rememberFeedText(entry)
    val messages = kind == FeedKind.MESSAGES
    DetailsSheet(onDismiss, Modifier.testTag("feed-details"),
        contentPadding = if (messages) PaddingValues(16.dp) else Spacing.detailPage,
        itemSpacing = if (messages) 12.dp else 20.dp) {
        item { DetailEyebrow(stringResource(kind.labelRes).uppercase()) }
        item { if (messages) Text(entry.title, style = InboxTypography.detailTitle) else DetailTitle(entry.title) }
        if (entry.moduleCode.isNotBlank()) item { Text("${entry.moduleCode} · ${entry.academicYear}") }
        if (entry.provider.isNotBlank()) item { Text(entry.provider,
            style = if (messages) InboxTypography.preview else LocalTextStyle.current) }
        if (entry.dateMillis != 0L) item { Text(stringResource(R.string.warwick_time, fullDateTimeLabel(entry.dateMillis)),
            style = if (messages) InboxTypography.preview else LocalTextStyle.current) }
        if (plainText.isNotBlank()) item { Text(plainText,
            style = if (messages) InboxTypography.detailBody else LocalTextStyle.current) }
        if (kind == FeedKind.MODULES) item { Text(stringResource(R.string.module_counts,
            pluralStringResource(R.plurals.announcement_count, entry.announcementCount, entry.announcementCount),
            pluralStringResource(R.plurals.evaluation_count, entry.evaluationCount, entry.evaluationCount))) }
        safeExternalUrl(entry.url)?.let { url -> item {
            ExternalLinkButton(url, if (kind == FeedKind.MODULES) stringResource(R.string.open_module_moodle) else stringResource(R.string.open_source_website), onOpen)
            Text(URI(url).host, style = MaterialTheme.typography.bodySmall)
        } }
        item { DetailClose(stringResource(R.string.close_service_details), onDismiss) }
    }
}
