package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.*
import java.io.IOException

data class CourseworkState(val entries: List<CourseworkContentItem> = emptyList(), val lastSynced: Long? = null, val message: String? = null)
data class AccountState(val email: String = "", val lastSynced: Long? = null, val message: String? = null)
data class FeedState(val entries: List<FeedContentItem> = emptyList(), val lastSynced: Long? = null,
    val message: String? = null, val loading: Boolean = false, val hasMore: Boolean = false,
    val description: String = "", val url: String = "", val webReadMillis: Long = 0, val olderPageFailed: Boolean = false)

enum class SyncResource(val label: String, val feed: FeedKind? = null) {
    TIMETABLE("Timetable"), COURSEWORK("Coursework"), MESSAGES("Messages", FeedKind.MESSAGES),
    LIBRARY("Library", FeedKind.LIBRARY), MODULES("Modules", FeedKind.MODULES), ACCOUNT("Account");
    companion object { fun forFeed(kind: FeedKind) = entries.first { it.feed == kind } }
}
data class SyncNotice(val id: Long, val message: String, val feed: FeedKind? = null, val olderMessages: Boolean = false,
    val resource: SyncResource? = null)
// Progress measures settled resource updates, with a half-step after account verification.
// A settled failure is finished work, not a successful download; retries never add work units.
data class SyncProgress(val id: Long, val total: Int, val completed: Int = 0, val resource: String = "",
    val accountChecked: Boolean = false, val retry: Int = 0, val failures: Int = 0, val finished: Boolean = false) {
    val fraction: Float get() = ((completed + if (accountChecked) .5f else 0f) / total).coerceIn(0f, 1f)
    val description: String get() = if (finished) "$completed of $total updates finished; $failures failed"
        else if (retry > 0) "Retry $retry/2 · $resource"
        else "Updating $resource · ${completed + 1}/$total"
}
data class TimetableState(
    val events: List<EventContentItem> = emptyList(), val name: String = "", val lastSynced: Long? = null,
    val busy: Boolean = false, val signedIn: Boolean = false, val needsLogin: Boolean = false,
    val message: String? = null, val coursework: CourseworkState = CourseworkState(),
    val feeds: Map<FeedKind, FeedState> = emptyMap(), val accountCode: String = "",
    val sessionCheckedAt: Long? = null, val signingOut: Boolean = false, val logoutFailed: Boolean = false,
    val notice: SyncNotice? = null, val syncProgress: SyncProgress? = null, val account: AccountState = AccountState()
) {
    fun feed(kind: FeedKind) = feeds[kind] ?: FeedState()
    val email get() = account.email
    val hasSavedData get() = lastSynced != null || coursework.lastSynced != null || account.lastSynced != null || feeds.values.any { it.lastSynced != null }
    fun syncedAt(resource: SyncResource): Long? = when (resource) {
        SyncResource.TIMETABLE -> lastSynced
        SyncResource.COURSEWORK -> coursework.lastSynced
        SyncResource.ACCOUNT -> account.lastSynced
        else -> feed(resource.feed!!).lastSynced
    }
    fun issue(resource: SyncResource): String? = when (resource) {
        SyncResource.TIMETABLE -> message
        SyncResource.COURSEWORK -> coursework.message
        SyncResource.ACCOUNT -> account.message
        else -> feed(resource.feed!!).message
    }
    fun updating(resource: SyncResource): Boolean = if (resource.feed != null) feed(resource.feed).loading
        else busy && syncProgress?.resource == resource.label
}

// Cache projection only replaces persisted fields; transient operation state stays with the UI.
internal fun TimetableState.withCache(cache: CachedTimetable): TimetableState {
    val owner = (listOfNotNull(cache.sync, cache.courseworkSync, cache.accountSync) +
        cache.feeds.values.mapNotNull { it.sync }).maxByOrNull { it.syncedAt }
    val projectedCoursework = coursework.copy(
        entries = cache.coursework.map { it.snapshot() }.reuseIfEqual(coursework.entries),
        lastSynced = cache.courseworkSync?.syncedAt)
    return copy(events = cache.events.map { it.snapshot() }.reuseIfEqual(events), lastSynced = cache.sync?.syncedAt,
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
        })
}

internal fun TimetableState.withFeed(kind: FeedKind, transform: (FeedState) -> FeedState) =
    copy(feeds = feeds + (kind to transform(feed(kind))))

internal fun TimetableState.withIssue(resource: SyncResource, issue: String?, olderMessages: Boolean = false): TimetableState = when (resource) {
    SyncResource.TIMETABLE -> copy(message = issue)
    SyncResource.COURSEWORK -> copy(coursework = coursework.copy(message = issue))
    SyncResource.ACCOUNT -> copy(account = account.copy(message = issue))
    else -> withFeed(requireNotNull(resource.feed)) { it.copy(message = issue, olderPageFailed = olderMessages) }
}

internal fun TimetableState.resourceFailed(resource: SyncResource, error: Exception, olderMessages: Boolean): TimetableState {
    if (resource == SyncResource.TIMETABLE) return copy(
        needsLogin = error is SignInRequiredException,
        signedIn = signedIn && error !is SignInRequiredException,
        message = when (error) {
            is SignInRequiredException -> if (hasSavedData) "Your session has expired. Sign in to update your saved timetable." else "Sign in to load your timetable."
            is IOException -> if (lastSynced != null) "Couldn't connect. Showing your saved timetable." else "Couldn't connect. Connect to the internet and try again."
            is ServiceException -> "MyWarwick is unavailable (${error.status}). Try again later."
            else -> if (lastSynced != null) "Couldn't read the timetable. Your saved data has been kept." else "Your timetable couldn't be loaded. Try refreshing."
        })
    val session = if (error is SignInRequiredException) copy(needsLogin = true, signedIn = false,
        message = "Your session has expired. Sign in to update your saved data.") else this
    val label = resource.label.lowercase(java.util.Locale.UK)
    val issue = when (error) {
        is SignInRequiredException -> "Sign in to update $label."
        is IOException -> "Couldn't connect. Your saved $label has been kept."
        is ServiceException -> "$label is unavailable (${error.status}). Your saved data has been kept."
        else -> "Couldn't read $label. Your saved data has been kept."
    }
    return session.withIssue(resource, issue, olderMessages)
}
