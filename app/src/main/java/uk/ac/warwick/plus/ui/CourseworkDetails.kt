package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import uk.ac.warwick.plus.data.CourseworkContentItem
import uk.ac.warwick.plus.data.safeCourseworkUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkDetails(entry: CourseworkContentItem, onDismiss: () -> Unit, onOpen: ((String) -> Unit)? = null) {
    DetailsSheet(onDismiss, Modifier.testTag("coursework-details")) {
        item {
            DetailHeader(stringResource(R.string.coursework_details), entry.title)
        }
        item { Text(stringResource(R.string.deadline_time, fullDateTimeLabel(entry.dueMillis))) }
        if (entry.description.isNotBlank()) item { Text(entry.description, style = MaterialTheme.typography.bodyLarge) }
        item { Text(stringResource(R.string.coursework_source_detail),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        safeCourseworkUrl(entry.url)?.let { url -> item {
            ExternalLinkButton(url, stringResource(R.string.open_source_service), onOpen)
            Text(stringResource(R.string.browser_sign_in_detail), style = MaterialTheme.typography.bodySmall)
        } }
        item { DetailClose(stringResource(R.string.close_coursework_details), onDismiss) }
    }
}
