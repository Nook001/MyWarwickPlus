package uk.ac.warwick.plus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.time.ZonedDateTime

class ChromeUiTest {
    @get:Rule val compose = createComposeRule()
    private fun fixture(): TimetableState {
        val tomorrow = ZonedDateTime.now(WarwickZone).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        val course = EventEntity().apply {
            id = "chrome-example"; moduleName = "Compiler Design"; title = "EX101L"; location = "Example lecture theatre"
            startMillis = tomorrow.toInstant().toEpochMilli(); endMillis = tomorrow.plusHours(1).toInstant().toEpochMilli()
        }
        return TimetableState(name = "Avery Smith", signedIn = true, lastSynced = 1L, events = listOf(course), coursework = CourseworkState(lastSynced = 1L))
    }
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(400)
        val instrument = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val image = instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir, "ui-08-$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }; image.recycle()
    }
    @Test fun compactChromeAndCardSeparationRemainReadableAcrossAllFiveColours() {
        var appearance by mutableStateOf(Appearance())
        compose.setContent { PlusTheme(appearance) { PlusScreen(fixture(), {}, {}) } }
        ColourTheme.entries.forEach { theme ->
            compose.runOnIdle { appearance = Appearance(theme) }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("background-ready-${theme.id}-false").fetchSemanticsNodes().isNotEmpty() }
            val home = compose.onNodeWithTag("tab-home").getUnclippedBoundsInRoot()
            val more = compose.onNodeWithTag("more-tab").getUnclippedBoundsInRoot()
            assertTrue("Tab content must be 48–68dp high", (home.bottom - home.top) in 48.dp..68.dp)
            assertTrue("Tab targets must be pulled in from rounded edges", home.left >= 20.dp)
            assertTrue("Both ends should use the same inset", more.right <= compose.onRoot().getUnclippedBoundsInRoot().right - 20.dp)
            assertTrue(compose.onNodeWithTag("page-header").getUnclippedBoundsInRoot().let { it.bottom - it.top } <= 48.dp)
            capture("home-${theme.id}")
        }
        compose.onNodeWithTag("schedule-tab").performClick()
        assertTrue(compose.onNodeWithTag("page-header").getUnclippedBoundsInRoot().let { it.bottom - it.top } <= 56.dp)
        capture("schedule-rosewood")
        compose.onNodeWithText("Coursework").performClick()
        assertTrue(compose.onNodeWithTag("page-header").getUnclippedBoundsInRoot().let { it.bottom - it.top } <= 48.dp)
        compose.onNodeWithTag("more-tab").performClick()
        assertTrue(compose.onNodeWithTag("page-header").getUnclippedBoundsInRoot().let { it.bottom - it.top } <= 48.dp)
        capture("more-rosewood")
    }
    @Test fun narrowLargeFontTabsRetainTouchTargetsAndSelectedSemantics() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.8f)) {
                PlusTheme { Box(Modifier.width(320.dp)) { PlusScreen(fixture(), {}, {}) } }
            }
        }
        for (tag in listOf("schedule-tab", "tab-coursework", "more-tab", "tab-home")) {
            compose.onNodeWithTag(tag).performClick().assertIsSelected()
            assertTrue(compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().let { it.bottom - it.top } >= 48.dp)
        }
        capture("large-font")
    }
    @Test fun failureNoticeDisappearsAndSavedContentIsNeverReplacedByAnErrorCard() {
        var state by mutableStateOf(fixture().copy(message = "A test connection failure", notice = SyncNotice(1, "A test connection failure")))
        compose.setContent { PlusTheme { PlusScreen(state, {}, {}, onNoticeConsumed = { id -> if (state.notice?.id == id) state = state.copy(notice = null) }) } }
        compose.onNodeWithText("Compiler Design").assertIsDisplayed()
        compose.onNodeWithText("A test connection failure").assertIsDisplayed()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("A test connection failure").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Compiler Design").assertIsDisplayed()
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithText("A test connection failure").assertDoesNotExist()
        capture("failed-request")
    }
    @Test fun noticeRetryTargetsTheFailedFeedAndExpiredSessionRetainsLoginInMore() {
        var full = 0; var feeds = 0; var logins = 0; var older = 0
        var state by mutableStateOf(fixture().copy(notice = SyncNotice(1, "Messages couldn't be updated", FeedKind.MESSAGES)))
        compose.setContent { PlusTheme { PlusScreen(state, { full++ }, { logins++ }, onFeedRefresh = { assertEquals(FeedKind.MESSAGES, it); feeds++ },
            onMoreMessages = { older++ },
            onNoticeConsumed = { id -> if (state.notice?.id == id) state = state.copy(notice = null) }) } }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(1, feeds); assertEquals(0, full)
            state = state.copy(signedIn = false, needsLogin = true, notice = SyncNotice(2, "Sign in to update your information.")) }
        compose.onNodeWithText("Sign in").performClick()
        compose.runOnIdle { assertEquals(1, logins) }
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithText("Sign in with Warwick").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(needsLogin = false, signedIn = true,
            notice = SyncNotice(3, "Older messages couldn't be loaded", FeedKind.MESSAGES, olderMessages = true)) }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(1, older); assertEquals(1, feeds); assertEquals(0, full) }
    }
}
