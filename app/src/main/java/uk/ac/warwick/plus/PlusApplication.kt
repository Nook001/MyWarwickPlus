package uk.ac.warwick.plus

import android.app.Application
import android.webkit.CookieManager
import android.net.ConnectivityManager
import androidx.room.Room
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.AppearancePreferences

class PlusApplication : Application() {
    fun hasNetwork(): Boolean = getSystemService(ConnectivityManager::class.java).activeNetwork != null
    val appearance by lazy { AppearancePreferences(getSharedPreferences("appearance", MODE_PRIVATE)) }
    private val session by lazy { AuthSession() }
    val api by lazy { MyWarwickApi(session) }
    val repository by lazy {
        TimetableRepository(api,
            Room.databaseBuilder(this, TimetableDatabase::class.java, "timetable.db")
                .build().timetable(), endSession = { api.cancelRequests(); session.clear(this) })
    }
    override fun onCreate() {
        super.onCreate()
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
