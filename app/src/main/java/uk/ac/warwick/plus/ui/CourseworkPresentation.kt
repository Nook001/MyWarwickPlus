package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.CourseworkContentItem

fun filterCoursework(entries: List<CourseworkContentItem>, query: String, filter: String, now: Long): List<CourseworkContentItem> {
    val search = query.trim()
    return entries.filter { entry ->
        (search.isEmpty() || entry.title.contains(search, true) || entry.description.contains(search, true)) &&
            (if (filter == "Past") entry.dueMillis < now else entry.dueMillis >= now)
    }
}
