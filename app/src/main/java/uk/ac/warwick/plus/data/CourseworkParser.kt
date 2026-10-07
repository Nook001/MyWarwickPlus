package uk.ac.warwick.plus.data

import org.json.JSONObject

object CourseworkParser {
    fun parse(body: String): List<CourseworkEntity> {
        val items = tileContent(body, "coursework").getJSONArray("items")
        return items.mapObjects { _, item ->
            CourseworkEntity().apply {
                id = item.requiredString("id").also { require(it.isNotBlank()) }
                title = item.requiredString("title").also { require(it.isNotBlank()) }
                description = item.stringOrEmpty("text")
                url = safeCourseworkUrl(item.stringOrEmpty("href")) ?: ""
                // The live aggregation uses both +01 (hour-only offset) and Z.
                dueMillis = networkDate(item.requiredString("date"))
            }
        }.requireUniqueIds { it.id }
            .sortedWith(compareBy<CourseworkEntity> { it.dueMillis }.thenBy { it.id })
    }
}
