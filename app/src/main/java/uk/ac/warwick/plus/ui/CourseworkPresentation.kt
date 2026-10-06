package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.CourseworkContentItem

fun filterCoursework(entries: List<CourseworkContentItem>, query: String, filter: String, now: Long): List<CourseworkContentItem> {
    return filterCoursework(entries, query, CourseworkFilter.restore(filter), now)
}

fun filterCoursework(entries: List<CourseworkContentItem>, query: String, filter: CourseworkFilter, now: Long): List<CourseworkContentItem> {
    val search = query.trim()
    return entries.filter { entry ->
        (search.isEmpty() || entry.title.contains(search, true) || entry.description.contains(search, true)) &&
            (when (filter) {
                CourseworkFilter.PAST -> entry.dueMillis < now
                CourseworkFilter.UPCOMING -> entry.dueMillis >= now
            })
    }
}
