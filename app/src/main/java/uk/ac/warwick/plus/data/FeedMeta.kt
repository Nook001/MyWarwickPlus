package uk.ac.warwick.plus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_meta")
data class FeedMeta(
    @PrimaryKey var feed: Int = 0,
    var description: String = "",
    var url: String = "",
    var hasMore: Boolean = false,
    var webReadMillis: Long = 0
)
