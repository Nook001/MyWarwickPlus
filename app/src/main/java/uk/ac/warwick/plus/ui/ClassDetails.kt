package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

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
            DetailHeader(stringResource(R.string.class_details), classIdentity(event).name, gap = 8.dp)
        }
        if (event.module.isNotBlank()) item { DetailField(stringResource(R.string.module), event.module) }
        if (event.moduleName.isNotBlank() && event.moduleName != event.title) item { DetailField(stringResource(R.string.timetable_entry), event.title) }
        item {
            DetailField(stringResource(R.string.when_label), classDetailTime(event))
        }
        item { DetailField(stringResource(R.string.location), event.location.ifBlank { stringResource(R.string.location_not_provided) }) }
        if (conflict) item { Text(stringResource(R.string.class_overlap_detail), color = MaterialTheme.colorScheme.error) }
        if (event.academicWeek > 0) item { DetailField(stringResource(R.string.academic_week), event.academicWeek.toString()) }
        safeExternalUrl(event.locationUrl)?.let { url -> item { ExternalLinkButton(url, stringResource(R.string.open_location), onOpen) } }
        item { DetailClose(stringResource(R.string.close_details), onDismiss) }
    }
}
