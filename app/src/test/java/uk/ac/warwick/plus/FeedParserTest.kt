package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.time.Instant

class FeedParserTest {
    private fun message(id: String, date: String = "2026-10-01T12:00:00Z") = """{"id":"$id","title":"Example message","date":"$date","provider":"example","providerDisplayName":"Example service","text":null,"textAsHtml":"<p>Example <b>body</b></p>","url":"https://www.warwicksu.com/example"}"""
    private fun messages(vararg rows: String) = """{"success":true,"data":{"notifications":[${rows.joinToString(",")}],"read":"2026-09-30T12:00:00Z"}}"""
    @Test fun notificationsPreserveRichTextAsInertDataAndSortByInstant() {
        val parsed = FeedParser.parse(FeedKind.MESSAGES, messages(message("old", "2026-09-29T12:00:00Z"), message("new")))
        assertEquals("new", parsed.entries.first().id)
        assertTrue(parsed.entries.first().html)
        assertEquals("Example service", parsed.entries.first().provider)
        assertEquals(Instant.parse("2026-09-30T12:00:00Z").toEpochMilli(), parsed.meta.webReadMillis)
        assertFalse(parsed.meta.hasMore)
    }
    @Test fun fullPageSignalsPossibleOlderPage() {
        assertTrue(FeedParser.parse(FeedKind.MESSAGES, messages(*(0 until 100).map { message("$it") }.toTypedArray())).meta.hasMore)
    }
    @Test fun duplicateOrMalformedMessagesRejectEntireSnapshot() {
        listOf(messages(message("same"), message("same")), messages(message("bad", "not-a-date")),
            """{"success":false,"data":{"notifications":[]}}""", """{"success":true,"data":{}}""").forEach {
            try { FeedParser.parse(FeedKind.MESSAGES, it); fail("Must preserve a previous cache") } catch (_: Exception) { }
        }
    }
    @Test fun numericModuleIdAndAcademicYearAreSupported() {
        val parsed = FeedParser.parse(FeedKind.MODULES, """{"success":true,"data":{"modules":{"content":{"items":[{"id":42,"fullName":"Example module","moduleCode":"EX101","academicYear":"2026/27","href":"https://moodle.warwick.ac.uk/course/view.php?id=42","announcements":[],"evaluations":[]}]}}}}""")
        assertEquals("42", parsed.entries.single().id)
        assertEquals("EX101", parsed.entries.single().moduleCode)
        assertEquals("2026/27", parsed.entries.single().academicYear)
    }
    @Test fun libraryEmptyResponsePreservesServiceDescriptionAndAccountLink() {
        val parsed = FeedParser.parse(FeedKind.LIBRARY, """{"success":true,"data":{"library":{"content":{"items":[],"defaultText":"No current checkouts or holds","href":"https://warwick.ac.uk/services/library/account"}}}}""")
        assertTrue(parsed.entries.isEmpty())
        assertEquals("No current checkouts or holds", parsed.meta.description)
        assertNotNull(safeExternalUrl(parsed.meta.url))
    }
    @Test fun unknownLibraryFieldsDoNotBecomeInventedLoanInformation() {
        val parsed = FeedParser.parse(FeedKind.LIBRARY, """{"success":true,"data":{"library":{"content":{"items":[{"unknownField":"private value"}]}}}}""")
        assertEquals("Library item", parsed.entries.single().title)
        assertFalse(parsed.entries.single().text.contains("private value"))
    }
    @Test fun cursorCannotInjectAnotherQueryAndExternalUrlsMustBeHttps() {
        assertEquals("/api/streams/notifications?limit=100&before=a%26limit%3D999", FeedKind.MESSAGES.path("a&limit=999"))
        listOf("http://example.com/", "javascript:alert(1)", "https://user:password@example.com/", "https://example.com:444/").forEach { assertNull(safeExternalUrl(it)) }
        assertNotNull(safeExternalUrl("https://www.warwicksu.com/"))
    }
    @Test fun courseworkFiltersSeparatePastAndUpcomingWithoutChangingUnderlyingData() {
        val now = Instant.parse("2026-10-24T23:30:00Z").toEpochMilli()
        val entries = listOf(CourseworkEntity().apply { id = "past"; dueMillis = now - 1; title = "Past report" },
            CourseworkEntity().apply { id = "soon"; dueMillis = now + 60_000; title = "Compiler report" },
            CourseworkEntity().apply { id = "far"; dueMillis = now + 10 * 86_400_000; title = "Future report" })
        assertEquals(listOf("soon"), filterCoursework(entries, "compiler", CourseworkFilter.UPCOMING, now).map { it.id })
        assertEquals(listOf("past"), filterCoursework(entries, "", CourseworkFilter.PAST, now).map { it.id })
        assertEquals(listOf("soon", "far"), filterCoursework(entries, "", CourseworkFilter.UPCOMING, now).map { it.id })
        assertEquals(3, entries.size)
    }
    @Test fun backToBackAllDayAndZeroDurationEventsDoNotCreateFalseConflicts() {
        fun event(id: String, start: Long, end: Long, allDay: Boolean = false) = EventEntity().apply { this.id = id; startMillis = start; endMillis = end; this.allDay = allDay }
        assertEquals(setOf("a", "c", "b"), conflictingEventIds(listOf(event("a",100,200), event("b",200,300), event("c",150,250), event("all",0,400,true), event("zero",150,150))))
        assertTrue(conflictingEventIds(listOf(event("a",100,200), event("b",200,300))).isEmpty())
    }
    @Test fun probeSupportsNotificationEnvelopeWithoutExportingFieldValues() {
        val summary = ProbeSummary.parse(ProbeEndpoint.MESSAGES, 200, 1, messages(message("private-id")))
        assertEquals(1, summary.itemCount)
        assertTrue(summary.itemFields.contains("textAsHtml"))
        assertFalse(summary.toString().contains("private-id"))
    }
}
