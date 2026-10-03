package uk.ac.warwick.plus.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CachedTimetable(val events: List<EventEntity>, val sync: SyncEntity?)

interface TimetableStore {
    suspend fun cached(): CachedTimetable
    suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable
}

class TimetableRepository(private val api: MyWarwickApi, private val dao: TimetableDao) : TimetableStore {
    private val mutex = Mutex()
    override suspend fun cached(): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock { CachedTimetable(dao.events(), dao.state()) }
    }
    override suspend fun sync(onAuthenticated: (SignedInUser, CachedTimetable) -> Unit): CachedTimetable = withContext(Dispatchers.IO) {
        mutex.withLock {
            val user = api.user()
            // Never leave the previous student's timetable visible after an account change.
            if (dao.state()?.userCode?.let { it != user.code } == true) dao.clear()
            onAuthenticated(user, CachedTimetable(dao.events(), dao.state()))
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
            CachedTimetable(dao.events(), state)
        }
    }
    suspend fun clear() = withContext(Dispatchers.IO) { mutex.withLock { dao.clear() } }
}
