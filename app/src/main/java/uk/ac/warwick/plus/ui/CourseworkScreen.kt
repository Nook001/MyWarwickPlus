package uk.ac.warwick.plus.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.safeCourseworkUrl
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun deadlineLabel(due: Long, now: Long): String {
    if (due < now) return "Deadline passed"
    val days = ChronoUnit.DAYS.between(atWarwick(now).toLocalDate(), atWarwick(due).toLocalDate())
    return when (days) {
        0L -> "Due today"; 1L -> "Due tomorrow"; else -> "Due in $days days"
    }
}

fun deadlineTime(due: Long): String = atWarwick(due).format(DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.UK))

private val CourseworkSearchIcon = ImageVector.Builder("CourseworkSearch", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(16f, 10f); curveTo(16f, 13.3f, 13.3f, 16f, 10f, 16f)
        curveTo(6.7f, 16f, 4f, 13.3f, 4f, 10f); curveTo(4f, 6.7f, 6.7f, 4f, 10f, 4f)
        curveTo(13.3f, 4f, 16f, 6.7f, 16f, 10f); close()
        moveTo(14.5f, 14.5f); lineTo(21f, 21f)
    }
}.build()

private val CourseworkClearIcon = ImageVector.Builder("CourseworkClear", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
    }
}.build()

@Composable
private fun CourseworkSearch(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()) {
        BasicTextField(query, onQueryChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.fillMaxWidth(), decorationBox = { input ->
                Row(Modifier.heightIn(min = 48.dp).padding(start = 14.dp, end = if (query.isEmpty()) 14.dp else 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(CourseworkSearchIcon, "Search coursework", Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        if (query.isEmpty()) Text("Search coursework", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        input()
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                        Icon(CourseworkClearIcon, "Clear search", Modifier.size(18.dp))
                    }
                }
            })
    }
}

@Composable
fun CourseworkRow(entry: CourseworkEntity, now: Long, onSelect: () -> Unit) {
    val due = atWarwick(entry.dueMillis)
    val today = atWarwick(now).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due.toLocalDate())
    val passed = entry.dueMillis < now
    val date = due.format(DateTimeFormatter.ofPattern(if (due.year != today.year) "d MMM yyyy · HH:mm" else "d MMM · HH:mm", Locale.UK))
    val countdownWidth = with(LocalDensity.current) { 48.sp.toDp() }
    Surface(onClick = onSelect, color = Color.Transparent, modifier = Modifier.fillMaxWidth()
        .semantics { onClick(label = "View coursework details", action = null) }) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(countdownWidth), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (passed || days == 0L) Text(if (passed) "Passed" else "Today", style = MaterialTheme.typography.bodySmall,
                    color = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                else {
                    Text(days.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    Text("days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(DetailsChevron, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkContent(state: CourseworkState, now: Long, busy: Boolean, onRefresh: () -> Unit,
    feedback: @Composable () -> Unit = {}, showFeedback: Boolean = false,
    filterOverride: String? = null, onFilterChanged: ((String) -> Unit)? = null,
    onSelect: (CourseworkEntity) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var localFilter by rememberSaveable { mutableStateOf("Upcoming") }
    val filter = if ((filterOverride ?: localFilter) == "Past") "Past" else "Upcoming"
    val chooseFilter: (String) -> Unit = { localFilter = it; onFilterChanged?.invoke(it) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val matching = filterCoursework(state.entries, query, filter, now)
    val ordered = if (filter == "Past") matching.sortedByDescending { it.dueMillis } else matching.sortedBy { it.dueMillis }
    LazyColumn(Modifier.fillMaxSize().testTag("coursework-list"), contentPadding = PaddingValues(Spacing.page)) {
        item {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseworkSearch(query, { query = it }) { focus.clearFocus() }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf("Upcoming", "Past")
                    options.forEachIndexed { index, option ->
                        SegmentedButton(selected = filter == option, onClick = { chooseFilter(option); focus.clearFocus() },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)) { Text(option) }
                    }
                }
            }
        }
        if (showFeedback) item { Box(Modifier.padding(bottom = 12.dp)) { feedback() } }
        when {
            state.lastSynced == null -> item {
                DataEmptyState(if (busy) "Loading coursework…" else "Coursework hasn't loaded yet")
            }
            matching.isEmpty() -> item {
                val hasQuery = query.isNotBlank()
                DataEmptyState(if (hasQuery) "No matching deadlines" else if (filter == "Past") "No past deadlines" else "No upcoming deadlines",
                    action = if (hasQuery) "Clear search" else null, onAction = { query = ""; focus.clearFocus() })
            }
            else -> itemsIndexed(ordered, key = { _, entry -> entry.id }) { index, entry ->
                val top = if (index == 0) 14.dp else 0.dp
                val bottom = if (index == ordered.lastIndex) 14.dp else 0.dp
                Surface(shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        CourseworkRow(entry, now) { focus.clearFocus(); onSelect(entry) }
                        if (index < ordered.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                    }
                }
            }
        }
    }
}

fun filterCoursework(entries: List<CourseworkEntity>, query: String, filter: String, now: Long): List<CourseworkEntity> {
    return entries.filter { entry ->
        (query.isBlank() || entry.title.contains(query.trim(), true) || entry.description.contains(query.trim(), true)) &&
            (if (filter == "Past") entry.dueMillis < now else entry.dueMillis >= now)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkDetails(entry: CourseworkEntity, onDismiss: () -> Unit, onOpen: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    var linkFailed by remember(entry.id) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.testTag("coursework-details"), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text("COURSEWORK DETAILS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close coursework details") } }
        }
    }
}
