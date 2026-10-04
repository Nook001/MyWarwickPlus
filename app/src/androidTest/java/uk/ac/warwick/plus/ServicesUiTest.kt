package uk.ac.warwick.plus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*

class ServicesUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir, "ui-04-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }; bitmap.recycle()
    }
    private fun state(kind: FeedKind, entry: FeedEntry? = null, more: Boolean = false) = TimetableState(
        name = "Example student", accountCode = "example-user", lastSynced = 1L, signedIn = true,
        feeds = mapOf(kind to FeedState(entries = listOfNotNull(entry), lastSynced = 1L, hasMore = more,
            description = "No current checkouts or holds", url = "https://warwick.ac.uk/services/library/account", webReadMillis = 1)))
    private fun navigate(kind: FeedKind) {
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNode(hasText(kind.label) and hasClickAction()).performScrollTo().performClick()
    }
    @Test fun signOutRequiresExplicitConfirmationAndMoreShowsSavedAccount() {
        var calls = 0
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.MESSAGES), {}, {}, onSignOut = { calls++ }) } }
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithText("example-user").assertIsDisplayed()
        capture("more")
        compose.onNodeWithText("Sign out of this app").performClick()
        assertEquals(0,calls)
        compose.onNodeWithText("Cancel").performClick(); assertEquals(0,calls)
        compose.onNodeWithText("Sign out of this app").performClick()
        compose.onNodeWithText("Sign out").performClick(); assertEquals(1,calls)
    }
    @Test fun loggedOutMoreStillOffersServicesAndExplicitLogin() {
        var login = false
        compose.setContent { PlusTheme { PlusScreen(TimetableState(needsLogin=true), {}, { login = true }) } }
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithText("Sign in with Warwick").performClick(); assertTrue(login)
        compose.onNode(hasText("Library") and hasClickAction()).assertExists()
    }
    @Test fun messagesArePlainTextAndOnlyOpenUrlsOnUserClick() {
        val entry = FeedEntry().apply { feed=3; id="example-message"; title="Example service update"; provider="Example service"
            text="<p>Hello <b>student</b><img src='https://invalid.example/image'></p>"; html=true; dateMillis=100
            url="https://www.warwicksu.com/example" }
        var opened: String? = null
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.MESSAGES,entry), {}, {}, onExternalLink = { opened=it }) } }
        navigate(FeedKind.MESSAGES)
        compose.onNodeWithText("Hello student",substring=true).assertIsDisplayed()
        assertNull(opened); capture("messages")
        compose.onNodeWithText("Example service update").performClick()
        compose.onNodeWithTag("feed-details").assertIsDisplayed()
        capture("message-details")
        compose.onNodeWithText("Open source website").performScrollTo().performClick()
        assertEquals(entry.url,opened)
    }
    @Test fun olderPageIsManualAndSearchKeepsLoadMoreAvailable() {
        val entry = FeedEntry().apply { feed=3; id="example"; title="Example message" }
        var calls=0
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.MESSAGES,entry,true), {}, {}, onMoreMessages={ calls++ }) } }
        navigate(FeedKind.MESSAGES); assertEquals(0,calls)
        compose.onNodeWithText("Search messages").performTextInput("nothing matches")
        compose.onNodeWithText("No matches").assertIsDisplayed()
        compose.onNodeWithText("Load older messages").performScrollTo().performClick(); assertEquals(1,calls)
    }
    @Test fun moduleMetadataAndMoodleLinkAreAvailable() {
        val entry=FeedEntry().apply { feed=5; id="42"; title="Example module"; moduleCode="EX101"; academicYear="2026/27"
            url="https://moodle.warwick.ac.uk/course/view.php?id=42"; announcementCount=2 }
        var opened: String?=null
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.MODULES,entry), {}, {}, onExternalLink={ opened=it }) } }
        navigate(FeedKind.MODULES)
        compose.onNodeWithText("EX101 · 2026/27").assertIsDisplayed(); capture("modules")
        compose.onNodeWithText("Example module").performClick()
        compose.onNodeWithText("Open module in Moodle").performScrollTo().performClick(); assertEquals(entry.url,opened)
    }
    @Test fun emptyLibraryDisplaysServerSummaryAndAccountLink() {
        var opened: String?=null
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.LIBRARY), {}, {}, onExternalLink={ opened=it }) } }
        navigate(FeedKind.LIBRARY)
        compose.onNodeWithText("No current checkouts or holds").assertIsDisplayed(); capture("library")
        compose.onNodeWithText("Open library website").performScrollTo().performClick()
        assertEquals("https://warwick.ac.uk/services/library/account",opened)
    }
    @Test fun swipeDownRequestsRefreshFromScrollableContent() {
        val calls=java.util.concurrent.atomic.AtomicInteger()
        compose.setContent { PlusTheme { PlusScreen(TimetableState(lastSynced=1L), { calls.incrementAndGet() }, {}) } }
        compose.onNodeWithTag("home-list").performScrollToIndex(0)
        compose.onNodeWithTag("home-list").performTouchInput { swipeDown(startY=10f,endY=height-10f,durationMillis=1_000) }
        capture("swipe-after")
        compose.waitUntil(5_000) { calls.get()>0 }
    }
    @Test fun pullRefreshUsesCurrentPageAndDoesNotRequestOlderMessages() {
        var globalCalls = 0
        var feed: FeedKind? = null
        var olderCalls = 0
        compose.setContent { PlusTheme { PlusScreen(state(FeedKind.MESSAGES), { globalCalls++ }, {},
            onFeedRefresh = { feed = it }, onMoreMessages = { olderCalls++ }) } }
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        compose.onNodeWithTag("home-list").performTouchInput { swipeDown(startY=10f, endY=height-10f, durationMillis=1_000) }
        compose.waitUntil(5_000) { globalCalls == 1 }
        assertEquals(1, globalCalls); assertNull(feed)
        navigate(FeedKind.MESSAGES)
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        compose.onNodeWithTag("feed-list").performTouchInput { swipeDown(startY=10f, endY=height-10f, durationMillis=1_000) }
        compose.waitUntil(5_000) { feed == FeedKind.MESSAGES }
        assertEquals(1, globalCalls); assertEquals(FeedKind.MESSAGES, feed); assertEquals(0, olderCalls)
    }
    @Test fun largeFontLongTitlesAndFiltersRemainUsable() {
        val entry=CourseworkEntity().apply { id="long"; title="A deliberately long assignment title that must remain readable at a large system font size"; dueMillis=1L }
        compose.setContent { PlusTheme {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,1.8f)) {
                PlusScreen(TimetableState(lastSynced=1L,coursework=CourseworkState(listOf(entry),1L)), {}, {})
            }
        } }
        compose.onNodeWithText("Coursework",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Search coursework").performTextInput("deliberately")
        compose.onNodeWithText("Past").performScrollTo().performClick()
        compose.onNodeWithTag("coursework-list").performScrollToNode(hasText(entry.title))
        compose.onNodeWithText(entry.title).assertIsDisplayed(); capture("large-font")
        compose.onNodeWithText(entry.title).performClick()
        compose.onNodeWithText("Close coursework details").performScrollTo().assertIsDisplayed()
    }
}
