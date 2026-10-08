package uk.ac.warwick.plus

import android.app.Application
import android.net.ConnectivityManager
import androidx.room.Room
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.AppearancePreferences

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
                    .build().timetable(), endSession = { api.cancelRequests(); session.clear(this) })
        }
    }
}
