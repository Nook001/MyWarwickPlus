package uk.ac.warwick.plus.data

import org.json.JSONObject
import java.net.URI
import java.time.ZonedDateTime

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
                val date = item.getString("date").replace(Regex("([+-]\\d{2})$"), "$1:00")
                dueMillis = ZonedDateTime.parse(date).toInstant().toEpochMilli()
            }
        }.also { require(it.map { entry -> entry.id }.distinct().size == it.size) }
            .sortedWith(compareBy<CourseworkEntity> { it.dueMillis }.thenBy { it.id })
    }
}

fun safeCourseworkUrl(raw: String): String? = runCatching {
    val uri = URI("https://my.warwick.ac.uk/").resolve(raw)
    val host = uri.host?.lowercase().orEmpty()
    if (raw.isNotBlank() && uri.scheme == "https" && uri.rawUserInfo == null &&
        (uri.port == -1 || uri.port == 443) && (host == "warwick.ac.uk" || host.endsWith(".warwick.ac.uk")))
        uri.toASCIIString() else null
}.getOrNull()
