package uk.ac.warwick.plus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RepositorySyncTest {
    // In-memory storage fixture: these checks cover orchestration, not Room's transaction engine.
    private class Dao : StudentCache {
        var cache = CachedTimetable(emptyList(), null)
        var writes = 0
        var reads = 0
        override fun changes() = flowOf(Unit)
        override fun snapshot() = cache.also { reads++ }
        override fun states() = listOfNotNull(cache.sync, cache.courseworkSync, cache.accountSync) +
            cache.feeds.values.mapNotNull { it.sync } + cache.services.values.mapNotNull { it.sync }
        override fun feedEntries(feed: Int) = cache.feeds[FeedKind.entries.first { it.key == feed }]?.entries.orEmpty()
        override fun replaceCoursework(entries: List<CourseworkEntity>, state: SyncEntity) { writes++; cache = cache.copy(coursework = entries, courseworkSync = state) }
        override fun replaceAccount(state: SyncEntity) { writes++; cache = cache.copy(accountSync = state) }
        override fun clear() { cache = CachedTimetable(emptyList(), null) }
        override fun replace(events: List<EventEntity>, state: SyncEntity) { writes++; cache = cache.copy(events = events, sync = state) }
        override fun replaceService(parsed: ParsedService, state: SyncEntity) {
            writes++
            cache = cache.copy(services = cache.services + (ServiceKind.entries.first { it.slot == state.id } to CachedService(parsed.summaries, parsed.events, parsed.meta, state)))
        }
        override fun replaceFeed(entries: List<FeedEntry>, meta: FeedMeta, state: SyncEntity) {
            writes++
            cache = cache.copy(feeds = cache.feeds + (FeedKind.entries.first { it.key == meta.feed } to CachedFeed(entries, meta, state)))
        }
    }
    private open class Api : StudentApi {
        override fun user() = SignedInUser("new-user", "New student", "", "")
        override fun timetable(user: SignedInUser) = emptyList<EventEntity>()
        override fun coursework(user: SignedInUser) = emptyList<CourseworkEntity>()
        override fun service(kind: ServiceKind, user: SignedInUser) = ParsedService(emptyList(), emptyList(), ServiceMeta(kind.slot, "", ""))
        override fun account(user: SignedInUser) = ""
        override fun feed(kind: FeedKind, user: SignedInUser, before: String?) = ParsedFeed(emptyList(), FeedMeta())
    }

    @Test fun serviceOnlyCacheIsClearedBeforeAnotherAccountDownloads() = runBlocking {
        val dao = Dao().apply {
            cache = cache.copy(services = mapOf(ServiceKind.PRINT to CachedService(
                listOf(ServiceSummaryEntity(8, "balance", "Old balance", "Print", 0)), emptyList(), null,
                SyncEntity().apply { id = 8; userCode = "old-user" })))
        }
        val api = object : Api() {
            override fun service(kind: ServiceKind, user: SignedInUser): ParsedService = throw InvalidResponseException()
        }
        val repo = TimetableRepository(api, dao, logSync = {})
        try {
            repo.syncService(ServiceKind.PRINT) { user, cleared ->
                assertEquals("new-user", user.code); assertTrue(cleared!!.services.isEmpty())
            }
            fail("Invalid response must fail")
        } catch (_: InvalidResponseException) { }
        assertTrue(repo.cached().services.isEmpty())
    }

    @Test fun sameAccountWritesDoNotReadSnapshotsButChangedAccountClearsBeforeFailure() = runBlocking {
        val dao = Dao()
        var owner = "first"
        var failDownload = false
        val api = object : Api() {
            override fun user() = SignedInUser(owner, "Student", "", "")
            override fun timetable(user: SignedInUser): List<EventEntity> {
                if (failDownload) throw java.io.IOException()
                return listOf(EventEntity().apply { id = user.code })
            }
        }
        val repo = TimetableRepository(api, dao, logSync = {})
        repo.cached()
        repo.sync { _, cleared -> assertNull(cleared) }
        repo.sync { _, cleared -> assertNull(cleared) }
        assertEquals(1, dao.reads) // Writes are commands; only observation/explicit reads fetch snapshots.
        assertEquals("first", repo.cached().events.single().id)
        owner = "second"
        failDownload = true
        try {
            repo.sync { _, cache -> assertTrue(cache!!.events.isEmpty()) }
            fail("Download must fail")
        } catch (_: java.io.IOException) { }
        assertTrue(repo.cached().events.isEmpty())
        assertNull(dao.cache.sync)
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
                assertNotNull(cache)
                assertTrue(cache!!.events.isEmpty()); assertNull(cache.accountSync); assertTrue(cache.feeds.isEmpty())
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
