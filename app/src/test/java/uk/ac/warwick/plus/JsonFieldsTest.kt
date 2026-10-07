package uk.ac.warwick.plus

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.*

class JsonFieldsTest {
    @Test fun nullAndWrongTypesNeverBecomeDisplayStringsOrLinks() {
        val fields = JSONObject("""{"name":null,"number":42,"object":{},"valid":"text"}""")
        for (key in listOf("name", "number", "object", "missing")) {
            assertEquals("", fields.stringOrEmpty(key))
            try { fields.requiredString(key); fail("Invalid required string") }
            catch (_: InvalidResponseException) { }
        }
        assertEquals("text", fields.requiredString("valid"))
        val task = CourseworkParser.parse("""{"success":true,"data":{"coursework":{"content":{"items":[{"id":"task","title":"Task","date":"2026-10-10T12:00:00Z","text":null,"href":null}]}}}}""").single()
        assertEquals("", task.url)
        assertEquals("", task.description)
        val event = TimetableParser.parse("""{"success":true,"data":{"timetable":{"content":{"items":[{"id":"event","title":null,"start":"2026-10-10T12:00:00Z","end":"2026-10-10T13:00:00Z","parent":{"shortName":null,"fullName":null},"location":{"name":null,"href":null}}]}}}}""").single()
        assertEquals("", event.title)
        assertEquals("", event.module)
        assertEquals("", event.locationUrl)
    }
}
