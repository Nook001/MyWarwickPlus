package uk.ac.warwick.plus.data

import androidx.room.Entity
import org.json.JSONObject

enum class ServiceKind(val slot: Int, val tile: String, val refreshMillis: Long) {
    BUSES(SyncSlots.BUSES, "bus", 60_000),
    PRINT(SyncSlots.PRINT, "print", 600_000),
    EVENTS(SyncSlots.CAMPUS_EVENTS, "uni-events", 1_800_000)
}

@Entity(tableName = "service_summaries", primaryKeys = ["resource", "id"])
data class ServiceSummaryEntity(val resource: Int, val id: String, val callout: String, val text: String, val position: Int)

@Entity(tableName = "campus_events", primaryKeys = ["id"])
data class CampusEventEntity(val id: String, val source: String, val title: String, val description: String,
    val url: String, val location: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean)

@Entity(tableName = "service_meta", primaryKeys = ["resource"])
data class ServiceMeta(val resource: Int, val description: String, val url: String)

data class ServiceSummary(val id: String, val callout: String, val text: String)
data class CampusEvent(val id: String, val source: String, val title: String, val description: String,
    val url: String, val location: String, val startMillis: Long, val endMillis: Long, val allDay: Boolean)

internal fun ServiceSummaryEntity.snapshot() = ServiceSummary(id, callout, text)
internal fun CampusEventEntity.snapshot() = CampusEvent(id, source, title, description, url, location, startMillis, endMillis, allDay)

data class ParsedService(val summaries: List<ServiceSummaryEntity>, val events: List<CampusEventEntity>, val meta: ServiceMeta)
data class CachedService(val summaries: List<ServiceSummaryEntity>, val events: List<CampusEventEntity>, val meta: ServiceMeta?, val sync: SyncEntity?)

object ServiceParser {
    fun parse(kind: ServiceKind, body: String): ParsedService {
        val content = tileContent(body, kind.tile)
        val meta = ServiceMeta(kind.slot, content.stringOrEmpty("defaultText"), safeExternalUrl(content.stringOrEmpty("href")) ?: "")
        val items = content.getJSONArray("items")
        if (items.length() > 2_000) throw InvalidResponseException()
        if (kind != ServiceKind.EVENTS) {
            val summaries = items.mapObjects { index, item ->
                ServiceSummaryEntity(kind.slot, item.requiredString("id"),
                    item.strictString("callout"), item.strictString("text"), index)
            }.requireUniqueIds { it.id }
            return ParsedService(summaries, emptyList(), meta)
        }
        val events = items.mapObjects { _, item ->
            val start = networkDate(item.requiredString("start"))
            val end = networkDate(item.requiredString("end"))
            if (end < start) throw InvalidResponseException()
            val allDay = item.opt("isAllDay") as? Boolean ?: throw InvalidResponseException()
            val location = item.getJSONArray("location").mapObjects { _, place -> place.strictString("name") }
                .filter { it.isNotBlank() }.joinToString(" · ")
            CampusEventEntity(item.requiredString("id"), item.strictString("source"), item.requiredString("title"),
                item.strictString("extraInfo"), safeExternalUrl(item.strictString("href")) ?: "", location,
                start, end, allDay)
        }.requireUniqueIds { it.id }
        return ParsedService(emptyList(), events.sortedWith(compareBy({ it.startMillis }, { it.id })), meta)
    }

    private fun JSONObject.strictString(key: String): String =
        (opt(key) as? String)?.takeIf { it.length <= 131_072 } ?: throw InvalidResponseException()
}
