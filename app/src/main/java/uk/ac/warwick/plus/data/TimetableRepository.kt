package uk.ac.warwick.plus.data

import uk.ac.warwick.plus.debugLog
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class CachedTimetable(val events: List<EventEntity>, val sync: SyncEntity?,
    val coursework: List<CourseworkEntity> = emptyList(), val courseworkSync: SyncEntity? = null,
    val feeds: Map<FeedKind, CachedFeed> = emptyMap(), val accountSync: SyncEntity? = null,
    val revision: Long = 0)

interface TimetableStore {
    fun observeCache(): Flow<CachedTimetable>
    suspend fun cached(): CachedTimetable
    suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit): Unit
    suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit): Unit
    suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit): Unit
    suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit): Unit
    suspend fun signOut()
}

interface StudentApi {
    // Synchronous test/probe clients remain compatible; production binds HTTP to cancellation.
    suspend fun <T> request(operation: () -> T): T = operation()
    fun user(): SignedInUser
    fun timetable(user: SignedInUser): List<EventEntity>
    fun coursework(user: SignedInUser): List<CourseworkEntity>
    fun account(user: SignedInUser): String
    fun feed(kind: FeedKind, user: SignedInUser, before: String?): ParsedFeed
}

class TimetableRepository(private val api: StudentApi, private val dao: StudentCache,
    private val logSync: (() -> String) -> Unit = { debugLog(it) },
    private val endSession: suspend () -> Unit = {}) : TimetableStore {
    private val mutex = Mutex()
    // Accessed only under mutex; this ordering also protects against late Flow reads.
    private var revision = 0L
    private fun readCache() = dao.snapshot().copy(revision = revision)

    override fun observeCache(): Flow<CachedTimetable> = dao.changes()
        .map { inStore { readCache() } }.distinctUntilChanged()

    // Keep network work outside Room transactions, but serialize it with cache reads and sign-out.
    private suspend fun <T> inStore(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            operation()
        }
    }

    private suspend fun authenticate(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit): SignedInUser {
        val user = api.request { api.user() }
        currentCoroutineContext().ensureActive()
        val changedAccount = dao.states().any { it.userCode != user.code }
        if (changedAccount) {
            dao.clear()
            revision++
        }
        onAuthenticated(user, if (changedAccount) readCache() else null)
        return user
    }

    private suspend fun <T> authenticatedSync(id: Int, onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit,
        read: suspend (SignedInUser) -> T, describe: (T) -> String = { "" }, save: (T, SyncEntity) -> Unit): Unit = inStore {
        val user = authenticate(onAuthenticated)
        val result = read(user)
        // A blocking HTTP read can finish after cancellation; never persist that result.
        currentCoroutineContext().ensureActive()
        val state = SyncEntity().apply {
            this.id = id
            userCode = user.code
            displayName = user.name
            syncedAt = System.currentTimeMillis()
        }
        save(result, state)
        revision++
        logSync { "Native resource sync succeeded; slot=$id ${describe(result)}" }
    }

    override suspend fun cached(): CachedTimetable = inStore { readCache() }

    override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) =
        authenticatedSync(SyncSlots.TIMETABLE, onAuthenticated,
            read = { user -> api.request { api.timetable(user) } },
            describe = { "events=${it.size}" }, save = dao::replace)

    override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) =
        authenticatedSync(SyncSlots.COURSEWORK, onAuthenticated,
            read = { user -> api.request { api.coursework(user) } },
            describe = { "items=${it.size}" }, save = dao::replaceCoursework)

    override suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) =
        authenticatedSync(SyncSlots.ACCOUNT, onAuthenticated,
            read = { user -> api.request { api.account(user) } },
            describe = { "emailAvailable=${it.isNotBlank()}" }) { email, state ->
            state.email = email
            dao.replaceAccount(state)
        }

    override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable?) -> Unit) =
        authenticatedSync(kind.key, onAuthenticated, read = { user ->
            val existing = dao.feedEntries(kind.key)
            if (before != null) {
                // Authenticate/clear first: a previous account's cursor must never reach the API.
                require(kind == FeedKind.MESSAGES && existing.lastOrNull()?.id == before)
            }
            val parsed = api.request { api.feed(kind, user, before) }
            val entries = if (before == null) parsed.entries else {
                val ids = existing.mapTo(HashSet()) { it.id }
                val additions = parsed.entries.filter { it.id !in ids }
                if (parsed.entries.isNotEmpty() && additions.isEmpty()) throw InvalidResponseException()
                (existing + additions).sortedWith(FeedOrder).take(500)
            }
            entries.forEachIndexed { index, item -> item.position = index }
            parsed.meta.hasMore = parsed.meta.hasMore && entries.size < 500
            ParsedFeed(entries, parsed.meta)
        }) { parsed, state -> dao.replaceFeed(parsed.entries, parsed.meta, state) }

    override suspend fun signOut(): Unit = inStore {
        endSession()
        dao.clear()
        revision++
    }
}
