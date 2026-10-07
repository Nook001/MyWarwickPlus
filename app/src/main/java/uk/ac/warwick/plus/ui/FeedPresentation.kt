package uk.ac.warwick.plus.ui

import android.text.Html
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import uk.ac.warwick.plus.data.FeedContentItem

fun feedText(entry: FeedContentItem): String = if (entry.html) Html.fromHtml(entry.text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\uFFFC", "").trim() else entry.text

internal class FeedTextCache {
    private val text = object : LruCache<String, String>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: String) = (key.length + value.length) * 2
    }
    fun plain(entry: FeedContentItem): String {
        if (!entry.html) return entry.text
        return text[entry.text] ?: feedText(entry).also { text.put(entry.text, it) }
    }
}

internal val LocalFeedTextCache = staticCompositionLocalOf<FeedTextCache?> { null }

// Associate the result with its source list so refreshed IDs never briefly display old bodies.
@Composable
internal fun rememberFeedText(entries: List<FeedContentItem>): Map<String, String> {
    val cache = LocalFeedTextCache.current ?: remember { FeedTextCache() }
    val result by produceState<Pair<List<FeedContentItem>, Map<String, String>>?>(null, entries, cache) {
        value = entries to withContext(Dispatchers.Default) {
            entries.associate { entry ->
                currentCoroutineContext().ensureActive()
                entry.id to cache.plain(entry)
            }
        }
    }
    return result?.takeIf { it.first === entries }?.second.orEmpty()
}

@Composable
internal fun rememberFeedText(entry: FeedContentItem): String {
    val cache = LocalFeedTextCache.current ?: remember { FeedTextCache() }
    val result by produceState<Pair<FeedContentItem, String>?>(null, entry, cache) {
        value = entry to withContext(Dispatchers.Default) { cache.plain(entry) }
    }
    return result?.takeIf { it.first == entry }?.second ?: if (entry.html) "" else entry.text
}
