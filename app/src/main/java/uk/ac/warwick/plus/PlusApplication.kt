package uk.ac.warwick.plus

import android.app.Application
import android.webkit.CookieManager
import androidx.room.Room
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*

class PlusApplication : Application() {
    val api by lazy { MyWarwickApi(AuthSession()) }
    val repository by lazy {
        TimetableRepository(api,
            Room.databaseBuilder(this, TimetableDatabase::class.java, "timetable.db")
                .addMigrations(TimetableDatabase.MIGRATION_1_2, TimetableDatabase.MIGRATION_2_3).build().timetable())
    }
    override fun onCreate() {
        super.onCreate()
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
