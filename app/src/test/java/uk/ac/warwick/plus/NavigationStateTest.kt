package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.FeedEntry
import uk.ac.warwick.plus.data.FeedKind
import uk.ac.warwick.plus.ui.*

class NavigationStateTest {
    @Test fun restoredDetailWaitsForCacheAndCannotCrossFeedRoutes() {
        val id = "message:with/separators\nand more"
        val route = MeRoute.Feed(FeedKind.MESSAGES)
        val detail = DetailSelection.Feed(FeedKind.MESSAGES, id)
        val restoredRoute = MeRoute.restore(route.savedValues())
        val restoredDetail = DetailSelection.restore(detail.savedValues())
        assertEquals(route, restoredRoute)
        assertEquals(detail, restoredDetail)
        assertEquals(detail, restoredDetail.validated(TimetableState(busy = true), restoredRoute))
        val loaded = TimetableState(feeds = mapOf(FeedKind.MESSAGES to FeedState(
            entries = listOf(FeedEntry().apply { this.id = id }), lastSynced = 1)))
        assertEquals(detail, restoredDetail.validated(loaded, restoredRoute))
        assertEquals(DetailSelection.None, restoredDetail.validated(loaded, MeRoute.Feed(FeedKind.MODULES)))
        assertEquals(DetailSelection.None, restoredDetail.validated(loaded.copy(feeds = emptyMap()), restoredRoute))
    }

    @Test fun oldAndUnsupportedSavedValuesUseSafeDefaultsWithoutEnumOrdinals() {
        assertEquals(AppTab.CLASSES, AppTab.restore(1))
        assertEquals(AppTab.TASKS, AppTab.restore("coursework"))
        assertEquals(AppTab.HOME, AppTab.restore("unknown future tab"))
        assertEquals(CourseworkFilter.PAST, CourseworkFilter.restore("Past"))
        assertEquals(CourseworkFilter.UPCOMING, CourseworkFilter.restore("Next seven days"))
        assertEquals(MeRoute.Overview, MeRoute.restore(listOf("feed", "999")))
        assertEquals(DetailSelection.None, DetailSelection.restore(listOf("feed", "3")))
        assertEquals(DetailSelection.None, DetailSelection.restore(listOf("class", "")))
    }
}
