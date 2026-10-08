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
    private val reportFailure: (Exception) -> Unit = ::reportSyncFailure, private val hasNetwork: () -> Boolean = { true },
    private val nowMillis: () -> Long = System::currentTimeMillis) : ViewModel() {
    private val mutable = MutableStateFlow(TimetableState(busy = true))
    val state = mutable.asStateFlow()
    private var syncJob: Job? = null
    private var cacheJob: Job? = null
    private var noticeId = 0L
    private var syncId = 0L
    private var refreshAfterCurrent = false
    private var homeVisible = false
    private var pendingHome = false
    private val serviceAttempts = mutableMapOf<SyncResource, Long>()
    init {
        syncJob = viewModelScope.launch {
            try { applyCache(repository.cached()) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update { it.copy(globalMessage = text(R.string.cache_open_failed)) }
            } finally { mutable.update { it.copy(busy = false) } }
            observeCache()
            refreshAfterCurrent = false
            pendingHome = homeVisible
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
        if (clearedCache != null) {
            serviceAttempts.clear()
            pendingHome = homeVisible
        }
        mutable.update { state ->
            val current = if (clearedCache != null) state.withCache(clearedCache) else state
            current.copy(signedIn = true, needsLogin = false, globalMessage = null, name = user.name,
                accountCode = user.code, sessionCheckedAt = System.currentTimeMillis(),
                syncProgress = state.syncProgress?.copy(accountChecked = true))
        }
    }
    private fun startSync(resource: SyncResource? = null, olderMessages: Boolean = false,
        resources: List<SyncResource> = resource?.let(::listOf) ?: SyncResource.core,
        operation: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.signingOut || mutable.value.logoutFailed) return
        if (cacheJob?.isActive != true) observeCache()
        val progress = SyncProgress(++syncId, resources.size,
            operation = if (olderMessages) SyncOperation.OlderMessages
                else SyncOperation.Refresh(resource ?: SyncResource.TIMETABLE))
        mutable.update { it.copy(busy = true, notice = null,
            syncProgress = progress) }
        syncJob = viewModelScope.launch {
            try { operation() } finally {
                if (!mutable.value.signingOut) {
                    val current = mutable.value
                    val notice = current.completionNotice(noticeId + 1, resource, olderMessages, resources)
                    if (notice != null) noticeId++
                    mutable.update { it.copy(busy = false, notice = notice,
                        syncProgress = it.syncProgress?.copy(finished = true)) }
                    if (refreshAfterCurrent) {
                        refreshAfterCurrent = false
                        refresh()
                    } else if (pendingHome) {
                        pendingHome = false
                        refreshHomeServicesIfDue()
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
    fun refresh() = refreshBatch(SyncResource.core)
    fun refreshHome() = refreshBatch(SyncResource.core + SyncResource.homeServices)

    fun setHomeVisible(visible: Boolean) {
        homeVisible = visible
        if (!visible) { pendingHome = false; return }
        if (mutable.value.busy) pendingHome = true else refreshHomeServicesIfDue()
    }

    private fun refreshHomeServicesIfDue() {
        if (!homeVisible || mutable.value.needsLogin || mutable.value.logoutFailed || mutable.value.signingOut || !hasNetwork()) return
        val now = nowMillis()
        val due = SyncResource.homeServices.filter { resource ->
            val latest = maxOf(mutable.value.syncedAt(resource) ?: 0L, serviceAttempts[resource] ?: 0L)
            latest == 0L || now < latest || now - latest >= requireNotNull(resource.service).refreshMillis
        }
        if (due.isNotEmpty()) refreshBatch(due)
    }

    private fun refreshBatch(resources: List<SyncResource>) = startSync(resources = resources) {
        mutable.update { current -> resources.fold(current.copy(globalMessage = null)) { state, resource -> state.withIssue(resource, null) } }
        for (resource in resources) {
            currentCoroutineContext().ensureActive()
            val outcome = syncResource(resource)
            if (mutable.value.needsLogin) {
                mutable.update { state -> resources.dropWhile { it != resource }.drop(1).fold(state) { next, kind ->
                    if (next.issue(kind) == null) next.withIssue(kind, text(R.string.sign_in_update_resource, text(kind.labelRes))) else next
                } }
                break
            }
            // An exhausted transport failure is not proof of being offline. Stop this batch,
            // but allow a new pull-to-refresh or a targeted resource retry to try again.
            if (outcome == ResourceOutcome.TransportFailed || !mutable.value.signedIn) {
                pendingHome = false
                break
            }
        }
    }
    fun refreshAfterSignIn() {
        if (mutable.value.signingOut || mutable.value.logoutFailed) return
        serviceAttempts.clear()
        pendingHome = homeVisible
        if (mutable.value.busy) refreshAfterCurrent = true else refresh()
    }
    private enum class ResourceOutcome { Updated, Failed, TransportFailed }
    // Every resource shares execution/recovery; the store still owns authentication and persistence.
    private suspend fun syncResource(resource: SyncResource, before: String? = null): ResourceOutcome {
        if (resource.service != null) serviceAttempts[resource] = nowMillis()
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
                    else -> if (resource.service != null) repository.syncService(resource.service, ::authenticated)
                        else repository.syncFeed(requireNotNull(resource.feed), before, ::authenticated)
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
        pendingHome = false
        serviceAttempts.clear()
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
