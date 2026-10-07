package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.res.stringResource

import java.util.Locale

import androidx.compose.ui.platform.LocalFocusManager

import uk.ac.warwick.plus.ui.components.*

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
    val focus = LocalFocusManager.current
    val ordered = remember(state.entries, query, filter, now) {
        val matching = filterCoursework(state.entries, query, filter, now)
        when (filter) {
            CourseworkFilter.PAST -> matching.sortedByDescending { it.dueMillis }
            CourseworkFilter.UPCOMING -> matching.sortedBy { it.dueMillis }
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("coursework-list"), contentPadding = PaddingValues(Spacing.page)) {
        item(contentType = "task-controls") {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, { query = it }, stringResource(R.string.search_items, stringResource(AppLabels.TASKS))) { focus.clearFocus() }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = CourseworkFilter.entries
                    options.forEachIndexed { index, option ->
                        SegmentedButton(selected = filter == option, onClick = { chooseFilter(option); focus.clearFocus() },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)) { Text(stringResource(option.labelRes)) }
                    }
                }
            }
        }
        if (showFeedback) item { Box(Modifier.padding(bottom = 12.dp)) { feedback() } }
        when {
            state.lastSynced == null -> item {
                DataEmptyState(if (busy) stringResource(R.string.loading_coursework) else stringResource(R.string.coursework_not_loaded))
            }
            ordered.isEmpty() -> item {
                val hasQuery = query.isNotBlank()
                DataEmptyState(if (hasQuery) stringResource(R.string.no_matching_deadlines) else if (filter == CourseworkFilter.PAST) stringResource(R.string.no_past_deadlines) else stringResource(R.string.no_upcoming_deadlines),
                    action = if (hasQuery) stringResource(AppActions.CLEAR_SEARCH) else null, onAction = { query = ""; focus.clearFocus() })
            }
            else -> itemsIndexed(ordered, key = { _, entry -> entry.id }, contentType = { _, _ -> "task" }) { index, entry ->
                GroupedListItem(first = index == 0, last = index == ordered.lastIndex) {
                    DeadlineRow(entry, now, DeadlinePresentation.List) { focus.clearFocus(); onSelect(entry) }
                }
            }
        }
    }
}
