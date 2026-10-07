package uk.ac.warwick.plus.data

import androidx.room.Entity

@Entity(tableName = "feed_entries", primaryKeys = ["feed", "id"])
data class FeedEntry(
    override var feed: Int = 0,
    override var id: String = "",
    override var title: String = "",
    override var text: String = "",
    override var url: String = "",
    override var provider: String = "",
    override var type: String = "",
    override var dateMillis: Long = 0,
    override var html: Boolean = false,
    override var moduleCode: String = "",
    override var academicYear: String = "",
    override var announcementCount: Int = 0,
    override var evaluationCount: Int = 0,
    override var position: Int = 0
) : FeedContentItem
