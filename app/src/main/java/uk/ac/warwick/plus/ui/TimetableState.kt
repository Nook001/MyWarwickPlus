package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import java.util.Locale
import uk.ac.warwick.plus.ui.components.*
import java.io.IOException
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*

data class CourseworkState(val entries: List<CourseworkContentItem> = emptyList(), val lastSynced: Long? = null, val message: UiText? = null)
data class AccountState(val email: String = "", val lastSynced: Long? = null, val message: UiText? = null)
data class FeedState(val entries: List<FeedContentItem> = emptyList(), val lastSynced: Long? = null,
    val message: UiText? = null, val loading: Boolean = false, val hasMore: Boolean = false,
    val description: String = "", val url: String = "", val webReadMillis: Long = 0, val olderPageFailed: Boolean = false)

data class ServiceState(val summaries: List<ServiceSummary> = emptyList(), val events: List<CampusEvent> = emptyList(),
    val lastSynced: Long? = null, val message: UiText? = null, val description: String = "", val url: String = "")

enum class SyncResource(val labelRes: Int, val slot: Int, val feed: FeedKind? = null, val service: ServiceKind? = null) {
    TIMETABLE(AppLabels.CLASSES, SyncSlots.TIMETABLE), COURSEWORK(AppLabels.TASKS, SyncSlots.COURSEWORK), MESSAGES(AppLabels.MESSAGES, SyncSlots.MESSAGES, FeedKind.MESSAGES),
    LIBRARY(AppLabels.LIBRARY, SyncSlots.LIBRARY, FeedKind.LIBRARY), MODULES(AppLabels.MODULES, SyncSlots.MODULES, FeedKind.MODULES), ACCOUNT(AppLabels.ACCOUNT, SyncSlots.ACCOUNT),
    BUSES(AppLabels.BUSES, SyncSlots.BUSES, service = ServiceKind.BUSES),
    PRINT(AppLabels.PRINT, SyncSlots.PRINT, service = ServiceKind.PRINT),
    EVENTS(AppLabels.EVENTS, SyncSlots.CAMPUS_EVENTS, service = ServiceKind.EVENTS);
    companion object {
        val core = entries.filter { it.service == null }
        val homeServices = entries.filter { it.service != null }
        fun forFeed(kind: FeedKind) = entries.first { it.feed == kind }
    }
}
data class SyncNotice(val id: Long, val message: UiText, val action: RecoveryAction = RecoveryAction.RefreshAll)
// Progress measures settled resource updates, with a half-step after account verification.
// A settled failure is finished work, not a successful download; retries never add work units.
data class SyncProgress(val id: Long, val total: Int, val completed: Int = 0,
    val operation: SyncOperation = SyncOperation.Refresh(SyncResource.TIMETABLE),
    val accountChecked: Boolean = false, val retry: Int = 0, val failures: Int = 0, val finished: Boolean = false) {
    val fraction: Float get() = ((completed + if (accountChecked) .5f else 0f) / total).coerceIn(0f, 1f)
    val description: UiText get() = if (finished) UiText.Quantity(R.plurals.progress_finished, total, listOf(completed, total, failures))
        else if (retry > 0) text(R.string.progress_retry, text(AppActions.RETRY), retry, text(operation.labelRes))
        else text(R.string.progress_updating, text(operation.labelRes), completed + 1, total)
}
data class TimetableState(
    val events: List<EventContentItem> = emptyList(), val name: String = "", val lastSynced: Long? = null,
    val busy: Boolean = false, val signedIn: Boolean = false, val needsLogin: Boolean = false,
    val message: UiText? = null, val coursework: CourseworkState = CourseworkState(),
    val feeds: Map<FeedKind, FeedState> = emptyMap(), val accountCode: String = "",
    val sessionCheckedAt: Long? = null, val signingOut: Boolean = false, val logoutFailed: Boolean = false,
    val notice: SyncNotice? = null, val syncProgress: SyncProgress? = null, val account: AccountState = AccountState(),
    val globalMessage: UiText? = null,
    internal val cacheRevision: Long = -1, val services: Map<ServiceKind, ServiceState> = emptyMap()
) {
    fun feed(kind: FeedKind) = feeds[kind] ?: FeedState()
    fun service(kind: ServiceKind) = services[kind] ?: ServiceState()
    val email get() = account.email
    val hasSavedData get() = lastSynced != null || coursework.lastSynced != null || account.lastSynced != null || feeds.values.any { it.lastSynced != null } || services.values.any { it.lastSynced != null }
    fun syncedAt(resource: SyncResource): Long? = when (resource) {
        SyncResource.TIMETABLE -> lastSynced
        SyncResource.COURSEWORK -> coursework.lastSynced
        SyncResource.ACCOUNT -> account.lastSynced
        else -> if (resource.service != null) service(resource.service).lastSynced else feed(requireNotNull(resource.feed)).lastSynced
    }
    fun issue(resource: SyncResource): UiText? = when (resource) {
        SyncResource.TIMETABLE -> message
        SyncResource.COURSEWORK -> coursework.message
        SyncResource.ACCOUNT -> account.message
        else -> if (resource.service != null) service(resource.service).message else feed(requireNotNull(resource.feed)).message
    }
    fun updating(resource: SyncResource): Boolean = if (resource.feed != null) feed(resource.feed).loading
        else busy && syncProgress?.operation?.resource == resource
}

