package uk.ac.warwick.plus

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.TimetableViewModel
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableStateTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private val user = SignedInUser("test-student", "Example student", "", "")
    private fun snapshot(events: List<EventEntity> = listOf(EventEntity().apply { id = "sample"; title = "Example class" })) =
        CachedTimetable(events, SyncEntity().apply { userCode = user.code; displayName = user.name; syncedAt = 100L })

    private class Store(var cache: CachedTimetable) : TimetableStore {
        var operation: suspend ((SignedInUser, CachedTimetable) -> Unit) -> CachedTimetable = { cache }
        var calls = 0
        override suspend fun cached() = cache
        override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            calls++; return operation(onAuthenticated)
        }
    }

    @Test fun coldStartShowsCacheBeforeNetworkCompletesAndPreventsDuplicateRefresh() = runTest(dispatcher) {
        val store = Store(snapshot())
        val gate = CompletableDeferred<Unit>()
        store.operation = { gate.await(); store.cache }
        val model = TimetableViewModel(store, {})
        runCurrent()
        assertEquals("Example class", model.state.value.events.single().title)
        assertTrue(model.state.value.busy)
        model.refresh(); runCurrent()
        assertEquals(1, store.calls)
        gate.complete(Unit); advanceUntilIdle()
        assertFalse(model.state.value.busy)
    }

    @Test fun offlineRefreshPreservesSavedTimetableAndRecovers() = runTest(dispatcher) {
        val store = Store(snapshot())
        store.operation = { throw IOException() }
        val model = TimetableViewModel(store, {})
        advanceUntilIdle()
        assertEquals(100L, model.state.value.lastSynced)
        assertEquals(1, model.state.value.events.size)
        assertFalse(model.state.value.needsLogin)
        assertTrue(model.state.value.message!!.contains("Showing your saved timetable"))
        store.operation = { callback -> callback(user, store.cache); store.cache }
        model.refresh(); advanceUntilIdle()
        assertTrue(model.state.value.signedIn)
        assertNull(model.state.value.message)
    }

    @Test fun expiredSessionRetainsCacheAndLoginRefreshClearsThePrompt() = runTest(dispatcher) {
        val store = Store(snapshot())
        store.operation = { throw SignInRequiredException() }
        val model = TimetableViewModel(store, {})
        advanceUntilIdle()
        assertTrue(model.state.value.needsLogin)
        assertFalse(model.state.value.signedIn)
        assertEquals(1, model.state.value.events.size)
        store.operation = { callback -> callback(user, store.cache); store.cache }
        model.refresh(); advanceUntilIdle()
        assertFalse(model.state.value.needsLogin)
        assertNull(model.state.value.message)
    }

    @Test fun authenticatedInvalidResponseDoesNotLookSignedOut() = runTest(dispatcher) {
        val store = Store(CachedTimetable(emptyList(), null))
        store.operation = { callback -> callback(user, store.cache); throw InvalidResponseException() }
        val model = TimetableViewModel(store, {})
        advanceUntilIdle()
        assertTrue(model.state.value.signedIn)
        assertFalse(model.state.value.needsLogin)
        assertNull(model.state.value.lastSynced)
        assertNotNull(model.state.value.message)
    }

    @Test fun legitimateEmptyTimetableRemainsLoadedWhenRefreshFails() = runTest(dispatcher) {
        val store = Store(snapshot(emptyList()))
        store.operation = { throw IOException() }
        val model = TimetableViewModel(store, {})
        advanceUntilIdle()
        assertTrue(model.state.value.events.isEmpty())
        assertEquals(100L, model.state.value.lastSynced)
    }

    @Test fun accountChangeRemovesPreviousAccountBeforeNewDownloadCompletes() = runTest(dispatcher) {
        val store = Store(snapshot())
        val gate = CompletableDeferred<Unit>()
        store.operation = { callback ->
            store.cache = CachedTimetable(emptyList(), null)
            callback(SignedInUser("other-student", "Other student", "", ""), store.cache)
            gate.await(); throw IOException()
        }
        val model = TimetableViewModel(store, {})
        runCurrent()
        assertEquals("Other student", model.state.value.name)
        assertTrue(model.state.value.events.isEmpty())
        assertNull(model.state.value.lastSynced)
        gate.complete(Unit); advanceUntilIdle()
        assertTrue(model.state.value.events.isEmpty())
    }
}
