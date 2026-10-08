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

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }

    @Test fun summariesKeepOriginalOrderAndRejectDuplicateIdsAndWrongTypes() {
        fun response(items: String) = """{"success":true,"data":{"bus":{"content":{"items":$items}}}}"""
        val entries = ServiceParser.parse(ServiceKind.BUSES, response("""[{"id":"a","callout":"23:59","text":"Example route"},{"id":"b","callout":"00:10","text":"Another route"}]""")).summaries
        assertEquals(listOf("23:59", "00:10"), entries.map { it.callout })
        for (items in listOf("""[{"id":"a","callout":"1","text":"x"},{"id":"a","callout":"2","text":"y"}]""",
            """[{"id":"a","callout":2,"text":"x"}]""")) {
            try { ServiceParser.parse(ServiceKind.BUSES, response(items)); fail("Invalid batch must be rejected") }
            catch (_: Exception) { }
        }
    }

    @Test fun eventsRetainOffsetAndAllDaySemanticsAndDiscardUnsafeLinks() {
        val parsed = ServiceParser.parse(ServiceKind.EVENTS, """{"success":true,"data":{"uni-events":{"content":{"items":[
            {"id":"event","source":"unknown-source","title":"Campus activity","extraInfo":"Details","href":"javascript:alert(1)","location":[{"name":"Campus"}],"start":"2026-10-08T00:00:00.000+01","end":"2026-10-10T00:00:00.000+01","isAllDay":true}]}}}}""")
        val event = parsed.events.single().snapshot()
        assertTrue(event.allDay); assertEquals("", event.url); assertEquals("Campus", event.location)
        assertEquals("8 Oct – 9 Oct", campusEventTime(event, event.startMillis))
        assertTrue(upcomingCampusEvents(listOf(event), event.endMillis).isEmpty())
    }

    private class Store : TimetableStore {
        val user = SignedInUser("test", "Test", "", "")
        val cache = MutableStateFlow(CachedTimetable(emptyList(), SyncEntity().apply { userCode = user.code; syncedAt = 1 }))
        var coreGate: CompletableDeferred<Unit>? = null
        var serviceGate: CompletableDeferred<Unit>? = null
        var serviceError: Exception? = null
        val services = mutableListOf<ServiceKind>()
        override fun observeCache() = cache
        override suspend fun cached() = cache.value
        override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) { onAuthenticated(user, null); coreGate?.await() }
        override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) { }
        override suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) { }
        override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) { }
        override suspend fun syncService(kind: ServiceKind, onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) {
            services += kind; onAuthenticated(user, null); serviceGate?.await(); serviceError?.let { throw it }
            val state = SyncEntity().apply { id = kind.slot; userCode = user.code; syncedAt = 1_000_000 }
            cache.value = cache.value.copy(services = cache.value.services +
                (kind to CachedService(emptyList(), emptyList(), ServiceMeta(kind.slot, "", ""), state)))
        }
        override suspend fun signOut() { cache.value = CachedTimetable(emptyList(), null) }
    }

    @Test fun homeRequestWaitsForCoreAndUsesDueResourcesWithoutDuplicateEntryRefresh() = runTest(dispatcher) {
        val coreGate = CompletableDeferred<Unit>()
        val serviceGate = CompletableDeferred<Unit>()
        val store = Store().apply { this.coreGate = coreGate; this.serviceGate = serviceGate }
        var now = 1_000_000L
        val model = TimetableViewModel(store, {}, nowMillis = { now })
        model.setHomeVisible(true); runCurrent()
        assertEquals(6, model.state.value.syncProgress!!.total); assertTrue(store.services.isEmpty())
        coreGate.complete(Unit); runCurrent()
        assertEquals(3, model.state.value.syncProgress!!.total)
        serviceGate.complete(Unit); advanceUntilIdle()
        assertEquals(ServiceKind.entries, store.services)
        model.setHomeVisible(false); model.setHomeVisible(true); advanceUntilIdle()
        assertEquals(3, store.services.size)
        now += 60_001
        model.setHomeVisible(false); model.setHomeVisible(true); advanceUntilIdle()
        assertEquals(ServiceKind.BUSES, store.services.last()); assertEquals(4, store.services.size)
        model.refresh(); advanceUntilIdle(); assertEquals(4, store.services.size)
        model.refreshHome(); advanceUntilIdle(); assertEquals(7, store.services.size)
        model.signOut(); advanceUntilIdle()
    }

    @Test fun failedServiceKeepsCachedDataAndAnExpiredSessionStopsTheBatch() = runTest(dispatcher) {
        val store = Store()
        val model = TimetableViewModel(store, {}, nowMillis = { 1_000_000 })
        advanceUntilIdle()
        store.cache.value = store.cache.value.copy(services = mapOf(ServiceKind.PRINT to CachedService(
            listOf(ServiceSummaryEntity(8, "balance", "Example balance", "Print", 0)), emptyList(), ServiceMeta(8, "", ""),
            SyncEntity().apply { id = 8; userCode = store.user.code; syncedAt = 1 })))
        runCurrent()
        store.serviceError = InvalidResponseException()
        model.refreshResource(SyncResource.PRINT); advanceUntilIdle()
        assertEquals("Example balance", model.state.value.service(ServiceKind.PRINT).summaries.single().callout)
        assertNotNull(model.state.value.service(ServiceKind.PRINT).message)
        store.services.clear(); store.serviceError = SignInRequiredException()
        model.setHomeVisible(true); advanceUntilIdle()
        assertEquals(listOf(ServiceKind.BUSES), store.services); assertTrue(model.state.value.needsLogin)
        model.signOut(); advanceUntilIdle(); assertFalse(model.state.value.hasSavedData)
        assertTrue(model.state.value.service(ServiceKind.PRINT).summaries.isEmpty())
    }
}
