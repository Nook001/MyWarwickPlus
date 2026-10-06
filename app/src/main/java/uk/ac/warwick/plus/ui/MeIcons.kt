package uk.ac.warwick.plus.ui

internal object MeIcons {
    val copy = serviceIcon("CopyEmail") {
        moveTo(8f, 8f); lineTo(20f, 8f); lineTo(20f, 21f); lineTo(8f, 21f); close()
        moveTo(16f, 8f); lineTo(16f, 3f); lineTo(3f, 3f); lineTo(3f, 16f); lineTo(8f, 16f)
    }
    val check = serviceIcon("Copied") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(20f, 7f) }
    val external = serviceIcon("OpenWebsite") {
        moveTo(14f, 3f); lineTo(21f, 3f); lineTo(21f, 10f); moveTo(21f, 3f); lineTo(10f, 14f)
        moveTo(10f, 5f); lineTo(4f, 5f); lineTo(4f, 20f); lineTo(19f, 20f); lineTo(19f, 14f)
    }
    val settings = serviceIcon("SettingsSliders") {
        for (y in listOf(6f, 12f, 18f)) { moveTo(3f, y); lineTo(21f, y) }
        moveTo(8f, 3f); lineTo(8f, 9f); moveTo(16f, 9f); lineTo(16f, 15f)
        moveTo(10f, 15f); lineTo(10f, 21f)
    }
    val data = serviceIcon("DataStatus") {
        moveTo(3f, 3f); lineTo(3f, 21f); lineTo(21f, 21f)
        moveTo(7f, 16f); lineTo(7f, 12f); moveTo(12f, 16f); lineTo(12f, 7f)
        moveTo(17f, 16f); lineTo(17f, 10f)
    }
    val messages = serviceIcon("Messages") {
        moveTo(5f, 3f); lineTo(19f, 3f); quadTo(21f, 3f, 21f, 5f)
        lineTo(21f, 16f); quadTo(21f, 18f, 19f, 18f); lineTo(9f, 18f)
        lineTo(3f, 22f); lineTo(3f, 5f); quadTo(3f, 3f, 5f, 3f); close()
        moveTo(7f, 8f); lineTo(17f, 8f); moveTo(7f, 13f); lineTo(14f, 13f)
    }
    val wellbeing = serviceIcon("Wellbeing") {
        moveTo(12f, 21f); curveTo(7f, 17f, 3f, 13f, 3f, 8f)
        curveTo(3f, 2f, 10f, 2f, 12f, 7f); curveTo(14f, 2f, 21f, 2f, 21f, 8f)
        curveTo(21f, 13f, 17f, 17f, 12f, 21f); close()
    }
    val safety = serviceIcon("Safety") {
        moveTo(12f, 2f); lineTo(21f, 6f); lineTo(21f, 12f)
        curveTo(21f, 17f, 16f, 20f, 12f, 22f); curveTo(8f, 20f, 3f, 17f, 3f, 12f)
        lineTo(3f, 6f); close(); moveTo(12f, 7f); lineTo(12f, 13f)
        moveTo(12f, 16f); lineTo(12f, 17f)
    }
    val help = serviceIcon("Help") {
        moveTo(22f, 12f); curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
        curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f); curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f)
        curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f); close()
        moveTo(9f, 8f); curveTo(9f, 4f, 17f, 5f, 15f, 10f)
        lineTo(12f, 13f); lineTo(12f, 14f); moveTo(12f, 17f); lineTo(12f, 18f)
    }
    val developer = serviceIcon("DeveloperTools") {
        moveTo(8f, 5f); lineTo(2f, 12f); lineTo(8f, 19f)
        moveTo(16f, 5f); lineTo(22f, 12f); lineTo(16f, 19f)
        moveTo(14f, 3f); lineTo(10f, 21f)
    }
}
