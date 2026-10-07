package uk.ac.warwick.plus.data

/** Atomic cache operations exposed to the repository; SQL mutation primitives stay in the DAO. */
@JvmSuppressWildcards
interface StudentCache {
    fun snapshot(): CachedTimetable
    fun states(): List<SyncEntity>
    fun feedEntries(feed: Int): List<FeedEntry>
    fun replace(events: List<EventEntity>, state: SyncEntity)
    fun replaceCoursework(entries: List<CourseworkEntity>, state: SyncEntity)
    fun replaceAccount(state: SyncEntity)
    fun replaceFeed(entries: List<FeedEntry>, meta: FeedMeta, state: SyncEntity)
    fun clear()
}
