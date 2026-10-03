package uk.ac.warwick.plus.data

import org.json.JSONObject
import java.net.URI
import java.time.ZonedDateTime

enum class FeedKind(val key: Int, val label: String, val tile: String) {
    MESSAGES(3, "Messages", "notifications"), LIBRARY(4, "Library", "library"), MODULES(5, "Modules", "modules");
    fun path(before: String? = null): String = if (this == MESSAGES) {
        "/api/streams/notifications?limit=100" + (before?.let { "&before=" + java.net.URLEncoder.encode(it, "UTF-8") } ?: "")
    } else "/api/tiles/content/$tile"
}
data class ParsedFeed(val entries: List<FeedEntry>, val meta: FeedMeta)
data class CachedFeed(val entries: List<FeedEntry>, val meta: FeedMeta?, val sync: SyncEntity?)

fun networkDate(value: String): Long = ZonedDateTime.parse(value.replace(Regex("([+-]\\d{2})$"), "$1:00"))
    .toInstant().toEpochMilli()

/** Links open only after a user gesture in the system browser, without our cookie jar. */
fun safeExternalUrl(raw: String): String? = runCatching {
    val uri = URI("https://my.warwick.ac.uk/").resolve(raw)
    if (raw.isNotBlank() && uri.scheme == "https" && !uri.host.isNullOrBlank() &&
        uri.rawUserInfo == null && uri.port in listOf(-1, 443)) uri.toASCIIString() else null
}.getOrNull()

object FeedParser {
    fun parse(kind: FeedKind, body: String): ParsedFeed {
        val root = JSONObject(body)
        if (!root.optBoolean("success")) throw InvalidResponseException()
        val data = root.getJSONObject("data")
        val content = if (kind == FeedKind.MESSAGES) data else data.getJSONObject(kind.tile).getJSONObject("content")
        val items = content.getJSONArray(if (kind == FeedKind.MESSAGES) "notifications" else "items")
        val meta = FeedMeta().apply {
            feed = kind.key
            description = content.stringOrEmpty("defaultText")
            url = safeExternalUrl(content.stringOrEmpty("href")) ?: ""
            hasMore = kind == FeedKind.MESSAGES && items.length() == 100
            if (kind == FeedKind.MESSAGES && content.stringOrEmpty("read").isNotBlank())
                webReadMillis = networkDate(content.getString("read"))
        }
        val entries = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            FeedEntry().apply {
                feed = kind.key; position = index
                when (kind) {
                    FeedKind.MESSAGES -> {
                        id = item.getString("id").also { require(it.isNotBlank()) }
                        title = item.getString("title").also { require(it.isNotBlank()) }
                        val rich = item.stringOrEmpty("textAsHtml")
                        html = rich.isNotBlank()
                        text = if (html) rich else item.stringOrEmpty("text")
                        url = safeExternalUrl(item.stringOrEmpty("url")) ?: ""
                        provider = item.stringOrEmpty("providerDisplayName").ifBlank { item.stringOrEmpty("provider") }
                        type = item.stringOrEmpty("type")
                        dateMillis = networkDate(item.getString("date"))
                    }
                    FeedKind.MODULES -> {
                        id = item.get("id").toString().also { require(it.isNotBlank() && it != "null") }
                        title = item.getString("fullName").also { require(it.isNotBlank()) }
                        moduleCode = item.stringOrEmpty("moduleCode")
                        academicYear = item.stringOrEmpty("academicYear")
                        url = safeCourseworkUrl(item.stringOrEmpty("href")) ?: ""
                        announcementCount = item.optJSONArray("announcements")?.length() ?: 0
                        evaluationCount = item.optJSONArray("evaluations")?.length() ?: 0
                        text = "Open the module site for announcements and learning materials."
                    }
                    FeedKind.LIBRARY -> {
                        // Empty live feed: only display fields actually present; never invent loan state.
                        id = item.stringOrEmpty("id").ifBlank { "entry-$index" }
                        title = item.stringOrEmpty("title").ifBlank { "Library item" }
                        text = item.stringOrEmpty("text").ifBlank { "Open your Library account for full details." }
                        url = safeExternalUrl(item.stringOrEmpty("href")) ?: ""
                    }
                }
                require(title.length <= 16_384 && text.length <= 131_072 && url.length <= 8_192)
            }
        }.also { require(it.map { entry -> entry.id }.distinct().size == it.size) }
        return ParsedFeed(if (kind == FeedKind.MESSAGES) entries.sortedWith(compareByDescending<FeedEntry> { it.dateMillis }.thenBy { it.id }) else entries, meta)
    }
    private fun JSONObject.stringOrEmpty(key: String) = if (isNull(key)) "" else optString(key)
}
