package uk.ac.warwick.plus

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.data.*

class FeedsCacheTest {
    private class Api : StudentApi {
        var code="example"
        var page=ParsedFeed(emptyList(),FeedMeta().apply { feed=3 })
        override fun service(kind: ServiceKind, user: SignedInUser) = ParsedService(emptyList(), emptyList(), ServiceMeta(kind.slot, "", ""))
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
            try { repo.syncFeed(FeedKind.MESSAGES,"old-cursor",{ _,cache -> assertTrue(cache!!.feeds.values.all { it.entries.isEmpty() }) }); fail("Old cursor must be rejected") }
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
