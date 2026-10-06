package uk.ac.warwick.plus.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.safeCourseworkUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkDetails(entry: CourseworkEntity, onDismiss: () -> Unit, onOpen: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    var linkFailed by remember(entry.id) { mutableStateOf(false) }
    DetailsSheet(onDismiss, Modifier.testTag("coursework-details")) {
        item {
            DetailHeader("COURSEWORK DETAILS", entry.title)
        }
        item { Text("Deadline\n${deadlineTime(entry.dueMillis)} · Warwick time") }
        if (entry.description.isNotBlank()) item { Text(entry.description, style = MaterialTheme.typography.bodyLarge) }
        item { Text("This feed covers a limited period and doesn't provide submission status. Check the source service for instructions and your submission record.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        safeCourseworkUrl(entry.url)?.let { url -> item {
            OutlinedButton(onClick = {
                linkFailed = runCatching {
                    if (onOpen != null) onOpen(url)
                    else CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                }.isFailure
            }, modifier = Modifier.fillMaxWidth()) { Text("Open source service") }
            Text("The browser may ask you to sign in separately.", style = MaterialTheme.typography.bodySmall)
            if (linkFailed) Text("Couldn't open a browser. Try again.")
        } }
        item { DetailClose("Close coursework details", onDismiss) }
    }
}
