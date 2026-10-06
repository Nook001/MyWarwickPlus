package uk.ac.warwick.plus.data

// Read-only contracts also let existing parser/Room fixtures use the presentation helpers.
// Production state receives value snapshots, never the mutable persistence objects.

interface EventContentItem {
    val id: String
    val title: String
    val module: String
    val moduleName: String
    val location: String
    val locationUrl: String
    val startMillis: Long
    val endMillis: Long
    val allDay: Boolean
    val academicWeek: Int
}

private data class EventSnapshot(
    override val id: String,
    override val title: String,
    override val module: String,
    override val moduleName: String,
    override val location: String,
    override val locationUrl: String,
    override val startMillis: Long,
    override val endMillis: Long,
    override val allDay: Boolean,
    override val academicWeek: Int
) : EventContentItem

internal fun EventContentItem.snapshot(): EventContentItem = EventSnapshot(
    id, title, module, moduleName, location, locationUrl, startMillis, endMillis, allDay, academicWeek)

interface CourseworkContentItem {
    val id: String
    val title: String
    val description: String
    val url: String
    val dueMillis: Long
}

private data class CourseworkSnapshot(
    override val id: String,
    override val title: String,
    override val description: String,
    override val url: String,
    override val dueMillis: Long
) : CourseworkContentItem

internal fun CourseworkContentItem.snapshot(): CourseworkContentItem = CourseworkSnapshot(
    id, title, description, url, dueMillis)

interface FeedContentItem {
    val feed: Int
    val id: String
    val title: String
    val text: String
    val url: String
    val provider: String
    val type: String
    val dateMillis: Long
    val html: Boolean
    val moduleCode: String
    val academicYear: String
    val announcementCount: Int
    val evaluationCount: Int
    val position: Int
}

private data class FeedSnapshot(
    override val feed: Int,
    override val id: String,
    override val title: String,
    override val text: String,
    override val url: String,
    override val provider: String,
    override val type: String,
    override val dateMillis: Long,
    override val html: Boolean,
    override val moduleCode: String,
    override val academicYear: String,
    override val announcementCount: Int,
    override val evaluationCount: Int,
    override val position: Int
) : FeedContentItem

internal fun FeedContentItem.snapshot(): FeedContentItem = FeedSnapshot(
    feed, id, title, text, url, provider, type, dateMillis, html, moduleCode, academicYear, announcementCount, evaluationCount, position)

// Keep the same list identity when a different resource only changed its sync metadata.
internal fun <T> List<T>.reuseIfEqual(previous: List<T>): List<T> =
    if (this == previous) previous else java.util.Collections.unmodifiableList(this)
