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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CourseworkTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private fun item(id: String, date: String) = """{"id":"$id","title":"Example assignment","text":"Instructions","href":"https://tabula.warwick.ac.uk/coursework/example","date":"$date"}"""
    private fun response(vararg items: String) = """{"success":true,"data":{"coursework":{"content":{"items":[${items.joinToString(",")}]}}}}"""

    @Test fun realOffsetVariantsBecomeInstantsAndAreSorted() {
        val entries = CourseworkParser.parse(response(item("later", "2026-10-30T12:00:00.000Z"), item("first", "2026-10-15T12:00:00.000+01")))
        assertEquals("first", entries.first().id)
        assertEquals(Instant.parse("2026-10-15T11:00:00Z").toEpochMilli(), entries.first().dueMillis)
        assertEquals("Instructions", entries.first().description)
    }
    @Test fun daylightSavingOffsetPreservesDeadlineInstant() {
        val entries = CourseworkParser.parse(response(item("summer", "2026-10-25T01:30:00+01:00[Europe/London]"), item("winter", "2026-10-25T01:30:00Z")))
        assertEquals(3_600_000L, entries.last().dueMillis - entries.first().dueMillis)
    }
    @Test fun legitimateEmptyFeedIsAccepted() { assertTrue(CourseworkParser.parse(response()).isEmpty()) }
    @Test fun malformedPartialFeedIsRejectedAsAWhole() {
        listOf(response(item("ok", "2026-10-15T12:00:00Z"), item("bad", "2026-10-15T12:00:00")),
            response(item("same", "2026-10-15T12:00:00Z"), item("same", "2026-10-15T12:00:00Z")),
            """{"success":false,"data":{"coursework":{"content":{"items":[]}}}}""",
            """{"success":true,"data":{}}""").forEach {
            try { CourseworkParser.parse(it); fail("Invalid feed must not overwrite a cache") } catch (_: Exception) { }
        }
    }
    @Test fun sourceLinksAreLimitedToOfficialHttpsSites() {
        assertEquals("https://my.warwick.ac.uk/example", safeCourseworkUrl("/example"))
        assertNotNull(safeCourseworkUrl("https://tabula.warwick.ac.uk/coursework/example"))
        listOf("", "javascript:alert(1)", "http://warwick.ac.uk/", "https://warwick.ac.uk.attacker.example/",
            "https://warwick.ac.uk:444/", "https://user@warwick.ac.uk/", "//attacker.example/").forEach { assertNull(it, safeCourseworkUrl(it)) }
    }
    @Test fun deadlineLabelsUseWarwickMidnightAndDoNotInferSubmission() {
        val now = Instant.parse("2026-10-03T22:30:00Z").toEpochMilli()
        assertEquals("Due today", deadlineLabel(now + 1_000, now))
        assertEquals("Due tomorrow", deadlineLabel(now + 3_600_000, now))
        assertEquals("Deadline passed", deadlineLabel(now - 1, now))
        assertEquals("Due in 2 days", deadlineLabel(now + 2 * 86_400_000, now))
    }
    private val user = SignedInUser("student", "Example", "", "")
    private fun cache() = CachedTimetable(listOf(EventEntity().apply { id = "class" }),
        SyncEntity().apply { userCode = user.code; syncedAt = 100 },
        listOf(CourseworkEntity().apply { id = "assignment"; title = "Saved deadline" }),
        SyncEntity().apply { id = 2; userCode = user.code; syncedAt = 200 })
    private inner class Store(var data: CachedTimetable = cache()) : TimetableStore {
        var timetableError: Exception? = null
        var courseworkError: Exception? = null
        var courseworkGate: CompletableDeferred<Unit>? = null
        var courseworkCalls = 0
        var changeAccount = false
        override suspend fun cached() = data
        override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) = data
        override suspend fun signOut() { data = CachedTimetable(emptyList(), null) }
        override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            onAuthenticated(user, data)
            timetableError?.let { throw it }; return data
        }
        override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable {
            courseworkCalls++
            if (changeAccount) data = CachedTimetable(emptyList(), null)
            onAuthenticated(if (changeAccount) user.copy(code = "other", name = "Other") else user, data)
            courseworkGate?.await()
            courseworkError?.let { throw it }
            return data
        }
    }
    @Test fun courseworkFailurePreservesBothCachesAndRetryRecovers() = runTest(dispatcher) {
        val store = Store().apply { courseworkError = IOException() }
        val model = TimetableViewModel(store, {})
        advanceUntilIdle()
        assertEquals(200L, model.state.value.coursework.lastSynced)
        assertEquals("Saved deadline", model.state.value.coursework.entries.single().title)
        assertEquals(1, model.state.value.events.size)
        assertNull(model.state.value.message)
        assertNotNull(model.state.value.coursework.message)
        store.courseworkError = null
        model.refresh(); advanceUntilIdle()
        assertNull(model.state.value.coursework.message)
    }
    @Test fun timetableParsingFailureStillAllowsCourseworkSync() = runTest(dispatcher) {
        val store = Store().apply { timetableError = InvalidResponseException() }
        val model = TimetableViewModel(store, {}); advanceUntilIdle()
        assertEquals(1, store.courseworkCalls)
        assertNotNull(model.state.value.message)
        assertNull(model.state.value.coursework.message)
    }
    @Test fun secondEndpointAuthExpiryRetainsDataAndRequestsLogin() = runTest(dispatcher) {
        val model = TimetableViewModel(Store().apply { courseworkError = SignInRequiredException() }, {})
        advanceUntilIdle()
        assertTrue(model.state.value.needsLogin)
        assertFalse(model.state.value.signedIn)
        assertEquals(1, model.state.value.coursework.entries.size)
    }
    @Test fun accountChangeDuringCourseworkClearsBothScreensAndKeepsRefreshLocked() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val store = Store().apply { changeAccount = true; courseworkGate = gate; courseworkError = IOException() }
        val model = TimetableViewModel(store, {}); runCurrent()
        assertTrue(model.state.value.events.isEmpty())
        assertTrue(model.state.value.coursework.entries.isEmpty())
        assertNull(model.state.value.coursework.lastSynced)
        assertEquals("Other", model.state.value.name)
        assertTrue(model.state.value.busy)
        model.refresh(); runCurrent(); assertEquals(1, store.courseworkCalls)
        gate.complete(Unit); advanceUntilIdle(); assertFalse(model.state.value.busy)
    }
}
