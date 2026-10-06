package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.TimetableState
import uk.ac.warwick.plus.ui.withCache

class ContentSnapshotTest {
    @Test fun databaseObjectsCannotMutatePublishedContentAndEqualReloadsKeepListIdentity() {
        val event = EventEntity().apply { id = "class"; title = "Original class"; locationUrl = "https://example.invalid/room" }
        val coursework = CourseworkEntity().apply { id = "deadline"; description = "Original details"; dueMillis = 100 }
        val entry = FeedEntry().apply { id = "message"; text = "Original message"; html = true }
        val sync = SyncEntity().apply { userCode = "student"; syncedAt = 1 }
        val cache = CachedTimetable(listOf(event), sync, listOf(coursework), sync,
            mapOf(FeedKind.MESSAGES to CachedFeed(listOf(entry), null, sync)))
        val first = TimetableState().withCache(cache)
        val second = first.withCache(cache.copy(sync = SyncEntity().apply { userCode = "student"; syncedAt = 2 }))
        assertSame(first.events, second.events)
        assertSame(first.coursework.entries, second.coursework.entries)
        assertSame(first.feed(FeedKind.MESSAGES).entries, second.feed(FeedKind.MESSAGES).entries)
        assertSame(first.coursework, second.coursework)
        assertSame(first.feed(FeedKind.MESSAGES), second.feed(FeedKind.MESSAGES))
        event.title = "Changed class"; coursework.description = "Changed details"; entry.text = "Changed message"
        assertEquals("Original class", first.events.single().title)
        assertEquals("Original details", first.coursework.entries.single().description)
        assertEquals("Original message", first.feed(FeedKind.MESSAGES).entries.single().text)
        val changed = second.withCache(cache)
        assertEquals("Changed class", changed.events.single().title)
        assertNotSame(second.events, changed.events)
        assertEquals("https://example.invalid/room", changed.events.single().locationUrl)
        assertTrue(changed.feed(FeedKind.MESSAGES).entries.single().html)
    }
}
