package uk.ac.warwick.plus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.data.*
import java.io.IOException

data class TimetableState(
    val events: List<EventEntity> = emptyList(),
    val name: String = "",
    val lastSynced: Long? = null,
    val busy: Boolean = false,
    val signedIn: Boolean = false,
    val needsLogin: Boolean = false,
    val message: String? = null
)

class TimetableViewModel(private val repository: TimetableStore,
    private val reportFailure: (Exception) -> Unit = {
        if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.w("MyWarwickPlus", "Timetable sync failed: ${it.javaClass.simpleName}")
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
            lastSynced = cache.sync?.syncedAt) }
    }
    fun refresh() {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                applyCache(repository.sync { user, cache ->
                    // Clear the previous account's UI before fetching a new account's timetable.
                    applyCache(cache)
                    mutable.update { it.copy(signedIn = true, needsLogin = false, name = user.name) }
                })
                mutable.update { it.copy(needsLogin = false, signedIn = true) }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                reportFailure(error)
                runCatching { repository.cached() }.onSuccess { applyCache(it) }.onFailure {
                    if (it is kotlinx.coroutines.CancellationException) throw it
                }
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
                mutable.update { it.copy(busy = false) }
            }
        }
    }
}
