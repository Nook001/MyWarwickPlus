package uk.ac.warwick.plus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CachedTimetable(val events: List<EventEntity>, val sync: SyncEntity?,
    val coursework: List<CourseworkEntity> = emptyList(), val courseworkSync: SyncEntity? = null,
    val feeds: Map<FeedKind, CachedFeed> = emptyMap(), val accountSync: SyncEntity? = null)

interface TimetableStore {
    suspend fun cached(): CachedTimetable
    suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
    suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
    suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
    suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
    suspend fun signOut()
}

interface StudentApi {
    fun user(): SignedInUser
    fun timetable(user: SignedInUser): List<EventEntity>
    fun coursework(user: SignedInUser): List<CourseworkEntity>
    fun account(user: SignedInUser): String
    fun feed(kind: FeedKind, user: SignedInUser, before: String?): ParsedFeed
}

class TimetableRepository(private val api: StudentApi, private val dao: TimetableDao,
    private val endSession: suspend () -> Unit = {}) : TimetableStore {
    private val mutex = Mutex()

    // Keep network work outside Room transactions, but serialize it with cache reads and sign-out.
    private suspend fun <T> inStore(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            operation()
        }
    }

    private suspend fun authenticate(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): SignedInUser {
        val user = api.user()
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (dao.states().any { it.userCode != user.code }) dao.clear()
        onAuthenticated(user, dao.snapshot())
        return user
    }

    private suspend fun <T> authenticatedSync(id: Int, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit,
        read: (SignedInUser) -> T, save: (T, SyncEntity) -> Unit): CachedTimetable = inStore {
        val user = authenticate(onAuthenticated)
        val result = read(user)
        // A blocking HTTP read can finish after cancellation; never persist that result.
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        val state = SyncEntity().apply {
            this.id = id; userCode = user.code; displayName = user.name; syncedAt = System.currentTimeMillis()
        }
        save(result, state)
        dao.snapshot()
    }

    override suspend fun cached(): CachedTimetable = inStore { dao.snapshot() }

    override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) =
        authenticatedSync(1, onAuthenticated, api::timetable) { events, state ->
            dao.replace(events, state)
            if (uk.ac.warwick.plus.BuildConfig.DEBUG)
                android.util.Log.i("MyWarwickPlus", "Native timetable sync succeeded; events=${events.size}")
        }

    override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) =
        authenticatedSync(2, onAuthenticated, api::coursework) { entries, state ->
            dao.replaceCoursework(entries, state)
            if (uk.ac.warwick.plus.BuildConfig.DEBUG)
                android.util.Log.i("MyWarwickPlus", "Native coursework sync succeeded; items=${entries.size}")
        }

    override suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) =
        authenticatedSync(6, onAuthenticated, api::account) { email, state ->
            state.email = email
            dao.replaceAccount(state)
            if (uk.ac.warwick.plus.BuildConfig.DEBUG)
                android.util.Log.i("MyWarwickPlus", "Native account sync succeeded; emailAvailable=${email.isNotBlank()}")
        }

    override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit) =
        authenticatedSync(kind.key, onAuthenticated, read = { user ->
            val existing = dao.feedEntries(kind.key)
            if (before != null) {
                // Authenticate/clear first: a previous account's cursor must never reach the API.
                require(kind == FeedKind.MESSAGES && existing.lastOrNull()?.id == before)
            }
            val parsed = api.feed(kind, user, before)
            val entries = if (before == null) parsed.entries else {
                val ids = existing.mapTo(HashSet()) { it.id }
                val additions = parsed.entries.filter { it.id !in ids }
                if (parsed.entries.isNotEmpty() && additions.isEmpty()) throw InvalidResponseException()
                (existing + additions).sortedWith(compareByDescending<FeedEntry> { it.dateMillis }.thenBy { it.id }).take(500)
            }
            entries.forEachIndexed { index, item -> item.position = index }
            parsed.meta.hasMore = parsed.meta.hasMore && entries.size < 500
            ParsedFeed(entries, parsed.meta)
        }) { parsed, state -> dao.replaceFeed(parsed.entries, parsed.meta, state) }

    override suspend fun signOut() = inStore { endSession(); dao.clear() }
}
