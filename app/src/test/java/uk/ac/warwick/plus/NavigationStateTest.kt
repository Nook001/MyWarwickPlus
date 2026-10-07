package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.FeedEntry
import uk.ac.warwick.plus.data.FeedKind
import uk.ac.warwick.plus.ui.*
import java.time.LocalDate

class NavigationStateTest {
    @Test fun navigatorRestoreKeepsDateAndRouteButAccountResetDropsPrivateSelections() {
        val today = LocalDate.of(2026, 10, 7)
        val navigator = PlusNavigator(today)
        navigator.chooseDate(today.plusDays(2), today)
        navigator.openTasks(CourseworkFilter.PAST)
        navigator.select(AppTab.ME)
        navigator.meRoute = MeRoute.Feed(FeedKind.MESSAGES)
        navigator.detail = DetailSelection.Feed(FeedKind.MESSAGES, "id:with\nseparator")
        val restored = PlusNavigator.restore(navigator.savedValues())
        assertEquals(navigator.detail, restored.detail)
        assertEquals(today.plusDays(2).toEpochDay(), restored.selectedDay)
        assertFalse(restored.followToday)
        restored.back()
        assertEquals(AppTab.ME, restored.tab)
        assertEquals(MeRoute.Overview, restored.meRoute)
        restored.meRoute = MeRoute.Settings
        restored.detail = DetailSelection.Task("private-task")
        restored.resetForAccountChange()
        assertEquals(DetailSelection.None, restored.detail)
        assertEquals(MeRoute.Overview, restored.meRoute)
        assertEquals(CourseworkFilter.UPCOMING, restored.courseworkFilter)
    }
    @Test fun restoredDetailWaitsForCacheAndCannotCrossFeedRoutes() {
        val id = "message:with/separators\nand more"
        val route = MeRoute.Feed(FeedKind.MESSAGES)
        val detail = DetailSelection.Feed(FeedKind.MESSAGES, id)
        val restoredRoute = MeRoute.restore(route.savedValues())
        val restoredDetail = DetailSelection.restore(detail.savedValues())
        assertEquals(route, restoredRoute)
        assertEquals(detail, restoredDetail)
        assertEquals(detail, restoredDetail.validated(TimetableState(busy = true), restoredRoute, AppTab.ME))
        val loaded = TimetableState(feeds = mapOf(FeedKind.MESSAGES to FeedState(
            entries = listOf(FeedEntry().apply { this.id = id }), lastSynced = 1)))
        assertEquals(detail, restoredDetail.validated(loaded, restoredRoute, AppTab.ME))
        assertEquals(DetailSelection.None, restoredDetail.validated(loaded, MeRoute.Feed(FeedKind.MODULES), AppTab.ME))
        assertEquals(DetailSelection.None, restoredDetail.validated(loaded.copy(feeds = emptyMap()), restoredRoute, AppTab.ME))
    }

    @Test fun currentSavedKeysRestoreAndUnsupportedValuesUseSafeDefaults() {
        assertEquals(AppTab.CLASSES, AppTab.restore(AppTab.CLASSES.key))
        assertEquals(AppTab.TASKS, AppTab.restore(AppTab.TASKS.key))
        assertEquals(AppTab.HOME, AppTab.restore("unknown future tab"))
        assertEquals(CourseworkFilter.PAST, CourseworkFilter.restore(CourseworkFilter.PAST.key))
        assertEquals(CourseworkFilter.UPCOMING, CourseworkFilter.restore("unknown filter"))
        assertEquals(MeRoute.Overview, MeRoute.restore(listOf("feed", "999")))
        assertEquals(DetailSelection.None, DetailSelection.restore(listOf("feed", "3")))
        assertEquals(DetailSelection.None, DetailSelection.restore(listOf("class", "")))
    }
}
