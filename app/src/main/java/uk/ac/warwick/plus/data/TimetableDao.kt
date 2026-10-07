package uk.ac.warwick.plus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TimetableDao : StudentCache {
    // Observe all five tables, then re-read them in snapshot()'s single transaction.
    // A constant result is intentional: the query signals invalidation, not content.
    @RawQuery(observedEntities = [EventEntity::class, CourseworkEntity::class,
        SyncEntity::class, FeedEntry::class, FeedMeta::class])
    protected abstract fun invalidations(query: SupportSQLiteQuery): Flow<Int>
    override fun changes(): Flow<Unit> = invalidations(SimpleSQLiteQuery("SELECT 1")).map { Unit }

    // Contents and metadata are always read from the same database transaction.
    @Transaction
    override fun snapshot(): CachedTimetable = CachedTimetable(
        events(), state(), coursework(), courseworkState(),
        FeedKind.entries.associateWith { CachedFeed(feedEntries(it.key), feedMeta(it.key), feedState(it.key)) },
        feedState(SyncSlots.ACCOUNT))

    @Query("SELECT * FROM events ORDER BY startMillis, id") abstract fun events(): List<EventEntity>
    @Query("SELECT * FROM sync_state WHERE id = ${SyncSlots.TIMETABLE}") abstract fun state(): SyncEntity?
    @Query("SELECT * FROM coursework ORDER BY dueMillis, id") abstract fun coursework(): List<CourseworkEntity>
    @Query("SELECT * FROM sync_state WHERE id = ${SyncSlots.COURSEWORK}") abstract fun courseworkState(): SyncEntity?
    @Query("DELETE FROM coursework") abstract fun deleteCoursework()
    @Query("DELETE FROM sync_state WHERE id = ${SyncSlots.COURSEWORK}") abstract fun deleteCourseworkState()
    @Query("DELETE FROM events") abstract fun deleteEvents()
    @Query("DELETE FROM sync_state WHERE id = ${SyncSlots.TIMETABLE}") abstract fun deleteState()
    @Insert abstract fun insertEvents(events: List<EventEntity>)
    @Insert abstract fun insertState(state: SyncEntity)
    @Insert abstract fun insertCoursework(entries: List<CourseworkEntity>)
    @Query("SELECT * FROM sync_state ORDER BY id") abstract override fun states(): List<SyncEntity>
    @Query("SELECT * FROM feed_entries WHERE feed = :feed ORDER BY position, id") abstract override fun feedEntries(feed: Int): List<FeedEntry>
    @Query("SELECT * FROM feed_meta WHERE feed = :feed") abstract fun feedMeta(feed: Int): FeedMeta?
    @Query("SELECT * FROM sync_state WHERE id = :feed") abstract fun feedState(feed: Int): SyncEntity?
    @Query("DELETE FROM feed_entries WHERE feed = :feed") abstract fun deleteFeedEntries(feed: Int)
    @Query("DELETE FROM feed_meta WHERE feed = :feed") abstract fun deleteFeedMeta(feed: Int)
    @Query("DELETE FROM sync_state WHERE id = :feed") abstract fun deleteFeedState(feed: Int)
    @Query("DELETE FROM feed_entries") abstract fun deleteFeeds()
    @Query("DELETE FROM feed_meta") abstract fun deleteFeedMetas()
    @Query("DELETE FROM sync_state WHERE id > ${SyncSlots.COURSEWORK}") abstract fun deleteNonCoreStates()
    @Insert abstract fun insertFeedEntries(entries: List<FeedEntry>)
    @Insert abstract fun insertFeedMeta(meta: FeedMeta)

    @Transaction
    override fun replaceFeed(entries: List<FeedEntry>, meta: FeedMeta, state: SyncEntity) {
        require(state.id == meta.feed) { "Feed state does not match" }
        deleteFeedEntries(meta.feed)
        deleteFeedMeta(meta.feed)
        deleteFeedState(meta.feed)
        insertFeedEntries(entries)
        insertFeedMeta(meta)
        insertState(state)
    }

    @Transaction
    override fun replace(events: List<EventEntity>, state: SyncEntity) {
        deleteEvents()
        deleteState()
        insertEvents(events)
        insertState(state)
    }

    @Transaction
    override fun replaceCoursework(entries: List<CourseworkEntity>, state: SyncEntity) {
        deleteCoursework()
        deleteCourseworkState()
        insertCoursework(entries)
        insertState(state)
    }

    @Transaction
    override fun clear() {
        deleteEvents()
        deleteState()
        deleteCoursework()
        deleteCourseworkState()
        deleteFeeds()
        deleteFeedMetas()
        deleteNonCoreStates()
    }

    @Transaction
    override fun replaceAccount(state: SyncEntity) {
        require(state.id == SyncSlots.ACCOUNT) { "Account state does not match" }
        deleteFeedState(SyncSlots.ACCOUNT)
        insertState(state)
    }
}
