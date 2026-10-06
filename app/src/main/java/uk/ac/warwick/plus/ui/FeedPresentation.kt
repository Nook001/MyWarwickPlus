package uk.ac.warwick.plus.ui

import android.text.Html
import uk.ac.warwick.plus.data.FeedEntry

fun feedText(entry: FeedEntry): String = if (entry.html) Html.fromHtml(entry.text, Html.FROM_HTML_MODE_LEGACY).toString().replace("\uFFFC", "").trim() else entry.text
