package uk.ac.warwick.plus

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*

class FeedsCacheTest {
    @Test fun versionThreeMigrationKeepsExistingDataAndIndependentFeedsSurviveCourseworkRefresh() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val name="feeds-migration-${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name,0,null).use { old ->
                old.execSQL("CREATE TABLE events (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, module TEXT NOT NULL, location TEXT NOT NULL, locationUrl TEXT NOT NULL, startMillis INTEGER NOT NULL, endMillis INTEGER NOT NULL, allDay INTEGER NOT NULL, academicWeek INTEGER NOT NULL, moduleName TEXT NOT NULL DEFAULT '')")
                old.execSQL("CREATE TABLE sync_state (id INTEGER NOT NULL PRIMARY KEY, userCode TEXT NOT NULL, displayName TEXT NOT NULL, syncedAt INTEGER NOT NULL)")
                old.execSQL("CREATE TABLE coursework (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, dueMillis INTEGER NOT NULL)")
                old.execSQL("INSERT INTO events VALUES ('saved','Example class','EX101','','',100,200,0,1,'Example module')")
                old.execSQL("INSERT INTO coursework VALUES ('assignment','Example assignment','','',400)")
                old.execSQL("INSERT INTO sync_state VALUES (1,'example','Example student',300)")
                old.execSQL("INSERT INTO sync_state VALUES (2,'example','Example student',500)")
                old.version=3
            }
            val db=Room.databaseBuilder(context,TimetableDatabase::class.java,name).addMigrations(TimetableDatabase.MIGRATION_3_4, TimetableDatabase.MIGRATION_4_5).build()
            try {
                val dao=db.timetable()
                assertEquals("Example module",dao.events().single().moduleName)
                assertEquals("Example assignment",dao.coursework().single().title)
                val entry=FeedEntry().apply { feed=3; id="saved-message"; title="Example message" }
                val meta=FeedMeta().apply { feed=3 }
                val state=SyncEntity().apply { id=3; userCode="example"; syncedAt=600 }
                dao.replaceFeed(listOf(entry),meta,state)
                dao.replaceCoursework(dao.coursework(),dao.courseworkState())
                assertEquals(600L,dao.feedState(3).syncedAt)
                try { dao.replaceFeed(listOf(entry,entry),meta,state); fail("Duplicates must roll back") }
                catch (_: android.database.sqlite.SQLiteConstraintException) { }
                assertEquals("Example message",dao.feedEntries(3).single().title)
            } finally { db.close() }
            val reopened=Room.databaseBuilder(context,TimetableDatabase::class.java,name).build()
            try { assertEquals("Example message",reopened.timetable().feedEntries(3).single().title) } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
    private class Api : StudentApi {
        var code="example"
        var page=ParsedFeed(emptyList(),FeedMeta().apply { feed=3 })
        override fun account(user: SignedInUser) = ""
        override fun user()=SignedInUser(code,"Example student","","")
        override fun timetable(user: SignedInUser)=emptyList<EventEntity>()
        override fun coursework(user: SignedInUser)=emptyList<CourseworkEntity>()
        override fun feed(kind: FeedKind,user: SignedInUser,before: String?)=page
    }
    @Test fun paginationDeduplicatesRejectsNonProgressingPageAndLogoutClearsAllFeeds()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,TimetableDatabase::class.java).build()
        try {
            fun entry(id: String,date: Long)=FeedEntry().apply { feed=3; this.id=id; dateMillis=date }
            val api=Api(); var cleared=false
            val repo=TimetableRepository(api,db.timetable(),{ cleared=true })
            api.page=ParsedFeed(listOf(entry("new",300),entry("middle",200)),FeedMeta().apply { feed=3; hasMore=true })
            repo.syncFeed(FeedKind.MESSAGES,null,{ _,_ -> })
            api.page=ParsedFeed(listOf(entry("middle",200),entry("old",100)),FeedMeta().apply { feed=3 })
            repo.syncFeed(FeedKind.MESSAGES,"middle",{ _,_ -> })
            assertEquals(listOf("new","middle","old"),repo.cached().feeds[FeedKind.MESSAGES]!!.entries.map { it.id })
            try { repo.syncFeed(FeedKind.MESSAGES,"old",{ _,_ -> }); fail("Repeated page must be rejected") } catch (_: InvalidResponseException) { }
            assertEquals(3,repo.cached().feeds[FeedKind.MESSAGES]!!.entries.size)
            repo.signOut(); assertTrue(cleared)
            assertTrue(db.timetable().states().isEmpty()); assertTrue(db.timetable().feedEntries(3).isEmpty())
        } finally { db.close() }
    }
    @Test fun aPreviousAccountsCursorCannotBeUsedAfterAccountChange()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,TimetableDatabase::class.java).build()
        try {
            val api=Api()
            val repo=TimetableRepository(api,db.timetable())
            api.page=ParsedFeed(listOf(FeedEntry().apply { feed=3; id="old-cursor" }),FeedMeta().apply { feed=3; hasMore=true })
            repo.syncFeed(FeedKind.MESSAGES,null,{ _,_ -> })
            api.code="other"
            try { repo.syncFeed(FeedKind.MESSAGES,"old-cursor",{ _,cache -> assertTrue(cache.feeds.values.all { it.entries.isEmpty() }) }); fail("Old cursor must be rejected") }
            catch (_: IllegalArgumentException) { }
            assertTrue(db.timetable().states().isEmpty())
        } finally { db.close() }
    }
    @Test fun localSignOutRemovesSyntheticWebViewCookies()=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val manager=android.webkit.CookieManager.getInstance()
        org.junit.Assume.assumeTrue("Cookie-clearing fixture runs only on an emulator with no existing MyWarwick cookie",
            android.os.Build.HARDWARE in setOf("ranchu","goldfish") && manager.getCookie("https://my.warwick.ac.uk/").isNullOrEmpty())
        manager.setCookie("https://my.warwick.ac.uk/","test-session=synthetic; Secure; Path=/")
        manager.flush()
        assertTrue(manager.getCookie("https://my.warwick.ac.uk/").orEmpty().contains("test-session"))
        AuthSession(manager).clear(context)
        assertTrue(manager.getCookie("https://my.warwick.ac.uk/").isNullOrEmpty())
    }
}
