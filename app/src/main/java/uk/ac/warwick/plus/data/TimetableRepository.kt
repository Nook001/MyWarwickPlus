package uk.ac.warwick.plus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CachedTimetable(val events: List<EventEntity>, val sync: SyncEntity?,
    val coursework: List<CourseworkEntity> = emptyList(), val courseworkSync: SyncEntity? = null)

interface TimetableStore {
    suspend fun cached(): CachedTimetable
    suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
    suspend fun syncCoursework(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
}

interface StudentApi {
    fun user(): SignedInUser
    fun timetable(user: SignedInUser): List<EventEntity>
    fun coursework(user: SignedInUser): List<CourseworkEntity>
}

class TimetableRepository(private val api: StudentApi, private val dao: TimetableDao) : TimetableStore {
    private val mutex = Mutex()
    private fun snapshot() = CachedTimetable(dao.events(), dao.state(), dao.coursework(), dao.courseworkState())
    private fun authenticate(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): SignedInUser {
        val user = api.user()
        if (listOfNotNull(dao.state(), dao.courseworkState()).any { it.userCode != user.code }) dao.clear()
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
    suspend fun clear() = withContext(Dispatchers.IO) { mutex.withLock { dao.clear() } }
}
