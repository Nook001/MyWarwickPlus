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
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.CourseworkContentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseworkContent(state: CourseworkState, now: Long, busy: Boolean,
    feedback: @Composable () -> Unit = {}, showFeedback: Boolean = false,
    filterOverride: CourseworkFilter? = null, onFilterChanged: ((CourseworkFilter) -> Unit)? = null,
    onSelect: (CourseworkContentItem) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var localFilter by rememberSaveable(stateSaver = CourseworkFilter.saver) { mutableStateOf(CourseworkFilter.UPCOMING) }
    val filter = filterOverride ?: localFilter
    val chooseFilter: (CourseworkFilter) -> Unit = { localFilter = it; onFilterChanged?.invoke(it) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val ordered = remember(state.entries, query, filter, now) {
        val matching = filterCoursework(state.entries, query, filter, now)
        when (filter) {
            CourseworkFilter.PAST -> matching.sortedByDescending { it.dueMillis }
            CourseworkFilter.UPCOMING -> matching.sortedBy { it.dueMillis }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("coursework-list"), contentPadding = PaddingValues(Spacing.page)) {
        item {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, { query = it }, "Search ${AppLabels.TASKS.lowercase(java.util.Locale.UK)}") { focus.clearFocus() }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = CourseworkFilter.entries
                    options.forEachIndexed { index, option ->
                        SegmentedButton(selected = filter == option, onClick = { chooseFilter(option); focus.clearFocus() },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)) { Text(option.label) }
                    }
                }
            }
        }
        if (showFeedback) item { Box(Modifier.padding(bottom = 12.dp)) { feedback() } }
        when {
            state.lastSynced == null -> item {
                DataEmptyState(if (busy) "Loading coursework…" else "Coursework hasn't loaded yet")
            }
            ordered.isEmpty() -> item {
                val hasQuery = query.isNotBlank()
                DataEmptyState(if (hasQuery) "No matching deadlines" else if (filter == CourseworkFilter.PAST) "No past deadlines" else "No upcoming deadlines",
                    action = if (hasQuery) AppActions.CLEAR_SEARCH else null, onAction = { query = ""; focus.clearFocus() })
            }
            else -> itemsIndexed(ordered, key = { _, entry -> entry.id }) { index, entry ->
                GroupedListItem(first = index == 0, last = index == ordered.lastIndex) {
                    DeadlineRow(entry, now, DeadlinePresentation.List) { focus.clearFocus(); onSelect(entry) }
                }
            }
        }
    }
}
