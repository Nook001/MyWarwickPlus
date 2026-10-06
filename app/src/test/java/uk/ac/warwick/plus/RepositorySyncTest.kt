package uk.ac.warwick.plus

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RepositorySyncTest {
    // In-memory storage fixture: these checks cover orchestration, not Room's transaction engine.
    private class Dao : TimetableDao() {
        var cache = CachedTimetable(emptyList(), null)
        var writes = 0
        override fun snapshot() = cache
        override fun states() = listOfNotNull(cache.sync, cache.courseworkSync, cache.accountSync) + cache.feeds.values.mapNotNull { it.sync }
        override fun events() = cache.events
        override fun state() = cache.sync
        override fun coursework() = cache.coursework
        override fun courseworkState() = cache.courseworkSync
        override fun feedEntries(feed: Int) = cache.feeds[FeedKind.entries.first { it.key == feed }]?.entries.orEmpty()
        override fun feedMeta(feed: Int) = cache.feeds[FeedKind.entries.first { it.key == feed }]?.meta
        override fun feedState(feed: Int) = if (feed == 6) cache.accountSync else cache.feeds[FeedKind.entries.first { it.key == feed }]?.sync
        override fun clear() { cache = CachedTimetable(emptyList(), null) }
        override fun replace(events: List<EventEntity>, state: SyncEntity) { writes++; cache = cache.copy(events = events, sync = state) }
        override fun replaceFeed(entries: List<FeedEntry>, meta: FeedMeta, state: SyncEntity) {
            writes++
            cache = cache.copy(feeds = cache.feeds + (FeedKind.entries.first { it.key == meta.feed } to CachedFeed(entries, meta, state)))
        }
        override fun deleteCoursework(): Unit = error("Unused")
        override fun deleteCourseworkState(): Unit = error("Unused")
        override fun deleteEvents(): Unit = error("Unused")
        override fun deleteState(): Unit = error("Unused")
        override fun insertEvents(events: List<EventEntity>): Unit = error("Unused")
        override fun insertState(state: SyncEntity): Unit = error("Unused")
        override fun insertCoursework(entries: List<CourseworkEntity>): Unit = error("Unused")
        override fun deleteFeedEntries(feed: Int): Unit = error("Unused")
        override fun deleteFeedMeta(feed: Int): Unit = error("Unused")
        override fun deleteFeedState(feed: Int): Unit = error("Unused")
        override fun deleteFeeds(): Unit = error("Unused")
        override fun deleteFeedMetas(): Unit = error("Unused")
        override fun deleteFeedStates(): Unit = error("Unused")
        override fun insertFeedEntries(entries: List<FeedEntry>): Unit = error("Unused")
        override fun insertFeedMeta(meta: FeedMeta): Unit = error("Unused")
    }
    private open class Api : StudentApi {
        override fun user() = SignedInUser("new-user", "New student", "", "")
        override fun timetable(user: SignedInUser) = emptyList<EventEntity>()
        override fun coursework(user: SignedInUser) = emptyList<CourseworkEntity>()
        override fun account(user: SignedInUser) = ""
        override fun feed(kind: FeedKind, user: SignedInUser, before: String?) = ParsedFeed(emptyList(), FeedMeta())
    }

    @Test fun changedAccountClearsEveryCacheAndRejectsItsOldCursorBeforeDownloading() = runBlocking {
        val savedState = SyncEntity().apply { userCode = "old-user"; syncedAt = 100 }
        val dao = Dao().apply {
            cache = CachedTimetable(listOf(EventEntity().apply { id = "old-class" }), savedState,
                accountSync = SyncEntity().apply { id = 6; userCode = "old-user"; email = "old@example.invalid" },
                feeds = mapOf(FeedKind.MESSAGES to CachedFeed(listOf(FeedEntry().apply { id = "old-cursor" }), null, savedState)))
        }
        var downloaded = false
        val api = object : Api() {
            override fun feed(kind: FeedKind, user: SignedInUser, before: String?): ParsedFeed {
                downloaded = true; return super.feed(kind, user, before)
            }
        }
        val repo = TimetableRepository(api, dao)
        var authenticated = false
        try {
            repo.syncFeed(FeedKind.MESSAGES, "old-cursor") { user, cache ->
                authenticated = true
                assertEquals("new-user", user.code)
                assertTrue(cache.events.isEmpty()); assertNull(cache.accountSync); assertTrue(cache.feeds.isEmpty())
            }
            fail("Previous account's cursor must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertTrue(authenticated); assertFalse(downloaded); assertEquals(0, dao.writes)
    }

    @Test fun cancelledBlockingDownloadCannotWriteItsResultAndSignOutStillClearsCache() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val dao = Dao().apply {
            cache = CachedTimetable(listOf(EventEntity().apply { id = "saved" }),
                SyncEntity().apply { userCode = "new-user"; syncedAt = 100 })
        }
        val api = object : Api() {
            override fun timetable(user: SignedInUser): List<EventEntity> {
                started.complete(Unit)
                check(release.await(5, TimeUnit.SECONDS))
                return listOf(EventEntity().apply { id = "late" })
            }
        }
        var endedSession = false
        val repo = TimetableRepository(api, dao) { endedSession = true }
        val job = launch { repo.sync { _, _ -> } }
        try {
            withTimeout(5_000) { started.await() }
            job.cancel(); release.countDown()
            withTimeout(5_000) { job.join() }
            assertEquals(0, dao.writes); assertEquals("saved", repo.cached().events.single().id)
            repo.signOut()
            assertTrue(endedSession); assertTrue(repo.cached().events.isEmpty())
        } finally { release.countDown(); job.cancelAndJoin() }
    }
}
