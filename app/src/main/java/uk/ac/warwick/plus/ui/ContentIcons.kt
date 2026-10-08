package uk.ac.warwick.plus.ui

/** One outline style for content categories, shared by headers and metadata. */
internal object ContentIcons {
    val bus = serviceIcon("Bus") {
        moveTo(5f, 17f); lineTo(5f, 5f); lineTo(7f, 3f); lineTo(17f, 3f); lineTo(19f, 5f); lineTo(19f, 17f); close()
        moveTo(5f, 11f); lineTo(19f, 11f); moveTo(8f, 14f); lineTo(9f, 14f); moveTo(15f, 14f); lineTo(16f, 14f)
        moveTo(7f, 17f); lineTo(7f, 21f); moveTo(17f, 17f); lineTo(17f, 21f)
    }
    val print = serviceIcon("Print") {
        moveTo(7f, 8f); lineTo(7f, 3f); lineTo(17f, 3f); lineTo(17f, 8f)
        moveTo(7f, 17f); lineTo(3f, 17f); lineTo(3f, 8f); lineTo(21f, 8f); lineTo(21f, 17f); lineTo(17f, 17f)
        moveTo(7f, 13f); lineTo(17f, 13f); lineTo(17f, 21f); lineTo(7f, 21f); close()
    }
    val clock = serviceIcon("ClassTime") {
        moveTo(21f, 12f); curveTo(21f, 17f, 17f, 21f, 12f, 21f)
        curveTo(7f, 21f, 3f, 17f, 3f, 12f); curveTo(3f, 7f, 7f, 3f, 12f, 3f)
        curveTo(17f, 3f, 21f, 7f, 21f, 12f); close()
        moveTo(12f, 7f); lineTo(12f, 12f); lineTo(16f, 14f)
    }
    val calendar get() = CalendarPickerIcon
    val deadlines = serviceIcon("Deadline") {
        moveTo(8f, 4f); lineTo(5f, 4f); lineTo(5f, 21f); lineTo(19f, 21f)
        lineTo(19f, 4f); lineTo(16f, 4f)
        moveTo(8f, 3f); lineTo(16f, 3f); lineTo(16f, 7f); lineTo(8f, 7f); close()
        moveTo(9f, 12f); lineTo(15f, 12f); moveTo(9f, 16f); lineTo(13f, 16f)
    }
    val location = serviceIcon("ClassLocation") {
        moveTo(12f, 22f); curveTo(9f, 18f, 5f, 14f, 5f, 9f)
        curveTo(5f, 5f, 8f, 2f, 12f, 2f); curveTo(16f, 2f, 19f, 5f, 19f, 9f)
        curveTo(19f, 14f, 15f, 18f, 12f, 22f); close()
        moveTo(15f, 9f); curveTo(15f, 13f, 9f, 13f, 9f, 9f)
        curveTo(9f, 5f, 15f, 5f, 15f, 9f); close()
    }
}
