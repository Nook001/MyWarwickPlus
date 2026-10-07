package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

import androidx.compose.ui.graphics.vector.ImageVector
import uk.ac.warwick.plus.auth.MY_WARWICK
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

/** Labels are presentation only; no page chooses a destination or icon by matching label text. */
internal enum class WarwickService(val labelRes: Int, val url: String, val icon: ImageVector,
    val shortLabelRes: Int = labelRes, val homeLabelRes: Int? = null) {
    MOODLE(R.string.service_moodle, "https://moodle.warwick.ac.uk/", ServiceIcons.moodle, homeLabelRes = R.string.service_moodle),
    EMAIL(R.string.service_email, "https://warwick.ac.uk/mymail", ServiceIcons.email, homeLabelRes = R.string.service_email),
    TABULA(R.string.service_tabula, "https://tabula.warwick.ac.uk/", ServiceIcons.tabula, homeLabelRes = R.string.service_tabula),
    LIBRARY(R.string.service_library_account, "https://warwick.ac.uk/services/library/account", ServiceIcons.library, homeLabelRes = R.string.label_library),
    ACADEMIC(R.string.service_academic, "https://warwick.ac.uk/academic-support/", ServiceIcons.moodle),
    WELLBEING(R.string.service_wellbeing_full, "https://warwick.ac.uk/wellbeing/students/", MeIcons.wellbeing, shortLabelRes = R.string.service_wellbeing),
    SAFETY(R.string.service_safety_full, "https://warwick.ac.uk/students/safety-and-support/emergency-support/", MeIcons.safety, shortLabelRes = R.string.service_safety),
    HELP(R.string.service_help_full, "https://warwick.ac.uk/mw-support", MeIcons.help, shortLabelRes = R.string.service_help)
}

internal val HomeServices = WarwickService.entries.filter { it.homeLabelRes != null }
internal fun FeedKind.websiteUrl(): String = when (this) {
    FeedKind.LIBRARY -> WarwickService.LIBRARY.url
    FeedKind.MODULES -> WarwickService.MOODLE.url
    FeedKind.MESSAGES -> "$MY_WARWICK/alerts"
}
