package uk.ac.warwick.plus.ui

import android.text.Html
import uk.ac.warwick.plus.data.FeedContentItem

fun feedText(entry: FeedContentItem): String = if (entry.html) Html.fromHtml(entry.text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\uFFFC", "").trim() else entry.text
