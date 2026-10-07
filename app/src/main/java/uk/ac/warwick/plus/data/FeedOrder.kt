package uk.ac.warwick.plus.data

internal val FeedOrder = compareByDescending<FeedEntry> { it.dateMillis }.thenBy { it.id }
