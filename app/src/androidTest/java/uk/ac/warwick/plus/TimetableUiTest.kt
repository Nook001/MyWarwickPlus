package uk.ac.warwick.plus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.ui.*
import java.time.*

class TimetableUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun homeDeadlineLeadsToCourseworkDetailsAndOnlyExplicitlyOpensSource() {
        val due = ZonedDateTime.now(WarwickZone).plusDays(2).toInstant().toEpochMilli()
        val entry = CourseworkEntity().apply {
            id = "sample-deadline"; title = "Example assignment"; description = "Example instructions"
            dueMillis = due; url = "https://tabula.warwick.ac.uk/coursework/example"
        }
        var opened: String? = null
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(lastSynced = 1L, coursework = CourseworkState(listOf(entry), 1L)),
                testActions(openExternal = { opened = it }))
        } }
        compose.onNodeWithText("View all tasks").performScrollTo().performClick()
        compose.onNodeWithText("Example assignment").assertIsDisplayed()
        capture("coursework")
        compose.onNodeWithText("Example assignment").performClick()
        compose.onNodeWithText("COURSEWORK DETAILS").assertIsDisplayed()
        compose.onNodeWithText("Example instructions").assertIsDisplayed()
        org.junit.Assert.assertNull(opened)
        capture("coursework-details")
        compose.onNodeWithText("Open source service").performScrollTo().performClick()
        org.junit.Assert.assertEquals(entry.url, opened)
    }
    @Test fun expiredSessionWithOnlyCourseworkCacheStillShowsDeadlines() {
        val entry = CourseworkEntity().apply { id = "past"; title = "Past assignment"; dueMillis = 1L }
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(needsLogin = true, message = UiText.Literal("Sign in to update your saved data."),
                coursework = CourseworkState(listOf(entry), 1L, UiText.Literal("Your saved deadlines have been kept."))), testActions())
        } }
        compose.onNodeWithText("Sign in with Warwick").assertDoesNotExist()
        compose.onNodeWithTag("tab-coursework").performClick()
        compose.onNodeWithText("Past assignment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Passed").assertIsDisplayed()
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
    }
    @Test fun emptyFeedDoesNotImplyAllAssignmentsAreSubmitted() {
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(lastSynced = 1L, coursework = CourseworkState(lastSynced = 1L)), testActions())
        } }
        compose.onNodeWithTag("tab-coursework").performClick()
        compose.onNodeWithText("No upcoming deadlines").assertIsDisplayed()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir, "ui-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    @Test fun signedOutScreenOffersOfficialLogin() {
        var requested = false
        compose.setContent { PlusTheme { PlusScreen(TimetableState(needsLogin = true), testActions(signIn = { requested = true })) } }
        compose.onNodeWithText("Sign in with Warwick").performClick()
        assertTrue(requested)
    }
    @Test fun authenticatedButFailedSyncOffersRetryInsteadOfLogin() {
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(signedIn = true, message = UiText.Literal("Your timetable couldn't be loaded."),
                notice = SyncNotice(1, UiText.Literal("Your timetable couldn't be loaded."))), testActions())
        } }
        compose.onNodeWithText("Sign in with Warwick").assertDoesNotExist()
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("No classes today").assertDoesNotExist()
    }
    @Test fun savedClassCanBeViewedOnScheduleWithoutLogin() {
        val start = ZonedDateTime.now(WarwickZone).withHour(23).withMinute(0).withSecond(0).withNano(0)
        val event = EventEntity().apply {
            id = "ui-example"; title = "Example module"; location = "Example room"
            startMillis = start.toInstant().toEpochMilli(); endMillis = start.plusMinutes(30).toInstant().toEpochMilli()
        }
        compose.setContent {
            PlusTheme { PlusScreen(TimetableState(events = listOf(event), lastSynced = 1L), testActions()) }
        }
        capture("home")
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithText("Example module").assertIsDisplayed()
        compose.onNodeWithText("Example room").assertIsDisplayed()
        compose.onNodeWithTag("schedule-date-picker").assertIsDisplayed()
        compose.onNodeWithText("Week").assertDoesNotExist()
    }

    @Test fun daySelectionAndDetailsShowModuleAndLocation() {
        val start = ZonedDateTime.now(WarwickZone).withHour(12).withMinute(0).withSecond(0).withNano(0)
        val event = EventEntity().apply {
            id = "details-example"; title = "Example seminar"; module = "EX101"; moduleName = "Example module name"; location = "Example room"
            startMillis = start.toInstant().toEpochMilli(); endMillis = start.plusHours(1).toInstant().toEpochMilli()
        }
        compose.setContent { PlusTheme { PlusScreen(TimetableState(events = listOf(event), lastSynced = 1L), testActions()) } }
        compose.onNodeWithTag("schedule-tab").performClick()
        capture("day")
        compose.onNodeWithText("Example module name").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
        compose.onNode(hasText("EX101") and hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
        compose.onNode(hasText("Example module name") and hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
        capture("details")
        compose.onNodeWithText("Close details").performScrollTo().performClick()
        compose.onNodeWithText("Example module name").assertIsDisplayed()
    }

    @Test fun expiredSessionStillShowsCachedClassesAndOffersLogin() {
        val start = ZonedDateTime.now(WarwickZone).withHour(12).withMinute(0)
        val event = EventEntity().apply {
            id = "cached-example"; title = "Cached seminar"
            startMillis = start.toInstant().toEpochMilli(); endMillis = start.plusHours(1).toInstant().toEpochMilli()
        }
        var requested = false
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(events = listOf(event), lastSynced = 1L, needsLogin = true,
                message = UiText.Literal("Your session has expired. Sign in to update your saved timetable.")), testActions(signIn = { requested = true }))
        } }
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithText("Sign in").performClick()
        assertTrue(requested)
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithText("Cached seminar").performScrollTo().assertIsDisplayed()
    }
}
