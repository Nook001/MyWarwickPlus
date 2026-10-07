package uk.ac.warwick.plus.data

import org.json.JSONObject
import java.time.ZonedDateTime

class InvalidResponseException : Exception("The server returned an unexpected response.")

object TimetableParser {
    fun parse(body: String): List<EventEntity> {
        val items = tileContent(body, "timetable").getJSONArray("items")
        // Do not replace a valid cache with a partially decoded response.
        return items.mapObjects { _, item ->
            val parent = item.optJSONObject("parent")
            val place = item.optJSONObject("location")
            EventEntity().apply {
                id = item.requiredString("id").also { require(it.isNotBlank()) }
                title = item.stringOrEmpty("title")
                // MyWarwick sends ISO_ZONED_DATE_TIME, e.g. +01:00[Europe/London].
                // ZonedDateTime also accepts offset-only strings and UTC Z.
                startMillis = ZonedDateTime.parse(item.requiredString("start")).toInstant().toEpochMilli()
                endMillis = ZonedDateTime.parse(item.requiredString("end")).toInstant().toEpochMilli()
                require(endMillis >= startMillis)
                allDay = item.optBoolean("isAllDay")
                academicWeek = item.optInt("academicWeek")
                module = parent?.stringOrEmpty("shortName").orEmpty()
                moduleName = parent?.stringOrEmpty("fullName").orEmpty()
                location = place?.stringOrEmpty("name").orEmpty()
                locationUrl = place?.stringOrEmpty("href").orEmpty()
            }
        }.requireUniqueIds { it.id }
    }
}
