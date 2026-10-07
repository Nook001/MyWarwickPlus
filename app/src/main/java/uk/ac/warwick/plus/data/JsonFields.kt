package uk.ac.warwick.plus.data

import org.json.JSONArray
import org.json.JSONObject

// Read actual types, never org.json's platform-dependent string coercion.
internal fun JSONObject.stringOrEmpty(key: String): String = opt(key) as? String ?: ""

internal fun JSONObject.requiredString(key: String): String =
    stringOrEmpty(key).takeIf { it.isNotBlank() } ?: throw InvalidResponseException()

internal fun responseData(body: String): JSONObject {
    val root = JSONObject(body)
    if (!root.optBoolean("success")) throw InvalidResponseException()
    return root.getJSONObject("data")
}

internal fun tileContent(body: String, tile: String): JSONObject =
    responseData(body).getJSONObject(tile).getJSONObject("content")

internal inline fun <T> JSONArray.mapObjects(transform: (Int, JSONObject) -> T): List<T> =
    List(length()) { transform(it, getJSONObject(it)) }

internal fun <T> List<T>.requireUniqueIds(id: (T) -> String): List<T> =
    also { require(mapTo(HashSet(), id).size == size) }
