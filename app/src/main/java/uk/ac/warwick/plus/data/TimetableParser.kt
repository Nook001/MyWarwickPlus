package uk.ac.warwick.plus.data

import org.json.JSONObject
import java.time.ZonedDateTime

class InvalidResponseException : Exception("The server returned an unexpected response.")

object TimetableParser {
    fun parse(body: String): List<EventEntity> {
        val root = JSONObject(body)
        if (!root.optBoolean("success")) throw InvalidResponseException()
        val items = root.getJSONObject("data").getJSONObject("timetable")
            .getJSONObject("content").getJSONArray("items")
        // Do not replace a valid cache with a partially decoded response.
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            EventEntity().apply {
                id = item.getString("id").also { require(it.isNotBlank()) }
                title = item.getString("title")
                // MyWarwick sends ISO_ZONED_DATE_TIME, e.g. +01:00[Europe/London].
                // ZonedDateTime also accepts offset-only strings and UTC Z.
                startMillis = ZonedDateTime.parse(item.getString("start")).toInstant().toEpochMilli()
                endMillis = ZonedDateTime.parse(item.getString("end")).toInstant().toEpochMilli()
                require(endMillis >= startMillis)
                allDay = item.optBoolean("isAllDay")
                academicWeek = item.optInt("academicWeek")
                module = item.optJSONObject("parent")?.optString("shortName").orEmpty()
                moduleName = item.optJSONObject("parent")?.optString("fullName").orEmpty()
                location = item.optJSONObject("location")?.optString("name").orEmpty()
                locationUrl = item.optJSONObject("location")?.optString("href").orEmpty()
            }
        }.also { require(it.map(EventEntity::id).distinct().size == it.size) }
    }
}
