package uk.ac.warwick.plus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import uk.ac.warwick.plus.data.*
import java.io.IOException

data class CourseworkState(
    val entries: List<CourseworkEntity> = emptyList(),
    val lastSynced: Long? = null,
    val message: String? = null
)

data class TimetableState(
    val events: List<EventEntity> = emptyList(),
    val name: String = "",
    val lastSynced: Long? = null,
    val busy: Boolean = false,
    val signedIn: Boolean = false,
    val needsLogin: Boolean = false,
    val message: String? = null,
    val coursework: CourseworkState = CourseworkState()
)

class TimetableViewModel(private val repository: TimetableStore,
    private val reportFailure: (Exception) -> Unit = {
        if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.w("MyWarwickPlus", "Student data sync failed: ${it.javaClass.simpleName}")
    }) : ViewModel() {
    private val mutable = MutableStateFlow(TimetableState(busy = true))
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try { applyCache(repository.cached()) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                mutable.update { it.copy(message = "Couldn't open your saved timetable. Try refreshing.") }
            } finally { mutable.update { it.copy(busy = false) } }
            refresh()
        }
    }
    private fun applyCache(cache: CachedTimetable) {
        mutable.update { it.copy(events = cache.events, name = cache.sync?.displayName ?: it.name,
            lastSynced = cache.sync?.syncedAt,
            coursework = it.coursework.copy(entries = cache.coursework, lastSynced = cache.courseworkSync?.syncedAt)) }
    }
    private fun authenticated(user: SignedInUser, cache: CachedTimetable) {
        applyCache(cache)
        mutable.update { it.copy(signedIn = true, needsLogin = false, name = user.name) }
    }
    private suspend fun restoreCache() {
        try { applyCache(repository.cached()) }
        catch (error: Exception) { if (error is kotlinx.coroutines.CancellationException) throw error }
    }
    fun refresh() {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, message = null, coursework = it.coursework.copy(message = null)) }
        viewModelScope.launch {
            try {
                applyCache(repository.sync(::authenticated))
                mutable.update { it.copy(needsLogin = false, signedIn = true) }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                reportFailure(error)
                restoreCache()
                mutable.update { current -> current.copy(needsLogin = error is SignInRequiredException,
                    signedIn = current.signedIn && error !is SignInRequiredException,
                    message = when (error) {
                        is SignInRequiredException -> if (current.lastSynced != null)
                            "Your session has expired. Sign in to update your saved timetable."
                            else "Sign in to load your timetable."
                        is IOException -> if (current.lastSynced != null)
                            "Couldn't connect. Showing your saved timetable."
                            else "Couldn't connect. Connect to the internet and try again."
                        is ServiceException -> "MyWarwick is unavailable (${error.status}). Try again later."
                        else -> if (current.lastSynced != null) "Couldn't read the timetable. Your saved data has been kept."
                            else "Your timetable couldn't be loaded. Try refreshing."
                    }) }
            } finally {
                try {
                    // Separate caches and failures: timetable parsing must not prevent deadlines updating.
                    if (!kotlinx.coroutines.currentCoroutineContext().isActive) Unit
                    else if (mutable.value.signedIn && !mutable.value.needsLogin) refreshCoursework()
                    else mutable.update { it.copy(coursework = it.coursework.copy(message =
                        if (it.needsLogin) "Sign in to update coursework."
                        else "Couldn't update coursework. Connect and refresh to try again.")) }
                } finally { mutable.update { it.copy(busy = false) } }
            }
        }
    }
    private suspend fun refreshCoursework() {
        try {
            applyCache(repository.syncCoursework(::authenticated))
            mutable.update { it.copy(coursework = it.coursework.copy(message = null)) }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            reportFailure(error)
            restoreCache()
            mutable.update { current -> current.copy(
                needsLogin = current.needsLogin || error is SignInRequiredException,
                signedIn = current.signedIn && error !is SignInRequiredException,
                message = if (error is SignInRequiredException) "Your session has expired. Sign in to update your saved data." else current.message,
                coursework = current.coursework.copy(message = when (error) {
                    is SignInRequiredException -> "Sign in to update coursework."
                    is IOException -> "Couldn't connect. Your saved deadlines have been kept."
                    is ServiceException -> "Coursework is unavailable (${error.status}). Your saved deadlines have been kept."
                    else -> "Couldn't read coursework. Your saved deadlines have been kept."
                })
            ) }
        }
    }
}
