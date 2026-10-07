package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.reportSyncFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*
import java.io.IOException

class TimetableViewModel(private val repository: TimetableStore,
    private val reportFailure: (Exception) -> Unit = ::reportSyncFailure, private val hasNetwork: () -> Boolean = { true }) : ViewModel() {
    private val mutable = MutableStateFlow(TimetableState(busy = true))
    val state = mutable.asStateFlow()
    private var syncJob: Job? = null
    private var cacheJob: Job? = null
    private var noticeId = 0L
    private var syncId = 0L
    private var refreshAfterCurrent = false
    init {
        syncJob = viewModelScope.launch {
            try { applyCache(repository.cached()) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(globalMessage = text(R.string.cache_open_failed)) }
            } finally { mutable.update { it.copy(busy = false) } }
            observeCache()
            refreshAfterCurrent = false
            refresh()
        }
    }
    private fun applyCache(cache: CachedTimetable) = mutable.update { it.withCache(cache) }
    private fun observeCache() {
        cacheJob = viewModelScope.launch {
            try { repository.observeCache().collect { cache ->
                if (!mutable.value.signingOut && !mutable.value.logoutFailed) applyCache(cache)
            } } catch (error: Exception) {
                if (error is CancellationException) throw error
                reportFailure(error)
                mutable.update { it.copy(globalMessage = text(R.string.cache_open_failed)) }
            }
        }
    }
    private fun authenticated(user: SignedInUser, clearedCache: CachedTimetable?) {
        mutable.update { state ->
            val current = if (clearedCache != null) state.withCache(clearedCache) else state
            current.copy(signedIn = true, needsLogin = false, globalMessage = null, name = user.name,
                accountCode = user.code, sessionCheckedAt = System.currentTimeMillis(),
                syncProgress = state.syncProgress?.copy(accountChecked = true))
        }
    }
    private fun startSync(resource: SyncResource? = null, olderMessages: Boolean = false,
        operation: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.signingOut || mutable.value.logoutFailed) return
        if (cacheJob?.isActive != true) observeCache()
        val progress = SyncProgress(++syncId, if (resource == null) SyncResource.entries.size else 1,
            operation = if (olderMessages) SyncOperation.OlderMessages
                else SyncOperation.Refresh(resource ?: SyncResource.TIMETABLE))
        mutable.update { it.copy(busy = true, notice = null,
            syncProgress = progress) }
        syncJob = viewModelScope.launch {
            try { operation() } finally {
                if (!mutable.value.signingOut) {
                    val current = mutable.value
                    val notice = current.completionNotice(noticeId + 1, resource, olderMessages)
                    if (notice != null) noticeId++
                    mutable.update { it.copy(busy = false, notice = notice,
                        syncProgress = it.syncProgress?.copy(finished = true)) }
                    if (refreshAfterCurrent) {
                        refreshAfterCurrent = false
                        refresh()
                    }
                }
            }
        }
    }
    fun consumeNotice(id: Long) = mutable.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    private fun beginResource(operation: SyncOperation) = mutable.update {
        it.copy(syncProgress = it.syncProgress?.copy(operation = operation, accountChecked = false, retry = 0))
    }
    private suspend fun finishResource(failed: Boolean) {
        if (!currentCoroutineContext().isActive || mutable.value.signingOut) return
        mutable.update { state -> state.copy(syncProgress = state.syncProgress?.let {
            it.copy(completed = (it.completed + 1).coerceAtMost(it.total), accountChecked = false,
                retry = 0, failures = it.failures + if (failed) 1 else 0)
        }) }
    }
    fun refresh() = startSync {
        mutable.update { it.copy(message = null, globalMessage = null, coursework = it.coursework.copy(message = null), account = it.account.copy(message = null),
            feeds = it.feeds.mapValues { (_, feed) -> feed.copy(message = null, olderPageFailed = false) }) }
        for (resource in SyncResource.entries) {
            currentCoroutineContext().ensureActive()
            val outcome = syncResource(resource)
            if (mutable.value.needsLogin) {
                mutable.update { state -> SyncResource.entries.dropWhile { it != resource }.drop(1).fold(state) { next, kind ->
                    if (next.issue(kind) == null) next.withIssue(kind, text(R.string.sign_in_update_resource, text(kind.labelRes))) else next
                } }
                break
            }
            // An exhausted transport failure is not proof of being offline. Stop this batch,
            // but allow a new pull-to-refresh or a targeted resource retry to try again.
            if (outcome == ResourceOutcome.TransportFailed || !mutable.value.signedIn) break
        }
    }
    fun refreshAfterSignIn() {
        if (mutable.value.signingOut || mutable.value.logoutFailed) return
        if (mutable.value.busy) refreshAfterCurrent = true else refresh()
    }
    private enum class ResourceOutcome { Updated, Failed, TransportFailed }
    // Every resource shares execution/recovery; the store still owns authentication and persistence.
    private suspend fun syncResource(resource: SyncResource, before: String? = null): ResourceOutcome {
        beginResource(if (before != null) SyncOperation.OlderMessages else SyncOperation.Refresh(resource))
        mutable.update { state ->
            val cleared = state.withIssue(resource, null)
            resource.feed?.let { kind -> cleared.withFeed(kind) { it.copy(loading = true) } } ?: cleared
        }
        try {
            if (!hasNetwork()) throw IOException("No active network")
            withSyncRetry(onRetry = { attempt ->
                mutable.update { it.copy(syncProgress = it.syncProgress?.copy(retry = attempt)) }
            }) {
                when (resource) {
                    SyncResource.TIMETABLE -> repository.sync(::authenticated)
                    SyncResource.COURSEWORK -> repository.syncCoursework(::authenticated)
                    SyncResource.ACCOUNT -> repository.syncAccount(::authenticated)
                    else -> repository.syncFeed(requireNotNull(resource.feed), before, ::authenticated)
                }
            }
            mutable.update { state ->
                val cleared = state.withIssue(resource, null)
                if (resource == SyncResource.TIMETABLE) cleared.copy(needsLogin = false, signedIn = true) else cleared
            }
            return ResourceOutcome.Updated
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error)
            mutable.update { it.resourceFailed(resource, error, before != null) }
            return if (error is IOException) ResourceOutcome.TransportFailed else ResourceOutcome.Failed
        } finally {
            resource.feed?.let { kind -> mutable.update { it.withFeed(kind) { feed -> feed.copy(loading = false) } } }
            finishResource(mutable.value.issue(resource) != null)
        }
    }
    fun refreshFeed(kind: FeedKind) = refreshResource(SyncResource.forFeed(kind))
    fun refreshResource(resource: SyncResource) = startSync(resource = resource) { syncResource(resource) }
    fun loadMoreMessages() {
        val feed = mutable.value.feed(FeedKind.MESSAGES)
        if (!feed.hasMore || mutable.value.needsLogin) return
        val cursor = feed.entries.lastOrNull()?.id ?: return
        startSync(SyncResource.MESSAGES, olderMessages = true) { syncResource(SyncResource.MESSAGES, cursor) }
    }
    fun signOut() {
        if (mutable.value.signingOut) return
        refreshAfterCurrent = false
        val previous = syncJob
        mutable.update { it.copy(signingOut = true, busy = true, syncProgress = null) }
        syncJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            cacheJob?.cancelAndJoin()
            mutable.value = TimetableState(busy = true, signingOut = true)
            try {
                repository.signOut()
                mutable.value = TimetableState(needsLogin = true)
                observeCache()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                reportFailure(error)
                mutable.value = TimetableState(needsLogin = true, logoutFailed = true,
                    globalMessage = text(R.string.logout_failed, text(AppLabels.ME)))
            }
        }
    }
}
