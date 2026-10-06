package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.TimetableParser
import uk.ac.warwick.plus.ui.*
import java.time.LocalDate

class TimetableParserTest {
    private fun response(items: String) = """{"success":true,"data":{"timetable":{"content":{"items":[$items]}}}}"""
    private fun event(id: String = "e1", start: String = "2026-10-25T01:30:00+01:00", end: String = "2026-10-25T01:30:00Z") =
        """{"id":"$id","title":"Compiler Design","start":"$start","end":"$end","location":null,"parent":null}"""

    @Test fun autumnClockChangeKeepsAbsoluteDuration() {
        val item = TimetableParser.parse(response(event())).single()
        assertEquals(3_600_000L, item.endMillis - item.startMillis)
        assertEquals("01:30", timeLabel(item.startMillis))
        assertEquals("01:30", timeLabel(item.endMillis))
        assertEquals(LocalDate.of(2026, 10, 25), atWarwick(item.startMillis).toLocalDate())
    }
    @Test fun summerTimeUsesLondonEvenIfPhoneIsInChina() {
        val item = TimetableParser.parse(response(event(start = "2026-10-05T09:00:00Z", end = "2026-10-05T10:00:00Z"))).single()
        assertEquals("10:00", timeLabel(item.startMillis))
        assertEquals("11:00", timeLabel(item.endMillis))
        assertEquals("", item.location)
    }
    @Test fun myWarwickZonedTimesAcceptRegionSuffixInSummerAndWinter() {
        val summer = TimetableParser.parse(response(event(start = "2026-10-05T10:00:00+01:00[Europe/London]",
            end = "2026-10-05T11:00:00+01:00[Europe/London]"))).single()
        assertEquals("10:00", timeLabel(summer.startMillis))
        assertEquals(3_600_000L, summer.endMillis - summer.startMillis)
        val winter = TimetableParser.parse(response(event(start = "2027-01-11T13:00:00Z[Europe/London]",
            end = "2027-01-11T14:00:00Z[Europe/London]"))).single()
        assertEquals("13:00", timeLabel(winter.startMillis))
        assertEquals(3_600_000L, winter.endMillis - winter.startMillis)
    }
    @Test fun regionalTimeAcrossAutumnClockChangeKeepsBothOccurrences() {
        val item = TimetableParser.parse(response(event(start = "2026-10-25T01:30:00+01:00[Europe/London]",
            end = "2026-10-25T01:30:00Z[Europe/London]"))).single()
        assertEquals(3_600_000L, item.endMillis - item.startMillis)
    }
    @Test fun missingTimezoneRejectsWholeResponse() {
        assertThrows(Exception::class.java) {
            TimetableParser.parse(response(event("valid") + "," + event("bad", "2026-10-05T10:00:00")))
        }
    }
    @Test fun duplicateIdsAndBackwardsDurationRejectResponse() {
        assertThrows(Exception::class.java) { TimetableParser.parse(response(event() + "," + event())) }
        assertThrows(Exception::class.java) {
            TimetableParser.parse(response(event(start = "2026-10-05T11:00:00Z", end = "2026-10-05T10:00:00Z")))
        }
    }
    @Test fun emptyTimetableIsValidButServiceFailureIsNot() {
        assertTrue(TimetableParser.parse(response("")).isEmpty())
        assertThrows(Exception::class.java) { TimetableParser.parse("""{"success":false,"status":"error"}""") }
    }
    @Test fun moduleFullNameIsKeptWithoutOverwritingOriginalEntryTitle() {
        val body = """{"success":true,"data":{"timetable":{"content":{"items":[{
            "id":"module-name","title":"EX101L","start":"2026-10-05T10:00:00+01:00[Europe/London]",
            "end":"2026-10-05T11:00:00+01:00[Europe/London]",
            "parent":{"shortName":"EX101","fullName":"Example module name"}}]}}}}"""
        val item = TimetableParser.parse(body).single()
        assertEquals("Example module name", item.moduleName)
        assertEquals("EX101L", item.title)
        assertEquals("EX101", item.module)
    }
}
