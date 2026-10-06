package uk.ac.warwick.plus.ui

import androidx.compose.ui.graphics.vector.ImageVector
import uk.ac.warwick.plus.auth.MY_WARWICK
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

/** Labels are presentation only; no page chooses a destination or icon by matching label text. */
internal enum class WarwickService(val label: String, val url: String, val icon: ImageVector,
    val shortLabel: String = label, val homeLabel: String? = null) {
    MOODLE("Moodle", "https://moodle.warwick.ac.uk/", ServiceIcons.moodle, homeLabel = "Moodle"),
    EMAIL("Email", "https://warwick.ac.uk/mymail", ServiceIcons.email, homeLabel = "Email"),
    TABULA("Tabula", "https://tabula.warwick.ac.uk/", ServiceIcons.tabula, homeLabel = "Tabula"),
    LIBRARY("Library account", "https://warwick.ac.uk/services/library/account", ServiceIcons.library, homeLabel = AppLabels.LIBRARY),
    ACADEMIC("Academic support", "https://warwick.ac.uk/academic-support/", ServiceIcons.moodle),
    WELLBEING("Wellbeing and Student Support", "https://warwick.ac.uk/wellbeing/students/", MeIcons.wellbeing, shortLabel = "Wellbeing"),
    SAFETY("Safety and emergency support", "https://warwick.ac.uk/students/safety-and-support/emergency-support/", MeIcons.safety, shortLabel = "Safety"),
    HELP("MyWarwick help", "https://warwick.ac.uk/mw-support", MeIcons.help, shortLabel = "Help")
}

internal val HomeServices = WarwickService.entries.filter { it.homeLabel != null }
internal fun FeedKind.websiteUrl(): String = when (this) {
    FeedKind.LIBRARY -> WarwickService.LIBRARY.url
    FeedKind.MODULES -> WarwickService.MOODLE.url
    FeedKind.MESSAGES -> "$MY_WARWICK/alerts"
}
