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
    val description: String = "", val url: String = "", val webReadMillis: Long = 0)
data class TimetableState(
    val events: List<EventEntity> = emptyList(), val name: String = "", val lastSynced: Long? = null,
    val busy: Boolean = false, val signedIn: Boolean = false, val needsLogin: Boolean = false,
    val message: String? = null, val coursework: CourseworkState = CourseworkState(),
    val feeds: Map<FeedKind, FeedState> = emptyMap(), val accountCode: String = "",
    val sessionCheckedAt: Long? = null, val signingOut: Boolean = false, val logoutFailed: Boolean = false
) {
    fun feed(kind: FeedKind) = feeds[kind] ?: FeedState()
    val hasSavedData get() = lastSynced != null || coursework.lastSynced != null || feeds.values.any { it.lastSynced != null }
}

class TimetableViewModel(private val repository: TimetableStore,
    private val reportFailure: (Exception) -> Unit = {
        if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.w("MyWarwickPlus", "Student data sync failed: ${it.javaClass.simpleName}")
    }) : ViewModel() {
    private val mutable = MutableStateFlow(TimetableState(busy = true))
    val state = mutable.asStateFlow()
    private var syncJob: Job? = null
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
            accountCode = user.code, sessionCheckedAt = System.currentTimeMillis()) }
    }
    private suspend fun restoreCache() {
        try { applyCache(repository.cached()) }
        catch (error: Exception) { if (error is CancellationException) throw error }
    }
    private fun startSync(operation: suspend () -> Unit) {
        if (mutable.value.busy || mutable.value.signingOut || mutable.value.logoutFailed) return
        mutable.update { it.copy(busy = true) }
        syncJob = viewModelScope.launch {
            try { operation() } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun refresh() = startSync {
        mutable.update { it.copy(message = null, coursework = it.coursework.copy(message = null)) }
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
        try {
            applyCache(repository.sync(::authenticated))
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
        }
    }
    private fun sessionFailure(error: Exception) {
        if (error is SignInRequiredException) mutable.update { it.copy(needsLogin = true, signedIn = false,
            message = "Your session has expired. Sign in to update your saved data.") }
    }
    private suspend fun refreshCoursework() {
        try {
            applyCache(repository.syncCoursework(::authenticated))
            mutable.update { it.copy(coursework = it.coursework.copy(message = null)) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache(); sessionFailure(error)
            mutable.update { it.copy(coursework = it.coursework.copy(message = failureMessage("coursework", error))) }
        }
    }
    private fun updateFeed(kind: FeedKind, transform: (FeedState) -> FeedState) = mutable.update {
        it.copy(feeds = it.feeds + (kind to transform(it.feed(kind))))
    }
    private suspend fun syncFeed(kind: FeedKind, before: String? = null) {
        updateFeed(kind) { it.copy(loading = true, message = null) }
        try { applyCache(repository.syncFeed(kind, before, ::authenticated)) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            reportFailure(error); restoreCache(); sessionFailure(error)
            updateFeed(kind) { it.copy(message = failureMessage(kind.label.lowercase(), error)) }
        } finally { updateFeed(kind) { it.copy(loading = false) } }
    }
    fun refreshFeed(kind: FeedKind) = startSync { syncFeed(kind) }
    fun loadMoreMessages() {
        val feed = mutable.value.feed(FeedKind.MESSAGES)
        if (!feed.hasMore || mutable.value.needsLogin) return
        val cursor = feed.entries.lastOrNull()?.id ?: return
        startSync { syncFeed(FeedKind.MESSAGES, cursor) }
    }
    fun signOut() {
        if (mutable.value.signingOut) return
        val previous = syncJob
        mutable.update { it.copy(signingOut = true, busy = true) }
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
