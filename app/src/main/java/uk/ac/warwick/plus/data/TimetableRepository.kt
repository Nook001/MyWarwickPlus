package uk.ac.warwick.plus.data

import kotlinx.coroutines.Dispatchers
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
    private fun snapshot() = CachedTimetable(dao.events(), dao.state(), dao.coursework(), dao.courseworkState(),
        FeedKind.entries.associateWith { CachedFeed(dao.feedEntries(it.key), dao.feedMeta(it.key), dao.feedState(it.key)) }, dao.feedState(6))
    private fun authenticate(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): SignedInUser {
        val user = api.user()
        if (dao.states().any { it.userCode != user.code }) dao.clear()
        onAuthenticated(user, snapshot())
        return user
    }
    override suspend fun cached(): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock { snapshot() }
    }
    override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = authenticate(onAuthenticated)
            val events = api.timetable(user)
            val state = SyncEntity().apply {
                userCode = user.code
                displayName = user.name
                syncedAt = System.currentTimeMillis()
            }
            dao.replace(events, state)
            if (uk.ac.warwick.plus.BuildConfig.DEBUG) {
                android.util.Log.i("MyWarwickPlus", "Native timetable sync succeeded; events=${events.size}")
            }
            snapshot()
        }
    }
    override suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = authenticate(onAuthenticated)
            val entries = api.coursework(user)
            dao.replaceCoursework(entries, SyncEntity().apply {
                id = 2; userCode = user.code; displayName = user.name; syncedAt = System.currentTimeMillis()
            })
            if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.i("MyWarwickPlus", "Native coursework sync succeeded; items=${entries.size}")
            snapshot()
        }
    }
    override suspend fun syncFeed(kind: FeedKind, before: String?, onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = authenticate(onAuthenticated)
            val existing = dao.feedEntries(kind.key)
            if (before != null) {
                // A cursor captured from a previous account must never be used for the new account.
                require(kind == FeedKind.MESSAGES && existing.lastOrNull()?.id == before)
            }
            val parsed = api.feed(kind, user, before)
            val entries = if (before == null) parsed.entries else {
                val additions = parsed.entries.filter { item -> existing.none { it.id == item.id } }
                if (parsed.entries.isNotEmpty() && additions.isEmpty()) throw InvalidResponseException()
                (existing + additions).sortedWith(compareByDescending<FeedEntry> { it.dateMillis }.thenBy { it.id }).take(500)
            }
            entries.forEachIndexed { index, item -> item.position = index }
            parsed.meta.hasMore = parsed.meta.hasMore && entries.size < 500
            dao.replaceFeed(entries, parsed.meta, SyncEntity().apply {
                id = kind.key; userCode = user.code; displayName = user.name; syncedAt = System.currentTimeMillis()
            })
            snapshot()
        }
    }
    override suspend fun signOut() = withContext(Dispatchers.IO) {
        mutex.withLock { endSession(); dao.clear() }
    }
    override suspend fun syncAccount(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = authenticate(onAuthenticated)
            val email = api.account(user)
            dao.replaceAccount(SyncEntity().apply {
                id = 6; userCode = user.code; displayName = user.name; this.email = email; syncedAt = System.currentTimeMillis()
            })
            if (uk.ac.warwick.plus.BuildConfig.DEBUG) android.util.Log.i("MyWarwickPlus", "Native account sync succeeded; emailAvailable=${email.isNotBlank()}")
            snapshot()
        }
    }
}
