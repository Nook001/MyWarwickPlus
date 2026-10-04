package uk.ac.warwick.plus

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.CourseworkEntity
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.ui.*
import java.time.ZonedDateTime

class HomeLayoutUiTest {
    @get:Rule val compose = createComposeRule()
    private val tomorrow = ZonedDateTime.now(WarwickZone).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
    private fun fixture(long: Boolean = false): TimetableState {
        val event = EventEntity().apply {
            id = "home-example"; title = "EX101L"; module = "EX101"
            moduleName = if (long) "A deliberately long module name for narrow screens and larger fonts" else "Compiler Design"
            location = if (long) "A deliberately long teaching location" else "Example lecture theatre"
            startMillis = tomorrow.toInstant().toEpochMilli(); endMillis = tomorrow.plusHours(1).toInstant().toEpochMilli()
        }
        val assignment = CourseworkEntity().apply {
            id = "home-deadline"; title = "Example assignment"; dueMillis = tomorrow.plusDays(2).toInstant().toEpochMilli()
        }
        return TimetableState(name = "Avery Smith", signedIn = true, events = listOf(event), lastSynced = System.currentTimeMillis(),
            coursework = CourseworkState(listOf(assignment), System.currentTimeMillis()))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir, "ui-05-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    @Test fun compactHomeShowsDeadlineOnFirstScreenAndRetainsAlignedDetailsInBothThemes() {
        var dark by mutableStateOf(false)
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                PlusTheme { PlusScreen(fixture(), {}, {}) }
            }
        }
        compose.onNodeWithTag("home-greeting").assertTextContains(", Avery", substring = true)
        compose.onNodeWithText("Smith", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        compose.onNodeWithText("View messages").assertDoesNotExist()
        compose.onNodeWithText(dateLabel(atWarwick(System.currentTimeMillis()).toLocalDate())).assertDoesNotExist()
        compose.onNodeWithText("NEXT").assertIsDisplayed()
        compose.onNodeWithTag("next-when", useUnmergedTree = true).assertTextEquals("Tomorrow · 10:00 – 11:00")
        compose.onNodeWithText("Example assignment").assertIsDisplayed()
        val name = compose.onNodeWithTag("next-name", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val code = compose.onNodeWithTag("next-code", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Name and code must share a row", code.top < name.bottom && name.top < code.bottom)
        val location = compose.onNodeWithTag("next-location", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val details = compose.onNodeWithText("View class details").getUnclippedBoundsInRoot()
        assertTrue("Location and details must share a row", details.top < location.bottom && location.top < details.bottom)
        assertTrue("Ordinary next card should leave room for today's data and deadlines",
            compose.onNodeWithTag("next-class-card").getUnclippedBoundsInRoot().let { it.bottom - it.top <= 160.dp })
        capture("home-light")
        compose.runOnIdle { dark = true }
        compose.onNodeWithText("Example assignment").assertIsDisplayed()
        capture("home-dark")
        compose.onNodeWithText("View class details").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
        compose.onNodeWithText("Close details").performScrollTo().performClick()
        compose.onNodeWithTag("next-class-card").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
    }
    @Test fun narrowLargeFontHomeKeepsCodeAndDetailActionAndRevealsFullTitleOnTap() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.8f)) {
                PlusTheme { Box(Modifier.width(320.dp)) { PlusScreen(fixture(long = true), {}, {}) } }
            }
        }
        compose.onNodeWithTag("next-code", useUnmergedTree = true).assertTextEquals("EX101L").assertIsDisplayed()
        compose.onNodeWithText("View class details").assertIsDisplayed()
        capture("home-large-font")
        compose.onNodeWithText("View class details").performClick()
        compose.onNode(hasText("A deliberately long module name for narrow screens and larger fonts") and
            hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
        compose.onNodeWithText("Close details").performScrollTo().assertIsDisplayed()
    }
}