// Cache projection only replaces persisted fields; transient operation state stays with the UI.
internal fun TimetableState.withCache(cache: CachedTimetable): TimetableState {
    // A Flow read may arrive after authentication already cleared another account.
    if (cache.revision < cacheRevision) return this
    val owner = (listOfNotNull(cache.sync, cache.courseworkSync, cache.accountSync) +
        cache.feeds.values.mapNotNull { it.sync } + cache.services.values.mapNotNull { it.sync }).maxByOrNull { it.syncedAt }
    val projectedCoursework = coursework.copy(
        entries = cache.coursework.map { it.snapshot() }.reuseIfEqual(coursework.entries),
        lastSynced = cache.courseworkSync?.syncedAt)
    return copy(cacheRevision = cache.revision, events = cache.events.map { it.snapshot() }.reuseIfEqual(events), lastSynced = cache.sync?.syncedAt,
        name = owner?.displayName ?: name, accountCode = owner?.userCode ?: accountCode,
        account = account.copy(email = cache.accountSync?.email.orEmpty(), lastSynced = cache.accountSync?.syncedAt),
        coursework = if (projectedCoursework == coursework) coursework else projectedCoursework,
        feeds = FeedKind.entries.associateWith { kind ->
            val saved = cache.feeds[kind]
            val previous = feed(kind)
            val projected = previous.copy(entries = saved?.entries.orEmpty().map { it.snapshot() }.reuseIfEqual(previous.entries), lastSynced = saved?.sync?.syncedAt,
                description = saved?.meta?.description.orEmpty(), url = saved?.meta?.url.orEmpty(),
                hasMore = saved?.meta?.hasMore ?: false, webReadMillis = saved?.meta?.webReadMillis ?: 0)
            if (projected == previous) previous else projected
        }, services = ServiceKind.entries.associateWith { kind ->
            val saved = cache.services[kind]
            val previous = service(kind)
            val projected = previous.copy(
                summaries = saved?.summaries.orEmpty().map { it.snapshot() }.reuseIfEqual(previous.summaries),
                events = saved?.events.orEmpty().map { it.snapshot() }.reuseIfEqual(previous.events),
                lastSynced = saved?.sync?.syncedAt, description = saved?.meta?.description.orEmpty(), url = saved?.meta?.url.orEmpty())
            if (projected == previous) previous else projected
        })
}

internal fun TimetableState.withFeed(kind: FeedKind, transform: (FeedState) -> FeedState) =
    copy(feeds = feeds + (kind to transform(feed(kind))))

internal fun TimetableState.withIssue(resource: SyncResource, issue: UiText?, olderMessages: Boolean = false): TimetableState = when (resource) {
    SyncResource.TIMETABLE -> copy(message = issue)
    SyncResource.COURSEWORK -> copy(coursework = coursework.copy(message = issue))
    SyncResource.ACCOUNT -> copy(account = account.copy(message = issue))
    else -> resource.service?.let { kind -> copy(services = services + (kind to service(kind).copy(message = issue))) }
        ?: withFeed(requireNotNull(resource.feed)) { it.copy(message = issue, olderPageFailed = olderMessages) }
}

internal fun TimetableState.resourceFailed(resource: SyncResource, error: Exception, olderMessages: Boolean): TimetableState {
    if (resource == SyncResource.TIMETABLE) return copy(
        needsLogin = error is SignInRequiredException,
        signedIn = signedIn && error !is SignInRequiredException,
        message = when (error) {
            is SignInRequiredException -> if (hasSavedData) text(R.string.session_expired_timetable) else text(R.string.sign_in_timetable)
            is IOException -> if (lastSynced != null) text(R.string.connection_saved_timetable) else text(R.string.connection_failed)
            is ServiceException -> text(R.string.service_unavailable, error.status)
            else -> if (lastSynced != null) text(R.string.timetable_read_failed_saved) else text(R.string.timetable_load_failed)
        })
    val session = if (error is SignInRequiredException) copy(needsLogin = true, signedIn = false,
        globalMessage = text(R.string.session_expired_data)) else this
    val label = text(resource.labelRes)
    val issue = when (error) {
        is SignInRequiredException -> text(R.string.sign_in_update_resource, label)
        is IOException -> text(R.string.connection_saved_data)
        is ServiceException -> text(R.string.resource_update_failed, label, error.status)
        else -> text(R.string.resource_read_failed, label)
    }
    return session.withIssue(resource, issue, olderMessages)
}
