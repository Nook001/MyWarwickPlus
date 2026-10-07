package uk.ac.warwick.plus.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyListState
import java.time.LocalDate
import uk.ac.warwick.plus.data.*

// Source compatibility for existing tests. The application uses typed actions/page projections;
// keep these adapters outside the state model and production page implementations.
@Composable
fun PlusScreen(state: TimetableState, onRefresh: () -> Unit, onLogin: () -> Unit,
    probe: ((ProbeEndpoint) -> ProbeResult)? = null, onCourseworkLink: ((String) -> Unit)? = null,
    onSignOut: () -> Unit = {}, onFeedRefresh: ((FeedKind) -> Unit)? = null,
    onMoreMessages: () -> Unit = {}, onExternalLink: ((String) -> Unit)? = null,
    onNoticeConsumed: (Long) -> Unit = {}, onResourceRefresh: ((SyncResource) -> Unit)? = null) {
    val resourceAction: (SyncResource) -> Unit = { resource ->
        onResourceRefresh?.invoke(resource)
            ?: resource.feed?.let { onFeedRefresh?.invoke(it) } ?: onRefresh()
    }
    PlusScreen(state, PlusActions(onRefresh, onLogin, onSignOut, resourceAction,
        onMoreMessages, onNoticeConsumed, onExternalLink ?: onCourseworkLink, probe))
}

@Composable
fun ScheduleContent(state: TimetableState, today: LocalDate, now: Long, from: LocalDate,
    listState: LazyListState, feedback: @Composable () -> Unit = {}, onSelect: (EventEntity) -> Unit) {
    // Preserve the existing entity-based entry point; the app uses the value-only overload below.
    ScheduleContent(state.schedulePage(), today, now, from, listState, feedback) { item ->
        onSelect(item as? EventEntity ?: EventEntity().apply {
            id = item.id; title = item.title; module = item.module; moduleName = item.moduleName
            location = item.location; locationUrl = item.locationUrl
            startMillis = item.startMillis; endMillis = item.endMillis
            allDay = item.allDay; academicWeek = item.academicWeek
        })
    }
}

@Composable
fun ResourceRecoveryRow(state: TimetableState, resource: SyncResource, onLogin: () -> Unit, onRefresh: () -> Unit) {
    DataRecoveryRow(state.syncedAt(resource), state.issue(resource), state.updating(resource), state.needsLogin,
        !state.busy && !state.signingOut && !state.logoutFailed, onLogin, onRefresh,
        resource.feed?.let { state.feed(it).olderPageFailed } == true)
}

fun filterCoursework(entries: List<CourseworkContentItem>, query: String, filter: String, now: Long): List<CourseworkContentItem> {
    return filterCoursework(entries, query, CourseworkFilter.restore(filter), now)
}

fun SyncNotice(id: Long, message: String, feed: FeedKind?, olderMessages: Boolean = false,
    resource: SyncResource? = null): SyncNotice = SyncNotice(id, message, when {
    olderMessages -> RecoveryAction.OlderMessages
    resource != null -> RecoveryAction.Refresh(resource)
    feed != null -> RecoveryAction.Refresh(SyncResource.forFeed(feed))
    else -> RecoveryAction.RefreshAll
})

val SyncNotice.resource: SyncResource? get() = when (val recovery = action) {
    is RecoveryAction.Refresh -> recovery.resource
    RecoveryAction.OlderMessages -> SyncResource.MESSAGES
    else -> null
}
val SyncNotice.feed: FeedKind? get() = resource?.feed
val SyncNotice.olderMessages: Boolean get() = action == RecoveryAction.OlderMessages
