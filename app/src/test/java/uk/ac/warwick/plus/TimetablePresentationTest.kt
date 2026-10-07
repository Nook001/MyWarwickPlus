package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.time.*

class TimetablePresentationTest {
    private fun event(start: String, end: String) = EventEntity().apply {
        id = "example"; startMillis = ZonedDateTime.parse(start).toInstant().toEpochMilli()
        endMillis = ZonedDateTime.parse(end).toInstant().toEpochMilli()
    }
    @Test fun overnightClassAppearsOnBothDatesButNotExclusiveEndDate() {
        val item = event("2026-10-05T23:30:00+01:00", "2026-10-06T01:00:00+01:00")
        assertEquals(1, eventsOnDate(listOf(item), LocalDate.of(2026, 10, 5)).size)
        assertEquals(1, eventsOnDate(listOf(item), LocalDate.of(2026, 10, 6)).size)
        assertTrue(eventsOnDate(listOf(item), LocalDate.of(2026, 10, 7)).isEmpty())
        val allDay = event("2026-10-05T00:00:00+01:00", "2026-10-06T00:00:00+01:00")
        assertTrue(eventsOnDate(listOf(allDay), LocalDate.of(2026, 10, 6)).isEmpty())
    }
    @Test fun londonDayBoundariesRespectDstAndPhoneTimezone() {
        val item = event("2026-10-25T23:30:00Z", "2026-10-26T00:30:00Z")
        assertEquals(1, eventsOnDate(listOf(item), LocalDate.of(2026, 10, 25)).size)
        assertEquals(1, eventsOnDate(listOf(item), LocalDate.of(2026, 10, 26)).size)
    }
    @Test fun countdownDistinguishesNowTomorrowAndRoundsUp() {
        val item = event("2026-10-05T10:00:00+01:00", "2026-10-05T11:00:00+01:00")
        assertEquals(text(R.string.in_minutes, 2L), nextClassLabel(item, item.startMillis - 61_000))
        assertEquals(text(R.string.happening_now), nextClassLabel(item, item.startMillis))
        assertEquals(text(R.string.tomorrow), nextClassLabel(item, item.startMillis - 24 * 3_600_000))
    }
    @Test fun probeExposesOnlySchemaAndCounts() {
        val body = """{"success":true,"data":{"coursework":{"content":{"items":[{"id":"private-id","title":"Private title","date":"2026-10-15T12:00:00.000+01","href":"https://tabula.warwick.ac.uk/private"}]}}}}"""
        val result = ProbeSummary.parse(ProbeEndpoint.COURSEWORK, 200, 10, body)
        assertEquals(1, result.itemCount)
        assertEquals(listOf("date", "href", "id", "title"), result.itemFields)
        assertFalse(result.toString().contains("private"))
        val empty = ProbeSummary.parse(ProbeEndpoint.LIBRARY, 200, 10,
            """{"success":true,"data":{"library":{"content":{"items":[]}}}}""")
        assertEquals(0, empty.itemCount)
        assertTrue(empty.itemFields.isEmpty())
    }
}
