package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.ui.components.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetails(event: EventContentItem, conflict: Boolean, onDismiss: () -> Unit, onOpen: ((String) -> Unit)? = null) {
    DetailsSheet(onDismiss, Modifier.testTag("class-details"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        item {
            DetailHeader("CLASS DETAILS", classIdentity(event).name, gap = 8.dp)
        }
        if (event.module.isNotBlank()) item { DetailField("Module", event.module) }
        if (event.moduleName.isNotBlank() && event.moduleName != event.title) item { DetailField("Timetable entry", event.title) }
        item {
            DetailField("When", classDetailTime(event))
        }
        item { DetailField("Location", event.location.ifBlank { "Location hasn't been provided" }) }
        if (conflict) item { Text("Another class overlaps this time in your saved timetable.", color = MaterialTheme.colorScheme.error) }
        if (event.academicWeek > 0) item { DetailField("Academic week", event.academicWeek.toString()) }
        safeExternalUrl(event.locationUrl)?.let { url -> item { ExternalLinkButton(url, "Open location", onOpen) } }
        item { DetailClose("Close details", onDismiss) }
    }
}
