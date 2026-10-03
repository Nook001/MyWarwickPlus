package uk.ac.warwick.plus.data

import org.json.JSONObject

// Explicit allowlist: no arbitrary URLs, write operations, or authentication exports.
enum class ProbeEndpoint(val tile: String) {
    COURSEWORK("coursework"), LIBRARY("library"), TIMETABLE("timetable");
    val path get() = "/api/tiles/content/$tile"
}

data class ProbeResult(
    val endpoint: ProbeEndpoint, val httpStatus: Int, val elapsedMillis: Long,
    val success: Boolean, val contentFields: List<String>, val itemCount: Int?,
    val itemFields: List<String>
)

object ProbeSummary {
    fun parse(endpoint: ProbeEndpoint, code: Int, elapsed: Long, body: String): ProbeResult {
        val root = JSONObject(body)
        val content = root.optJSONObject("data")?.optJSONObject(endpoint.tile)?.optJSONObject("content")
        val items = content?.optJSONArray("items")
        fun keys(value: JSONObject?) = value?.keys()?.asSequence()?.toList()?.sorted().orEmpty()
        // Metadata only: raw responses and personal field values are never retained.
        return ProbeResult(endpoint, code, elapsed, root.optBoolean("success"), keys(content),
            items?.length(), keys(items?.optJSONObject(0)))
    }
}
