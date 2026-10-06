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
    @Test fun versionTwoUpgradePreservesTimetableAndCreatesIndependentCourseworkCache() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "coursework-migration-${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                old.execSQL("CREATE TABLE events (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, module TEXT NOT NULL, location TEXT NOT NULL, locationUrl TEXT NOT NULL, startMillis INTEGER NOT NULL, endMillis INTEGER NOT NULL, allDay INTEGER NOT NULL, academicWeek INTEGER NOT NULL, moduleName TEXT NOT NULL DEFAULT '')")
                old.execSQL("CREATE TABLE sync_state (id INTEGER NOT NULL PRIMARY KEY, userCode TEXT NOT NULL, displayName TEXT NOT NULL, syncedAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO events VALUES ('saved', 'Example class', 'EX101', '', '', 100, 200, 0, 1, 'Example module')")
                old.execSQL("INSERT INTO sync_state VALUES (1, 'example-user', 'Example student', 300)")
                old.version = 2
            }
            val db = Room.databaseBuilder(context, TimetableDatabase::class.java, name)
                .addMigrations(TimetableDatabase.MIGRATION_2_3, TimetableDatabase.MIGRATION_3_4, TimetableDatabase.MIGRATION_4_5).build()
            try {
                val dao = db.timetable()
                assertEquals("Example module", dao.events().single().moduleName)
                assertTrue(dao.coursework().isEmpty()); assertNull(dao.courseworkState())
                val entry = CourseworkEntity().apply { id = "assignment"; title = "Example deadline"; dueMillis = 400 }
                dao.replaceCoursework(listOf(entry), SyncEntity().apply { id = 2; userCode = "example-user"; syncedAt = 500 })
                dao.replace(dao.events(), dao.state())
                assertEquals(500L, dao.courseworkState().syncedAt)
                try {
                    dao.replaceCoursework(listOf(entry, entry), SyncEntity().apply { id = 2 })
                    fail("Duplicate deadline IDs must roll back")
                } catch (_: android.database.sqlite.SQLiteConstraintException) { }
                assertEquals("Example deadline", dao.coursework().single().title)
                assertEquals(500L, dao.courseworkState().syncedAt)
                assertEquals(300L, dao.state().syncedAt)
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }

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
    @Test fun upgradeAddsModuleNameWithoutLosingSavedTimetable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                // Version 1 schema, preserved in app/schemas for comparison.
                old.execSQL("CREATE TABLE events (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, module TEXT NOT NULL, location TEXT NOT NULL, locationUrl TEXT NOT NULL, startMillis INTEGER NOT NULL, endMillis INTEGER NOT NULL, allDay INTEGER NOT NULL, academicWeek INTEGER NOT NULL)")
                old.execSQL("CREATE TABLE sync_state (id INTEGER NOT NULL PRIMARY KEY, userCode TEXT NOT NULL, displayName TEXT NOT NULL, syncedAt INTEGER NOT NULL)")
                old.execSQL("INSERT INTO events VALUES ('saved', 'Example class', 'EX101', 'Example room', '', 100, 200, 0, 1)")
                old.execSQL("INSERT INTO sync_state VALUES (1, 'example-user', 'Example student', 300)")
                old.version = 1
            }
            val upgraded = Room.databaseBuilder(context, TimetableDatabase::class.java, name)
                .addMigrations(TimetableDatabase.MIGRATION_1_2, TimetableDatabase.MIGRATION_2_3, TimetableDatabase.MIGRATION_3_4, TimetableDatabase.MIGRATION_4_5).build()
            try {
                val saved = upgraded.timetable().events().single()
                assertEquals("Example class", saved.title)
                assertEquals("", saved.moduleName)
                assertEquals("example-user", upgraded.timetable().state().userCode)
                saved.moduleName = "Example module name"
                upgraded.timetable().replace(listOf(saved), upgraded.timetable().state())
                assertEquals("Example module name", upgraded.timetable().events().single().moduleName)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
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
