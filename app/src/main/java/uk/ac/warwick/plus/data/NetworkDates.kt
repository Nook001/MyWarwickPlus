package uk.ac.warwick.plus.data

import java.time.ZonedDateTime

private val hourOnlyOffset = Regex("([+-]\\d{2})$")

/** Aggregate dates require a zone/offset; some tiles use the hour-only +01 form. */
fun networkDate(value: String): Long = ZonedDateTime.parse(value.replace(hourOnlyOffset, "$1:00"))
    .toInstant().toEpochMilli()
