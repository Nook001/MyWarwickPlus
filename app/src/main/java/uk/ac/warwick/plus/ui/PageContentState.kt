package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.EventContentItem

internal data class RecoveryState(val lastSynced: Long?, val issue: UiText?, val updating: Boolean,
    val needsLogin: Boolean, val enabled: Boolean)

internal fun TimetableState.recovery(resource: SyncResource) = RecoveryState(
    syncedAt(resource), issue(resource), updating(resource), needsLogin,
    !busy && !signingOut && !logoutFailed)

internal data class HomePageState(val events: List<EventContentItem>, val lastSynced: Long?,
    val busy: Boolean, val coursework: CourseworkState,
    val timetableRecovery: RecoveryState, val courseworkRecovery: RecoveryState,
    val conflicts: Set<String>)

internal fun TimetableState.homePage(conflicts: Set<String> = conflictingEventIds(events)) = HomePageState(events, lastSynced, busy, coursework,
    recovery(SyncResource.TIMETABLE), recovery(SyncResource.COURSEWORK), conflicts)

internal data class SchedulePageState(val events: List<EventContentItem>, val lastSynced: Long?,
    val busy: Boolean, val showFeedback: Boolean, val conflicts: Set<String>)

internal fun TimetableState.schedulePage(conflicts: Set<String> = conflictingEventIds(events)) = SchedulePageState(events, lastSynced, busy,
    needsLogin || message != null, conflicts)
