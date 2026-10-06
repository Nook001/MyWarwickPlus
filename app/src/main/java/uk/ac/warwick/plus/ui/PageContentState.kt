package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.EventContentItem

internal data class RecoveryState(val lastSynced: Long?, val issue: String?, val updating: Boolean,
    val needsLogin: Boolean, val enabled: Boolean)

internal fun TimetableState.recovery(resource: SyncResource) = RecoveryState(
    syncedAt(resource), issue(resource), updating(resource), needsLogin,
    !busy && !signingOut && !logoutFailed)

internal data class HomePageState(val events: List<EventContentItem>, val lastSynced: Long?,
    val busy: Boolean, val coursework: CourseworkState,
    val timetableRecovery: RecoveryState, val courseworkRecovery: RecoveryState)

internal fun TimetableState.homePage() = HomePageState(events, lastSynced, busy, coursework,
    recovery(SyncResource.TIMETABLE), recovery(SyncResource.COURSEWORK))

internal data class SchedulePageState(val events: List<EventContentItem>, val lastSynced: Long?,
    val busy: Boolean, val showFeedback: Boolean)

internal fun TimetableState.schedulePage() = SchedulePageState(events, lastSynced, busy,
    needsLogin || message != null)
