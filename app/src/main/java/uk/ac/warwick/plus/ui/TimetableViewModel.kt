package uk.ac.warwick.plus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.ac.warwick.plus.data.*

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
    private fun applyCache(cache: CachedTimetable) = mutable.update { it.withCache(cache) }
    private fun authenticated(user: SignedInUser, cache: CachedTimetable) {
        mutable.update { it.withCache(cache).copy(signedIn = true, needsLogin = false, name = user.name,
            accountCode = user.code, sessionCheckedAt = System.currentTimeMillis(),
            syncProgress = it.syncProgress?.copy(accountChecked = true)) }
    }
    private suspend fun restoreCache() {
        try { applyCache(repository.cached()) }
        catch (error: Exception) { if (error is CancellationException) throw error }
    }
    private fun startSync(resource: SyncResource? = null, olderMessages: Boolean = false,
        operation: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.signingOut || mutable.value.logoutFailed) return
        val progress = SyncProgress(++syncId, if (resource == null) SyncResource.entries.size else 1,
            resource = if (olderMessages) "Older messages" else resource?.label ?: "Timetable")
        mutable.update { it.copy(busy = true, notice = null,
            syncProgress = progress) }
        syncJob = viewModelScope.launch {
            try { operation() } finally {
                if (!mutable.value.signingOut) {
                    val current = mutable.value
                    val failures = (resource?.let(::listOf) ?: SyncResource.entries).mapNotNull { kind ->
                        current.issue(kind)?.let { kind to it }
                    }
                    val singleFailure = failures.singleOrNull()
                    val notice = if (failures.isEmpty()) null else SyncNotice(++noticeId,
                        if (current.needsLogin) "Sign in to update your information."
                        else if (failures.size > 1) "Couldn't update some information. Your saved data has been kept."
                        else failures.single().second,
                        singleFailure?.first?.feed, olderMessages, resource ?: singleFailure?.first)
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
    fun refresh() = startSync {
        mutable.update { it.copy(message = null, coursework = it.coursework.copy(message = null), account = it.account.copy(message = null),
            feeds = it.feeds.mapValues { (_, feed) -> feed.copy(message = null, olderPageFailed = false) }) }
        syncResource(SyncResource.TIMETABLE)
        if (mutable.value.signedIn && !mutable.value.needsLogin) {
            for (resource in SyncResource.entries.drop(1)) {
                currentCoroutineContext().ensureActive()
                if (mutable.value.needsLogin) break
                syncResource(resource)
            }
        } else mutable.update { it.copy(coursework = it.coursework.copy(message =
            if (it.needsLogin) "Sign in to update coursework." else "Couldn't update coursework. Connect and refresh to try again.")) }
    }
    // Every resource shares execution/recovery; the store still owns authentication and persistence.
    private suspend fun syncResource(resource: SyncResource, before: String? = null) {
        beginResource(if (before != null) "Older messages" else resource.label)
        mutable.update { state ->
            val cleared = state.withIssue(resource, null)
            resource.feed?.let { kind -> cleared.withFeed(kind) { it.copy(loading = true) } } ?: cleared
        }
        try {
            applyCache(withSyncRetry(onRetry = { attempt ->
                mutable.update { it.copy(syncProgress = it.syncProgress?.copy(retry = attempt)) }
            }) {
                when (resource) {
                    SyncResource.TIMETABLE -> repository.sync(::authenticated)
                    SyncResource.COURSEWORK -> repository.syncCoursework(::authenticated)
                    SyncResource.ACCOUNT -> repository.syncAccount(::authenticated)
                    else -> repository.syncFeed(requireNotNull(resource.feed), before, ::authenticated)
                }
            })
            mutable.update { state ->
                val cleared = state.withIssue(resource, null)
                if (resource == SyncResource.TIMETABLE) cleared.copy(needsLogin = false, signedIn = true) else cleared
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache()
            mutable.update { it.resourceFailed(resource, error, before != null) }
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
}
