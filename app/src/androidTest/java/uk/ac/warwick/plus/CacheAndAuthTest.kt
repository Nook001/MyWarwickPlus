package uk.ac.warwick.plus

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*

class CacheAndAuthTest {
    @Test fun repositoryClearsBothAccountsBeforeFailedCourseworkDownload() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, TimetableDatabase::class.java).build()
        try {
            val dao = db.timetable()
            dao.replace(listOf(EventEntity().apply { id = "class" }), SyncEntity().apply { userCode = "old" })
            dao.replaceCoursework(listOf(CourseworkEntity().apply { id = "deadline" }), SyncEntity().apply { id = 2; userCode = "old" })
            val api = object : StudentApi {
                override fun account(user: SignedInUser) = ""
                override fun user() = SignedInUser("new", "New student", "", "")
                override fun timetable(user: SignedInUser) = emptyList<EventEntity>()
                override fun coursework(user: SignedInUser): List<CourseworkEntity> = throw java.io.IOException()
                override fun feed(kind: FeedKind, user: SignedInUser, before: String?) = ParsedFeed(emptyList(), FeedMeta().apply { feed = kind.key })
            }
            val repo = TimetableRepository(api, dao)
            var authenticated = false
            try {
                repo.syncCoursework { user, cache ->
                    authenticated = true
                    assertEquals("new", user.code)
                    assertTrue(cache.events.isEmpty()); assertTrue(cache.coursework.isEmpty())
                    assertNull(cache.sync); assertNull(cache.courseworkSync)
                }
                fail("The network request must fail")
            } catch (_: java.io.IOException) { }
            assertTrue(authenticated)
            assertTrue(repo.cached().events.isEmpty()); assertTrue(repo.cached().coursework.isEmpty())
        } finally { db.close() }
    }
    @Test fun nativeCookiesAreRestrictedToExactHttpsOrigin() {
        assertTrue(AuthSession.isApiOrigin("https://my.warwick.ac.uk/user/info".toHttpUrl()))
        listOf("http://my.warwick.ac.uk/", "https://my.warwick.ac.uk:444/",
            "https://my.warwick.ac.uk.attacker.example/", "https://tabula.warwick.ac.uk/", "https://websignon.warwick.ac.uk/").forEach {
            assertFalse(it, AuthSession.isApiOrigin(it.toHttpUrl()))
            assertTrue(AuthSession().loadForRequest(it.toHttpUrl()).isEmpty())
        }
        assertTrue(AuthSession.isLoginUrl(Uri.parse("https://websignon.warwick.ac.uk/origin/hs")))
        assertFalse(AuthSession.isLoginUrl(Uri.parse("https://warwick.ac.uk.attacker.example/")))
        assertFalse(AuthSession.isLoginUrl(Uri.parse("http://websignon.warwick.ac.uk/")))
    }
    @Test fun cacheSurvivesReopeningAndFailedReplacementRollsBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "cache-test-${System.nanoTime()}.db"
        try {
            val initial = Room.databaseBuilder(context, TimetableDatabase::class.java, name).build()
            try {
                val db = initial
                val event = EventEntity().apply { id = "sample"; title = "Example class"; startMillis = 100; endMillis = 200 }
                db.timetable().replace(listOf(event), SyncEntity().apply { userCode = "sample-user"; syncedAt = 300 })
                try {
                    db.timetable().replace(listOf(event, event), SyncEntity())
                    fail("Duplicate primary keys should fail")
                } catch (_: android.database.sqlite.SQLiteConstraintException) { }
                assertEquals("sample-user", db.timetable().state().userCode)
            } finally { initial.close() }
            val reopened = Room.databaseBuilder(context, TimetableDatabase::class.java, name).build()
            try {
                val db = reopened
                assertEquals("Example class", db.timetable().events().single().title)
                assertEquals(300L, db.timetable().state().syncedAt)
                db.timetable().clear()
                assertNull(db.timetable().state())
                assertTrue(db.timetable().events().isEmpty())
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
