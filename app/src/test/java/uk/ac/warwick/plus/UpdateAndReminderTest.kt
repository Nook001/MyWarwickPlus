package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.reminders.*
import uk.ac.warwick.plus.update.Versions
import uk.ac.warwick.plus.update.parseLatestRelease

class UpdateAndReminderTest {
    @Test fun versionsFollowSemanticPrecedenceIncludingBetaNumbers() {
        val ordered = listOf("0.27.0-beta.1", "0.27.1-beta.1", "0.27.1-beta.2", "0.27.1-beta.10", "0.27.1", "0.28.0", "1.0.0")
        ordered.zipWithNext().forEach { (lower, higher) ->
            assertTrue("$lower < $higher", Versions.compare(lower, higher) < 0)
            assertTrue("$higher > $lower", Versions.compare(higher, lower) > 0)
        }
        assertEquals(0, Versions.compare("v0.27.1-beta.1", "0.27.1-beta.1"))
        assertFalse(Versions.isValid("latest"))
    }

    @Test fun releaseMustBePublishedFromThisRepository() {
        val release = parseLatestRelease("""{"tag_name":"v0.28.0","draft":false,"prerelease":false,
            "html_url":"https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.28.0"}""")
        assertEquals("0.28.0", release.version)
        for (body in listOf(
            """{"tag_name":"v0.28.0","html_url":"https://example.com/releases/tag/v0.28.0"}""",
            """{"tag_name":"v0.28.0","prerelease":true,"html_url":"https://github.com/Nook001/MyWarwickPlus/releases/tag/v0.28.0"}""")) {
            assertThrows(java.io.IOException::class.java) { parseLatestRelease(body) }
        }
    }

    @Test fun lateAlarmStillRemindsUnlessTheClassHasStarted() {
        val start = 10 * 3_600_000L
        val classes = listOf(EventEntity(id = "a", startMillis = start, endMillis = start + 3_600_000),
            EventEntity(id = "all-day", startMillis = 0, endMillis = 86_400_000, allDay = true))
        val tasks = listOf(CourseworkEntity(id = "t", dueMillis = start + DEADLINE_LEAD_MILLIS))
        val all = reminders(classes, tasks, classes = true, deadlines = true)
        assertEquals(listOf("a", "t"), all.map { it.id })
        assertEquals(start - CLASS_LEAD_MILLIS, nextTrigger(all, 0))
        val trigger = start - CLASS_LEAD_MILLIS
        assertEquals(listOf("a"), dueReminders(all, trigger - 1, trigger + 5 * 60_000).map { it.id })
        assertTrue(dueReminders(all, trigger - 1, start + 1).none { it.id == "a" })
        assertTrue(dueReminders(all, trigger, trigger + 60_000).isEmpty())
        assertTrue(reminders(classes, tasks, classes = false, deadlines = false).isEmpty())
    }
}
