package uk.ac.warwick.plus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.ac.warwick.plus.data.*
import java.io.IOException

data class CourseworkState(val entries: List<CourseworkEntity> = emptyList(), val lastSynced: Long? = null, val message: String? = null)
data class FeedState(val entries: List<FeedEntry> = emptyList(), val lastSynced: Long? = null,
    val message: String? = null, val loading: Boolean = false, val hasMore: Boolean = false,
    val description: String = "", val url: String = "", val webReadMillis: Long = 0, val olderPageFailed: Boolean = false)

enum class SyncResource(val label: String, val feed: FeedKind? = null) {
    TIMETABLE("Timetable"), COURSEWORK("Coursework"), MESSAGES("Messages", FeedKind.MESSAGES),
    LIBRARY("Library", FeedKind.LIBRARY), MODULES("Modules", FeedKind.MODULES);
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
    val events: List<EventEntity> = emptyList(), val name: String = "", val lastSynced: Long? = null,
    val busy: Boolean = false, val signedIn: Boolean = false, val needsLogin: Boolean = false,
    val message: String? = null, val coursework: CourseworkState = CourseworkState(),
    val feeds: Map<FeedKind, FeedState> = emptyMap(), val accountCode: String = "",
    val sessionCheckedAt: Long? = null, val signingOut: Boolean = false, val logoutFailed: Boolean = false,
    val notice: SyncNotice? = null, val syncProgress: SyncProgress? = null
) {
    fun feed(kind: FeedKind) = feeds[kind] ?: FeedState()
    val hasSavedData get() = lastSynced != null || coursework.lastSynced != null || feeds.values.any { it.lastSynced != null }
    fun syncedAt(resource: SyncResource): Long? = when (resource) {
        SyncResource.TIMETABLE -> lastSynced
        SyncResource.COURSEWORK -> coursework.lastSynced
        else -> feed(resource.feed!!).lastSynced
    }
    fun issue(resource: SyncResource): String? = when (resource) {
        SyncResource.TIMETABLE -> message
        SyncResource.COURSEWORK -> coursework.message
        else -> feed(resource.feed!!).message
    }
    fun updating(resource: SyncResource): Boolean = if (resource.feed != null) feed(resource.feed).loading
        else busy && syncProgress?.resource == resource.label
}

