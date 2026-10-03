package uk.ac.warwick.plus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.ProbeEndpoint
import uk.ac.warwick.plus.data.ProbeResult
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
            PlusScreen(TimetableState(lastSynced = 1L, coursework = CourseworkState(listOf(entry), 1L)), {}, {},
                onCourseworkLink = { opened = it })
        } }
        compose.onNodeWithText("View all coursework").performScrollTo().performClick()
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
            PlusScreen(TimetableState(needsLogin = true, message = "Sign in to update your saved data.",
                coursework = CourseworkState(listOf(entry), 1L, "Your saved deadlines have been kept.")), {}, {})
        } }
        compose.onNodeWithText("Sign in with Warwick").assertDoesNotExist()
        compose.onNodeWithText("Coursework", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Past assignment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Deadline passed").assertIsDisplayed()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
    }
    @Test fun emptyFeedDoesNotImplyAllAssignmentsAreSubmitted() {
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(lastSynced = 1L, coursework = CourseworkState(lastSynced = 1L)), {}, {})
        } }
        compose.onNodeWithText("Coursework", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No deadlines returned").assertIsDisplayed()
        compose.onNodeWithText("Check the source service for the full record.", substring = true).assertIsDisplayed()
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
        compose.setContent { PlusTheme { PlusScreen(TimetableState(needsLogin = true), {}, { requested = true }) } }
        compose.onNodeWithText("Sign in with Warwick").performClick()
        assertTrue(requested)
    }
    @Test fun authenticatedButFailedSyncOffersRetryInsteadOfLogin() {
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(signedIn = true, message = "You're signed in, but your timetable couldn't be loaded. Try refreshing."), {}, {})
        } }
        compose.onNodeWithText("Sign in with Warwick").assertDoesNotExist()
        compose.onNodeWithText("Try again").assertIsDisplayed()
        compose.onNodeWithText("No classes today").assertDoesNotExist()
    }
    @Test fun savedClassCanBeViewedOnScheduleWithoutLogin() {
        val start = ZonedDateTime.now(WarwickZone).withHour(23).withMinute(0).withSecond(0).withNano(0)
        val event = EventEntity().apply {
            id = "ui-example"; title = "Example module"; location = "Example room"
            startMillis = start.toInstant().toEpochMilli(); endMillis = start.plusMinutes(30).toInstant().toEpochMilli()
        }
        compose.setContent {
            PlusTheme { PlusScreen(TimetableState(events = listOf(event), lastSynced = 1L), {}, {}) }
        }
        capture("home")
        compose.onNodeWithText("Schedule").performClick()
        compose.onNodeWithText("Example module").assertIsDisplayed()
        compose.onNodeWithText("Example room").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Week").performClick()
        compose.onNodeWithText("No classes this week").assertIsDisplayed()
    }

    @Test fun daySelectionAndDetailsShowModuleAndLocation() {
        val start = ZonedDateTime.now(WarwickZone).withHour(12).withMinute(0).withSecond(0).withNano(0)
        val event = EventEntity().apply {
            id = "details-example"; title = "Example seminar"; module = "EX101"; moduleName = "Example module name"; location = "Example room"
            startMillis = start.toInstant().toEpochMilli(); endMillis = start.plusHours(1).toInstant().toEpochMilli()
        }
        compose.setContent { PlusTheme { PlusScreen(TimetableState(events = listOf(event), lastSynced = 1L), {}, {}) } }
        compose.onNodeWithText("Schedule").performClick()
        capture("day")
        compose.onNodeWithText("Example seminar").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
        compose.onNode(hasText("EX101") and hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
        compose.onNode(hasText("Example module name") and hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
        capture("details")
        compose.onNodeWithText("Close details").performScrollTo().performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("No classes on this day").assertIsDisplayed()
        compose.onNodeWithText("Today").performClick()
        compose.onNodeWithText("Example seminar").assertIsDisplayed()
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
                message = "Your session has expired. Sign in to update your saved timetable."), {}, { requested = true })
        } }
        compose.onNodeWithText("Sign in").performClick()
        assertTrue(requested)
        compose.onNodeWithText("Schedule").performClick()
        compose.onNodeWithText("Cached seminar").performScrollTo().assertIsDisplayed()
    }

    @Test fun explorerMakesNoRequestUntilManuallyTriggered() {
        val calls = java.util.concurrent.atomic.AtomicInteger()
        compose.setContent { PlusTheme {
            PlusScreen(TimetableState(lastSynced = 1L), {}, {}, probe = { endpoint ->
                calls.incrementAndGet()
                ProbeResult(endpoint, 200, 5, true, listOf("items"), 3, listOf("date", "title"))
            })
        } }
        compose.onNodeWithTag("home-list").performScrollToNode(hasText("Developer tools"))
        compose.onNodeWithText("Developer tools").performClick()
        compose.onNodeWithText("API explorer").assertIsDisplayed()
        org.junit.Assert.assertEquals(0, calls.get())
        compose.onNodeWithText("Run request").performScrollTo().performClick()
        compose.waitUntil(5_000) { calls.get() == 1 }
        compose.onNodeWithText("HTTP 200", substring = true).performScrollTo().assertIsDisplayed()
    }
}
