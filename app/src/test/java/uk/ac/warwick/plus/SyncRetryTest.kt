package uk.ac.warwick.plus

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRetryTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private class Store : TimetableStore {
        val user = SignedInUser("example", "Example student", "", "")
        var cache = CachedTimetable(listOf(EventEntity().apply { id = "saved"; title = "Saved class" }),
            SyncEntity().apply { userCode = user.code; syncedAt = 100 }, feeds = mapOf(FeedKind.MESSAGES to
                CachedFeed(listOf(FeedEntry().apply { id = "cursor"; feed = FeedKind.MESSAGES.key }),
                    FeedMeta().apply { hasMore = true }, SyncEntity().apply { syncedAt = 100 })))
        var timetableCalls = 0
        var courseworkCalls = 0
        var timetableFailure: (Int) -> Exception? = { null }
        var courseworkFailure: Exception? = null
        var timetableGate: CompletableDeferred<Unit>? = null
        var feedGate: CompletableDeferred<Unit>? = null
        var feedFailure: (FeedKind, String?) -> Exception? = { _, _ -> null }
        val feeds = mutableListOf<Pair<FeedKind, String?>>()
        override suspend fun cached() = cache
        override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            timetableCalls++; onAuthenticated(user, cache)
            timetableGate?.await()
            timetableFailure(timetableCalls)?.let { throw it }; return cache
        }
        override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            courseworkCalls++; onAuthenticated(user, cache); courseworkFailure?.let { throw it }; return cache
        }
        override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            feeds += kind to before; onAuthenticated(user, cache); feedGate?.await()
            feedFailure(kind, before)?.let { throw it }; return cache
        }
        override suspend fun signOut() { cache = CachedTimetable(emptyList(), null) }
    }
    @Test fun transientFailureRecoversAtTwoAndFiveSecondsWithoutRepeatingSuccessfulResources() = runTest(dispatcher) {
        val store = Store().apply { timetableFailure = { if (it < 3) IOException() else null } }
        val model = TimetableViewModel(store, {})
        runCurrent(); assertEquals(1, store.timetableCalls)
        assertEquals("Saved class", model.state.value.events.single().title)
        assertNull(model.state.value.notice)
        model.refresh(); runCurrent(); assertEquals(1, store.timetableCalls)
        advanceTimeBy(1_999); runCurrent(); assertEquals(1, store.timetableCalls)
        advanceTimeBy(1); runCurrent(); assertEquals(2, store.timetableCalls)
        advanceTimeBy(4_999); runCurrent(); assertEquals(2, store.timetableCalls)
        advanceTimeBy(1); runCurrent(); assertEquals(3, store.timetableCalls)
        assertEquals(1, store.courseworkCalls); assertEquals(3, store.feeds.size)
        assertFalse(model.state.value.busy); assertNull(model.state.value.notice)
    }
    @Test fun feedExhaustsOnlyItsOwnBudgetAndEmitsOneConsumableNotice() = runTest(dispatcher) {
        val store = Store().apply { feedFailure = { kind, _ -> if (kind == FeedKind.MESSAGES) ServiceException(503) else null } }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertEquals(3, store.feeds.count { it.first == FeedKind.MESSAGES })
        assertEquals(1, store.feeds.count { it.first == FeedKind.MODULES })
        assertEquals(1, store.feeds.count { it.first == FeedKind.LIBRARY })
        assertEquals(1, store.timetableCalls); assertEquals(1, store.courseworkCalls)
        assertEquals("cursor", model.state.value.feed(FeedKind.MESSAGES).entries.single().id)
        val notice = model.state.value.notice!!
        assertEquals(FeedKind.MESSAGES, notice.feed)
        model.consumeNotice(notice.id); assertNull(model.state.value.notice)
        model.refreshFeed(FeedKind.MESSAGES); advanceUntilIdle()
        assertTrue(model.state.value.notice!!.id > notice.id)
        assertEquals(6, store.feeds.count { it.first == FeedKind.MESSAGES })
        store.feedFailure = { _, _ -> null }
        model.refreshFeed(FeedKind.MESSAGES); advanceUntilIdle()
        assertNull(model.state.value.notice)
    }
    @Test fun authenticationParsingAndPermanentHttpFailuresAreNeverRetried() = runTest(dispatcher) {
        for (error in listOf(SignInRequiredException(), InvalidResponseException(), ServiceException(400), ServiceException(429))) {
            val store = Store().apply { timetableFailure = { error } }
            val model = TimetableViewModel(store, {}); advanceUntilIdle()
            assertEquals(1, store.timetableCalls)
            assertEquals(error is SignInRequiredException, model.state.value.needsLogin)
            assertNotNull(model.state.value.notice)
            assertEquals("Saved class", model.state.value.events.single().title)
        }
    }
    @Test fun signOutCancelsTheRetryDelayAndCannotRestoreOldCache() = runTest(dispatcher) {
        val store = Store().apply { timetableFailure = { IOException() } }
        val model = TimetableViewModel(store, {}); runCurrent()
        assertEquals(1, store.timetableCalls)
        model.signOut(); advanceUntilIdle()
        assertEquals(1, store.timetableCalls)
        assertFalse(model.state.value.hasSavedData); assertFalse(model.state.value.busy)
        assertTrue(model.state.value.needsLogin); assertNull(model.state.value.notice)
    }
    @Test fun explicitOlderPageRetriesKeepTheSameCursorAndNeverAutoPaginate() = runTest(dispatcher) {
        val store = Store()
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertTrue(store.feeds.all { it.second == null })
        store.feedFailure = { _, before -> if (before != null) IOException() else null }
        model.loadMoreMessages(); advanceUntilIdle()
        assertEquals(List(3) { FeedKind.MESSAGES to "cursor" }, store.feeds.filter { it.second != null })
        assertTrue(model.state.value.notice!!.olderMessages)
        store.feedFailure = { _, _ -> null }
        model.loadMoreMessages(); advanceUntilIdle()
        assertNull(model.state.value.notice)
    }
    @Test fun repeatedFailuresStopAtThreeAttemptsAndSummarizeWithoutClearingSavedData() = runTest(dispatcher) {
        val store = Store().apply {
            timetableFailure = { IOException() }; courseworkFailure = ServiceException(502)
        }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertEquals(3, store.timetableCalls); assertEquals(3, store.courseworkCalls)
        assertEquals(3, store.feeds.size)
        assertFalse(model.state.value.busy); assertTrue(model.state.value.hasSavedData)
        assertTrue(model.state.value.notice!!.message.contains("some information"))
        advanceTimeBy(60_000); runCurrent(); assertEquals(3, store.timetableCalls)
        model.refreshFeed(FeedKind.MODULES); advanceUntilIdle()
        assertNull("A successful feed refresh must not re-announce an earlier timetable failure", model.state.value.notice)
    }
    @Test fun progressFollowsRealPhasesAndHoldsDuringRetryWithoutDoubleCounting() = runTest(dispatcher) {
        val timetable = CompletableDeferred<Unit>()
        val feeds = CompletableDeferred<Unit>()
        val store = Store().apply {
            timetableGate = timetable; feedGate = feeds
            feedFailure = { kind, _ -> if (kind == FeedKind.MESSAGES) IOException() else null }
        }
        val model = TimetableViewModel(store, {}); runCurrent()
        assertEquals(5, model.state.value.syncProgress!!.total)
        assertEquals(.1f, model.state.value.syncProgress!!.fraction, .001f)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(.1f, model.state.value.syncProgress!!.fraction, .001f)
        timetable.complete(Unit); runCurrent()
        assertEquals(.5f, model.state.value.syncProgress!!.fraction, .001f)
        feeds.complete(Unit); runCurrent()
        assertEquals(1, model.state.value.syncProgress!!.retry)
        assertEquals(.5f, model.state.value.syncProgress!!.fraction, .001f)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(2, model.state.value.syncProgress!!.retry)
        assertEquals(.5f, model.state.value.syncProgress!!.fraction, .001f)
        advanceUntilIdle()
        val progress = model.state.value.syncProgress!!
        assertEquals(5, progress.completed); assertEquals(1, progress.failures)
        assertEquals(1f, progress.fraction, .001f); assertTrue(progress.finished)
        assertNotNull(model.state.value.notice); assertTrue(model.state.value.hasSavedData)
    }
    @Test fun authStopLeavesUnstartedWorkIncompleteAndSignOutClearsSingleFeedProgress() = runTest(dispatcher) {
        val store = Store().apply { timetableFailure = { SignInRequiredException() } }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        val stopped = model.state.value.syncProgress!!
        assertEquals(1, stopped.completed); assertTrue(stopped.fraction < 1f)
        assertEquals(1, stopped.failures); assertTrue(stopped.finished)
        assertTrue(store.feeds.isEmpty())
        store.timetableFailure = { null }; model.refresh(); advanceUntilIdle()
        store.feedGate = CompletableDeferred()
        model.refreshFeed(FeedKind.MODULES); runCurrent()
        assertEquals(1, model.state.value.syncProgress!!.total)
        assertEquals(.5f, model.state.value.syncProgress!!.fraction, .001f)
        model.signOut(); advanceUntilIdle()
        assertNull(model.state.value.syncProgress)
        assertFalse(model.state.value.hasSavedData)
    }
}
