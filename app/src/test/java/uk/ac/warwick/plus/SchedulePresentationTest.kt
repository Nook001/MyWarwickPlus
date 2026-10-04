package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.ui.*
import java.time.*
import java.util.TimeZone

class SchedulePresentationTest {
    private fun event(id: String, start: String, end: String) = EventEntity().apply {
        this.id = id; startMillis = ZonedDateTime.parse(start).toInstant().toEpochMilli()
        endMillis = ZonedDateTime.parse(end).toInstant().toEpochMilli()
    }
    @Test fun agendaKeepsEmptyAnchorSkipsGapsAndSortsCoursesAndDates() {
        val from = LocalDate.of(2026,10,4)
        val events = listOf(event("late", "2026-10-05T13:00:00+01:00", "2026-10-05T14:00:00+01:00"),
            event("early", "2026-10-05T10:00:00+01:00", "2026-10-05T11:00:00+01:00"),
            event("past", "2026-10-03T10:00:00+01:00", "2026-10-03T11:00:00+01:00"),
            event("later", "2026-10-07T10:00:00+01:00", "2026-10-07T11:00:00+01:00"))
        val days = scheduleDays(events,from)
        assertEquals(listOf(from,from.plusDays(1),from.plusDays(3)),days.map { it.date })
        assertTrue(days.first().events.isEmpty())
        assertEquals(listOf("early","late"),days[1].events.map { it.id })
        assertEquals("past",scheduleDays(events,from.minusDays(1)).first().events.single().id)
    }
    @Test fun overnightAndDstSegmentsRespectExclusiveEndAndZeroDuration() {
        val from = LocalDate.of(2026,10,25)
        val overnight = event("overnight", "2026-10-24T23:30:00+01:00", "2026-10-25T02:30:00Z")
        val midnight = event("midnight", "2026-10-25T23:00:00Z", "2026-10-26T00:00:00Z")
        val point = event("point", "2026-10-26T12:00:00Z", "2026-10-26T12:00:00Z")
        val days = scheduleDays(listOf(overnight,midnight,point),from)
        assertEquals(listOf("overnight","midnight"),days.first().events.map { it.id })
        assertEquals(listOf("point"),days[1].events.map { it.id })
        assertEquals(ScheduleTime("00:00","02:30",true,false),scheduleTime(overnight,from))
        assertEquals(ScheduleTime("23:00","24:00",false,false),scheduleTime(midnight,from))
        assertEquals(ScheduleTime("23:30","24:00",false,true),scheduleTime(overnight,from.minusDays(1)))
    }
    @Test fun pickerDatesIgnorePhoneTimezoneAndLabelsRemainUnambiguous() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            listOf(LocalDate.of(2026,3,29),LocalDate.of(2026,10,25),LocalDate.of(2027,1,1)).forEach {
                assertEquals(it,pickerDate(pickerMillis(it)))
            }
            val today=LocalDate.of(2026,10,4)
            assertEquals("Today",scheduleDateLabel(today,today))
            assertEquals("Tomorrow · 5 Oct",scheduleDateLabel(today.plusDays(1),today))
            assertEquals("Fri 1 Jan 2027",scheduleDateLabel(LocalDate.of(2027,1,1),today))
        } finally { TimeZone.setDefault(previous) }
    }
}
