package uk.ac.warwick.plus.data

import org.json.JSONObject

object CourseworkParser {
    fun parse(body: String): List<CourseworkEntity> {
        val root = JSONObject(body)
        if (!root.optBoolean("success")) throw InvalidResponseException()
        val items = root.getJSONObject("data").getJSONObject("coursework")
            .getJSONObject("content").getJSONArray("items")
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            CourseworkEntity().apply {
                id = item.getString("id").also { require(it.isNotBlank()) }
                title = item.getString("title").also { require(it.isNotBlank()) }
                description = if (item.isNull("text")) "" else item.optString("text")
                url = safeCourseworkUrl(item.optString("href")) ?: ""
                // The live aggregation uses both +01 (hour-only offset) and Z.
                dueMillis = networkDate(item.getString("date"))
            }
        }.also { require(it.map { entry -> entry.id }.distinct().size == it.size) }
            .sortedWith(compareBy<CourseworkEntity> { it.dueMillis }.thenBy { it.id })
    }
}
