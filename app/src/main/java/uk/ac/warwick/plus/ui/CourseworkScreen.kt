package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.data.CourseworkEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkContent(state: CourseworkState, now: Long, busy: Boolean,
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
                SearchField(query, { query = it }, "Search coursework") { focus.clearFocus() }
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
                GroupedListItem(first = index == 0, last = index == ordered.lastIndex) {
                    DeadlineRow(entry, now, DeadlinePresentation.List) { focus.clearFocus(); onSelect(entry) }
                }
            }
        }
    }
}
