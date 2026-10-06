package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.data.CourseworkEntity

fun filterCoursework(entries: List<CourseworkEntity>, query: String, filter: String, now: Long): List<CourseworkEntity> {
    val search = query.trim()
    return entries.filter { entry ->
        (search.isEmpty() || entry.title.contains(search, true) || entry.description.contains(search, true)) &&
            (if (filter == "Past") entry.dueMillis < now else entry.dueMillis >= now)
    }
}
