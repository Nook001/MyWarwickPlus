package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetails(event: EventEntity, conflict: Boolean, onDismiss: () -> Unit) {
    DetailsSheet(onDismiss, Modifier.testTag("class-details"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        item {
            DetailHeader("CLASS DETAILS", event.moduleName.ifBlank { event.title }, gap = 8.dp)
        }
        if (event.module.isNotBlank()) item { DetailField("Module", event.module) }
        if (event.moduleName.isNotBlank() && event.moduleName != event.title) item { DetailField("Timetable entry", event.title) }
        item {
            DetailField("When", "${dateLabel(atWarwick(event.startMillis).toLocalDate())}\n" +
                if (event.allDay) "All day · Warwick time" else
                    "${timeLabel(event.startMillis)} – ${atWarwick(event.endMillis).let { if (it.toLocalDate() != atWarwick(event.startMillis).toLocalDate()) dateLabel(it.toLocalDate()) + " · " else "" }}${timeLabel(event.endMillis)} · Warwick time")
        }
        item { DetailField("Location", event.location.ifBlank { "Location hasn't been provided" }) }
        if (conflict) item { Text("Another class overlaps this time in your saved timetable.", color = MaterialTheme.colorScheme.error) }
        if (event.academicWeek > 0) item { DetailField("Academic week", event.academicWeek.toString()) }
        item { LocationLink(event) }
        item { DetailClose("Close details", onDismiss) }
    }
}