class TimetableViewModel(private val repository: TimetableStore,
    private val reportFailure: (Exception) -> Unit = {
        if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.w("MyWarwickPlus", "Student data sync failed: ${it.javaClass.simpleName}")
    }) : ViewModel() {
    private val mutable = MutableStateFlow(TimetableState(busy = true))
    val state = mutable.asStateFlow()
    private var syncJob: Job? = null
    private var noticeId = 0L
    private var syncId = 0L
    init {
        syncJob = viewModelScope.launch {
            try { applyCache(repository.cached()) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(message = "Couldn't open your saved data. Try refreshing.") }
            } finally { mutable.update { it.copy(busy = false) } }
            refresh()
        }
    }
    private fun applyCache(cache: CachedTimetable) {
        val account = (listOfNotNull(cache.sync, cache.courseworkSync) + cache.feeds.values.mapNotNull { it.sync }).maxByOrNull { it.syncedAt }
        mutable.update { current -> current.copy(events = cache.events,
            name = account?.displayName ?: current.name, accountCode = account?.userCode ?: current.accountCode,
            lastSynced = cache.sync?.syncedAt,
            coursework = current.coursework.copy(entries = cache.coursework, lastSynced = cache.courseworkSync?.syncedAt),
            feeds = FeedKind.entries.associateWith { kind ->
                val saved = cache.feeds[kind]
                current.feed(kind).copy(entries = saved?.entries.orEmpty(), lastSynced = saved?.sync?.syncedAt,
                    description = saved?.meta?.description.orEmpty(), url = saved?.meta?.url.orEmpty(),
                    hasMore = saved?.meta?.hasMore ?: false, webReadMillis = saved?.meta?.webReadMillis ?: 0)
            }) }
    }
    private fun authenticated(user: SignedInUser, cache: CachedTimetable) {
        applyCache(cache)
        mutable.update { it.copy(signedIn = true, needsLogin = false, name = user.name,
            accountCode = user.code, sessionCheckedAt = System.currentTimeMillis(),
            syncProgress = it.syncProgress?.copy(accountChecked = true)) }
    }
    private suspend fun restoreCache() {
        try { applyCache(repository.cached()) }
        catch (error: Exception) { if (error is CancellationException) throw error }
    }
    private fun startSync(feed: FeedKind? = null, olderMessages: Boolean = false, resource: SyncResource? = null,
        operation: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.signingOut || mutable.value.logoutFailed) return
        val target = resource ?: feed?.let(SyncResource::forFeed)
        val progress = SyncProgress(++syncId, if (target == null) SyncResource.entries.size else 1,
            resource = if (olderMessages) "Older messages" else target?.label ?: "Timetable")
        mutable.update { it.copy(busy = true, notice = null,
            syncProgress = progress) }
        syncJob = viewModelScope.launch {
            try { operation() } finally {
                if (!mutable.value.signingOut) {
                    val current = mutable.value
                    val failures = (target?.let(::listOf) ?: SyncResource.entries).mapNotNull { kind ->
                        current.issue(kind)?.let { kind to it }
                    }
                    val notice = if (failures.isEmpty()) null else SyncNotice(++noticeId,
                        if (current.needsLogin) "Sign in to update your information."
                        else if (failures.size > 1) "Couldn't update some information. Your saved data has been kept."
                        else failures.single().second,
                        failures.singleOrNull()?.first?.feed, olderMessages, target ?: failures.singleOrNull()?.first)
                    mutable.update { it.copy(busy = false, notice = notice,
                        syncProgress = it.syncProgress?.copy(finished = true)) }
                }
            }
        }
    }
    fun consumeNotice(id: Long) = mutable.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    private fun beginResource(label: String) = mutable.update {
        it.copy(syncProgress = it.syncProgress?.copy(resource = label, accountChecked = false, retry = 0))
    }
    private suspend fun finishResource(failed: Boolean) {
        if (!currentCoroutineContext().isActive || mutable.value.signingOut) return
        mutable.update { state -> state.copy(syncProgress = state.syncProgress?.let {
            it.copy(completed = (it.completed + 1).coerceAtMost(it.total), accountChecked = false,
                retry = 0, failures = it.failures + if (failed) 1 else 0)
        }) }
    }
    // Only transient read failures are retried; successful resources are never downloaded again.
    private suspend fun <T> withSyncRetry(operation: suspend () -> T): T {
        for (attempt in 0..2) {
            currentCoroutineContext().ensureActive()
            try { return operation() } catch (error: Exception) {
                val transient = error is IOException || error is ServiceException && (error.status == 408 || error.status in 500..599)
                if (error is CancellationException || !transient || attempt == 2) throw error
                mutable.update { it.copy(syncProgress = it.syncProgress?.copy(retry = attempt + 1)) }
                delay(if (attempt == 0) 2_000L else 5_000L)
            }
        }
        error("Unreachable retry state")
    }
    fun refresh() = startSync {
        mutable.update { it.copy(message = null, coursework = it.coursework.copy(message = null),
            feeds = it.feeds.mapValues { (_, feed) -> feed.copy(message = null, olderPageFailed = false) }) }
        refreshTimetable()
        if (mutable.value.signedIn && !mutable.value.needsLogin) {
            refreshCoursework()
            for (kind in FeedKind.entries) {
                currentCoroutineContext().ensureActive()
                if (mutable.value.needsLogin) break
                syncFeed(kind)
            }
        } else mutable.update { it.copy(coursework = it.coursework.copy(message =
            if (it.needsLogin) "Sign in to update coursework." else "Couldn't update coursework. Connect and refresh to try again.")) }
    }
    private suspend fun refreshTimetable() {
        beginResource("Timetable")
        try {
            applyCache(withSyncRetry { repository.sync(::authenticated) })
            mutable.update { it.copy(needsLogin = false, signedIn = true) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache()
            mutable.update { current -> current.copy(needsLogin = error is SignInRequiredException,
                signedIn = current.signedIn && error !is SignInRequiredException,
                message = when (error) {
                    is SignInRequiredException -> if (current.hasSavedData) "Your session has expired. Sign in to update your saved timetable." else "Sign in to load your timetable."
                    is IOException -> if (current.lastSynced != null) "Couldn't connect. Showing your saved timetable." else "Couldn't connect. Connect to the internet and try again."
                    is ServiceException -> "MyWarwick is unavailable (${error.status}). Try again later."
                    else -> if (current.lastSynced != null) "Couldn't read the timetable. Your saved data has been kept." else "Your timetable couldn't be loaded. Try refreshing."
                }) }
        } finally { finishResource(mutable.value.message != null) }
    }
    private fun sessionFailure(error: Exception) {
        if (error is SignInRequiredException) mutable.update { it.copy(needsLogin = true, signedIn = false,
            message = "Your session has expired. Sign in to update your saved data.") }
    }
    private suspend fun refreshCoursework() {
        beginResource("Coursework")
        try {
            applyCache(withSyncRetry { repository.syncCoursework(::authenticated) })
            mutable.update { it.copy(coursework = it.coursework.copy(message = null)) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache(); sessionFailure(error)
            mutable.update { it.copy(coursework = it.coursework.copy(message = failureMessage("coursework", error))) }
        } finally { finishResource(mutable.value.coursework.message != null) }
    }
    private fun updateFeed(kind: FeedKind, transform: (FeedState) -> FeedState) = mutable.update {
        it.copy(feeds = it.feeds + (kind to transform(it.feed(kind))))
    }
    private suspend fun syncFeed(kind: FeedKind, before: String? = null) {
        beginResource(if (before != null) "Older messages" else kind.label)
        updateFeed(kind) { it.copy(loading = true, message = null, olderPageFailed = false) }
        try { applyCache(withSyncRetry { repository.syncFeed(kind, before, ::authenticated) }) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache(); sessionFailure(error)
            updateFeed(kind) { it.copy(message = failureMessage(kind.label.lowercase(), error), olderPageFailed = before != null) }
        } finally {
            updateFeed(kind) { it.copy(loading = false) }
            finishResource(mutable.value.feed(kind).message != null)
        }
    }
    fun refreshFeed(kind: FeedKind) = startSync(kind) { syncFeed(kind) }
    fun refreshResource(resource: SyncResource) {
        when (resource) {
            SyncResource.TIMETABLE -> startSync(resource = resource) {
                mutable.update { it.copy(message = null) }; refreshTimetable()
            }
            SyncResource.COURSEWORK -> startSync(resource = resource) {
                mutable.update { it.copy(coursework = it.coursework.copy(message = null)) }; refreshCoursework()
            }
            else -> refreshFeed(resource.feed!!)
        }
    }
    fun loadMoreMessages() {
        val feed = mutable.value.feed(FeedKind.MESSAGES)
        if (!feed.hasMore || mutable.value.needsLogin) return
        val cursor = feed.entries.lastOrNull()?.id ?: return
        startSync(FeedKind.MESSAGES, olderMessages = true) { syncFeed(FeedKind.MESSAGES, cursor) }
    }
    fun signOut() {
        if (mutable.value.signingOut) return
        val previous = syncJob
        mutable.update { it.copy(signingOut = true, busy = true, syncProgress = null) }
        syncJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            mutable.value = TimetableState(busy = true, signingOut = true)
            try {
                repository.signOut()
                mutable.value = TimetableState(needsLogin = true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                reportFailure(error)
                mutable.value = TimetableState(needsLogin = true, logoutFailed = true,
                    message = "Couldn't finish signing out. Retry from More before signing in again.")
            }
        }
    }
    private fun failureMessage(label: String, error: Exception) = when (error) {
        is SignInRequiredException -> "Sign in to update $label."
        is IOException -> "Couldn't connect. Your saved $label has been kept."
        is ServiceException -> "$label is unavailable (${error.status}). Your saved data has been kept."
        else -> "Couldn't read $label. Your saved data has been kept."
    }
}
