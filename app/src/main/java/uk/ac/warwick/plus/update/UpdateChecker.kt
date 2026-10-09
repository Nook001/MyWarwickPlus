package uk.ac.warwick.plus.update

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AvailableRelease(val version: String, val url: String)

enum class UpdateResult { NONE, UP_TO_DATE, FAILED }

data class UpdateState(val automatic: Boolean = true, val checking: Boolean = false,
    val available: AvailableRelease? = null, val result: UpdateResult = UpdateResult.NONE,
    val lastChecked: Long = 0, val notifiedVersion: String = "")

/** Semantic-version precedence, including numeric pre-release parts such as beta.2 < beta.10. */
internal object Versions {
    private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

    fun isValid(version: String) = pattern.matches(version)

    fun compare(left: String, right: String): Int {
        val a = requireNotNull(pattern.matchEntire(left)).groupValues
        val b = requireNotNull(pattern.matchEntire(right)).groupValues
        for (index in 1..3) a[index].toBigInteger().compareTo(b[index].toBigInteger()).let { if (it != 0) return it }
        val preA = a[4]; val preB = b[4]
        if (preA.isEmpty() || preB.isEmpty()) return when {
            preA == preB -> 0
            preA.isEmpty() -> 1
            else -> -1
        }
        val partsA = preA.split('.'); val partsB = preB.split('.')
        for (index in 0 until minOf(partsA.size, partsB.size)) {
            val x = partsA[index]; val y = partsB[index]
            val numX = x.all(Char::isDigit); val numY = y.all(Char::isDigit)
            val order = when {
                numX && numY -> x.toBigInteger().compareTo(y.toBigInteger())
                numX -> -1
                numY -> 1
                else -> x.compareTo(y)
            }
            if (order != 0) return order
        }
        return partsA.size.compareTo(partsB.size)
    }
}

internal const val RELEASES_URL = "https://github.com/Nook001/MyWarwickPlus/releases/"

/** Drafts and pre-releases are excluded by GitHub's latest endpoint, matching the published Latest. */
internal fun parseLatestRelease(body: String): AvailableRelease {
    val root = JSONObject(body)
    val tag = root.optString("tag_name")
    val url = root.optString("html_url")
    if (root.optBoolean("draft") || root.optBoolean("prerelease") || !Versions.isValid(tag) ||
        !url.startsWith(RELEASES_URL)) throw IOException("Unexpected release response")
    return AvailableRelease(tag.removePrefix("v"), url)
}

/**
 * Asks GitHub once a day for the Latest release. The request carries no cookies or account data;
 * downloading and installing stay with the browser and Android's package installer.
 */
class UpdateChecker(private val preferences: SharedPreferences, private val currentVersion: String,
    private val automaticAllowed: Boolean, private val userAgent: String,
    private val fetch: () -> String = { fetchLatest(userAgent) },
    private val clock: () -> Long = System::currentTimeMillis) {

    private val current = MutableStateFlow(load())
    val state = current.asStateFlow()

    private fun load(): UpdateState {
        val version = preferences.getString(KEY_VERSION, null)
        val url = preferences.getString(KEY_URL, null)
        val available = if (version != null && url != null && url.startsWith(RELEASES_URL) &&
            isNewer(version)) AvailableRelease(version, url) else null
        return UpdateState(automatic = preferences.getBoolean(KEY_AUTOMATIC, true), available = available,
            lastChecked = preferences.getLong(KEY_CHECKED, 0), notifiedVersion = preferences.getString(KEY_NOTIFIED, "").orEmpty())
    }

    private fun isNewer(version: String) =
        Versions.isValid(version) && Versions.isValid(currentVersion) && Versions.compare(version, currentVersion) > 0

    fun setAutomatic(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_AUTOMATIC, enabled) }
        current.update { it.copy(automatic = enabled) }
    }

    fun markNotified(version: String) {
        preferences.edit { putString(KEY_NOTIFIED, version) }
        current.update { it.copy(notifiedVersion = version) }
    }

    suspend fun checkIfDue() {
        val state = current.value
        val elapsed = clock() - state.lastChecked
        if (!automaticAllowed || !state.automatic || elapsed in 0 until DAY_MILLIS) return
        check()
    }

    suspend fun check() {
        if (current.value.checking) return
        current.update { it.copy(checking = true) }
        val latest = try {
            withContext(Dispatchers.IO) { parseLatestRelease(fetch()) }
        } catch (error: Exception) {
            if (error is CancellationException) { current.update { it.copy(checking = false) }; throw error }
            current.update { it.copy(checking = false, result = UpdateResult.FAILED) }
            return
        }
        val now = clock()
        val newer = latest.takeIf { isNewer(it.version) }
        preferences.edit {
            putLong(KEY_CHECKED, now)
            putString(KEY_VERSION, latest.version)
            putString(KEY_URL, latest.url)
        }
        current.update { it.copy(checking = false, available = newer, lastChecked = now,
            result = if (newer == null) UpdateResult.UP_TO_DATE else UpdateResult.NONE) }
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val KEY_AUTOMATIC = "update_auto_check"
        const val KEY_CHECKED = "update_last_checked"
        const val KEY_VERSION = "update_latest_version"
        const val KEY_URL = "update_latest_url"
        const val KEY_NOTIFIED = "update_notified_version"

        private val client by lazy {
            OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS).build()
        }

        fun fetchLatest(userAgent: String): String {
            val request = Request.Builder().url("https://api.github.com/repos/Nook001/MyWarwickPlus/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", userAgent).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val source = response.body.source()
                source.request(MAX_BYTES + 1)
                if (source.buffer.size > MAX_BYTES) throw IOException("Release response too large")
                return source.readUtf8()
            }
        }
        const val MAX_BYTES = 512L * 1024
    }
}
