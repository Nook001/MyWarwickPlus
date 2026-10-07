package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import java.net.URI

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetails(kind: FeedKind, entry: FeedContentItem, onOpen: ((String) -> Unit)?, onDismiss: () -> Unit) {
    val plainText = rememberFeedText(entry)
    DetailsSheet(onDismiss, Modifier.testTag("feed-details")) {
        item { DetailEyebrow(stringResource(kind.labelRes).uppercase()) }
        item { DetailTitle(entry.title) }
        if (entry.moduleCode.isNotBlank()) item { Text("${entry.moduleCode} · ${entry.academicYear}") }
        if (entry.provider.isNotBlank()) item { Text(entry.provider) }
        if (entry.dateMillis != 0L) item { Text(stringResource(R.string.warwick_time, fullDateTimeLabel(entry.dateMillis))) }
        if (plainText.isNotBlank()) item { Text(plainText) }
        if (kind == FeedKind.MODULES) item { Text(stringResource(R.string.module_counts, entry.announcementCount, entry.evaluationCount)) }
        safeExternalUrl(entry.url)?.let { url -> item {
            ExternalLinkButton(url, if (kind == FeedKind.MODULES) stringResource(R.string.open_module_moodle) else stringResource(R.string.open_source_website), onOpen)
            Text(URI(url).host, style = MaterialTheme.typography.bodySmall)
        } }
        item { DetailClose(stringResource(R.string.close_service_details), onDismiss) }
    }
}
