package uk.ac.warwick.plus

import android.app.Application
import android.net.ConnectivityManager
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.auth.SessionRefresher
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.reminders.ReminderScheduler
import uk.ac.warwick.plus.ui.AppearancePreferences
import uk.ac.warwick.plus.update.UpdateChecker
import uk.ac.warwick.plus.widget.NextClassWidget

class PlusApplication : Application() {
    fun hasNetwork(): Boolean = getSystemService(ConnectivityManager::class.java).activeNetwork != null
    val appearance by lazy {
        traceWork("MWP.Appearance.load") { AppearancePreferences(getSharedPreferences("appearance", MODE_PRIVATE)) }
    }
    private val session by lazy { traceWork("MWP.Session.create") { AuthSession() } }
    val api by lazy { traceWork("MWP.Api.create") { MyWarwickApi(session) } }
    val repository by lazy {
        traceWork("MWP.Repository.create") {
            TimetableRepository(api,
                Room.databaseBuilder(this, TimetableDatabase::class.java, "timetable.db")
                    .addMigrations(ServiceMigration)
                    .build().timetable(), refreshSession = SessionRefresher(this)::refresh,
                endSession = { api.cancelRequests(); session.clear(this) })
        }
    }
    val updates by lazy {
        UpdateChecker(getSharedPreferences("updates", MODE_PRIVATE), BuildConfig.BASE_VERSION_NAME,
            BuildConfig.AUTO_UPDATE_CHECK, "MyWarwickPlus/${BuildConfig.VERSION_NAME} (Android)")
    }
    val reminders by lazy {
        ReminderScheduler(this, getSharedPreferences("reminders", MODE_PRIVATE)) { repository.cached() }
    }

    /** While the UI runs, every cache or theme change re-plans reminders and redraws widgets. */
    @OptIn(FlowPreview::class)
    fun followCache(scope: CoroutineScope) = scope.launch(Dispatchers.Default) {
        repository.observeCache().combine(appearance.state) { cache, _ -> cache }.debounce(1_000).collect { cache ->
            reminders.reschedule(cache)
            NextClassWidget.update(this@PlusApplication, cache)
        }
    }
}
