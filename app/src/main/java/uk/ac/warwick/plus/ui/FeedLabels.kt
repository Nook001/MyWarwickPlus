package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

val FeedKind.label: String get() = when (this) {
    FeedKind.MESSAGES -> AppLabels.MESSAGES
    FeedKind.LIBRARY -> AppLabels.LIBRARY
    FeedKind.MODULES -> AppLabels.MODULES
}
