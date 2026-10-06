package uk.ac.warwick.plus.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetails(kind: FeedKind, entry: FeedEntry, onOpen: ((String) -> Unit)?, onDismiss: () -> Unit) {
    DetailsSheet(onDismiss, Modifier.testTag("feed-details")) {
        item { DetailEyebrow(kind.label.uppercase()) }
        item { DetailTitle(entry.title) }
        if (entry.moduleCode.isNotBlank()) item { Text("${entry.moduleCode} · ${entry.academicYear}") }
        if (entry.provider.isNotBlank()) item { Text(entry.provider) }
        if (entry.dateMillis != 0L) item { Text("${fullDateTimeLabel(entry.dateMillis)} · Warwick time") }
        if (entry.text.isNotBlank()) item { Text(feedText(entry)) }
        if (kind == FeedKind.MODULES) item { Text("${entry.announcementCount} announcements · ${entry.evaluationCount} evaluations returned. View their contents on the module site.") }
        safeExternalUrl(entry.url)?.let { url -> item {
            ExternalLinkButton(url, if (kind == FeedKind.MODULES) "Open module in Moodle" else "Open source website", onOpen)
            Text(java.net.URI(url).host, style = MaterialTheme.typography.bodySmall)
        } }
        item { DetailClose("Close service details", onDismiss) }
    }
}
