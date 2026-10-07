package uk.ac.warwick.plus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FeedStateTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private val user = SignedInUser("example", "Example student", "", "")
    private inner class Store : TimetableStore {
        private val snapshots = MutableStateFlow(CachedTimetable(emptyList(), SyncEntity().apply { userCode = user.code; displayName = user.name; syncedAt = 100 },
            feeds = FeedKind.entries.associateWith { kind -> CachedFeed(listOf(FeedEntry().apply { feed = kind.key; id = "saved"; title = "Saved ${kind.name}" }),
                FeedMeta().apply { feed = kind.key; hasMore = kind == FeedKind.MESSAGES }, SyncEntity().apply { id = kind.key; userCode = user.code; syncedAt = 200 }) }))
        var cache: CachedTimetable
            get() = snapshots.value
            set(value) { snapshots.value = value }
        override fun observeCache() = snapshots
        var errors = mutableMapOf<FeedKind, Exception>()
        var gate: CompletableDeferred<Unit>? = null
        var logoutError = false
        var signedOut = false
        val calls = mutableListOf<Pair<FeedKind,String?>>()
        override suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) = cache
        override suspend fun cached() = cache
        override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            onAuthenticated(user,cache); gate?.await(); return cache
        }
        override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) = cache
        override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            calls.add(kind to before); onAuthenticated(user,cache); errors[kind]?.let { throw it }; return cache
        }
        override suspend fun signOut() {
            if (logoutError) throw IOException()
            signedOut = true; cache = CachedTimetable(emptyList(),null)
        }
    }
    @Test fun oneFeedFailureRetainsCacheAndOtherFeedsStillRefresh() = runTest(dispatcher) {
        val store = Store().apply { errors[FeedKind.MESSAGES] = ServiceException(503) }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertEquals(5,store.calls.size)
        assertEquals(1,model.state.value.feed(FeedKind.MESSAGES).entries.size)
        assertNotNull(model.state.value.feed(FeedKind.MESSAGES).message)
        assertNull(model.state.value.feed(FeedKind.MODULES).message)
        store.errors.clear(); model.refreshFeed(FeedKind.MESSAGES); advanceUntilIdle()
        assertNull(model.state.value.feed(FeedKind.MESSAGES).message)
    }
    @Test fun feedAuthExpiryStopsLaterRequestsButKeepsSavedData() = runTest(dispatcher) {
        val store = Store().apply { errors[FeedKind.MESSAGES] = SignInRequiredException() }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertEquals(1,store.calls.size)
        assertTrue(model.state.value.needsLogin)
        assertTrue(model.state.value.hasSavedData)
        assertEquals(1,model.state.value.feed(FeedKind.MODULES).entries.size)
    }
    @Test fun signOutCancelsRefreshAndPreventsLateDataRestoration() = runTest(dispatcher) {
        val store = Store().apply { gate = CompletableDeferred() }
        val model = TimetableViewModel(store, {}); runCurrent()
        assertTrue(model.state.value.busy)
        model.signOut(); advanceUntilIdle()
        assertTrue(store.signedOut)
        assertTrue(model.state.value.needsLogin)
        assertFalse(model.state.value.hasSavedData)
        assertEquals("",model.state.value.name)
        store.gate!!.complete(Unit); advanceUntilIdle()
        assertFalse(model.state.value.hasSavedData)
    }
    @Test fun failedLogoutBlocksRefreshUntilRetried() = runTest(dispatcher) {
        val store = Store()
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        store.logoutError = true; model.signOut(); advanceUntilIdle()
        assertTrue(model.state.value.logoutFailed)
        val before = store.calls.size
        model.refresh(); advanceUntilIdle(); assertEquals(before,store.calls.size)
        store.logoutError = false; model.signOut(); advanceUntilIdle()
        assertFalse(model.state.value.logoutFailed); assertTrue(store.signedOut)
    }
    @Test fun olderPageOnlyLoadsOnExplicitRequestWithTheSavedCursor() = runTest(dispatcher) {
        val store = Store()
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertTrue(store.calls.all { it.second == null })
        model.loadMoreMessages(); advanceUntilIdle()
        assertEquals(FeedKind.MESSAGES to "saved",store.calls.last())
    }
}
