package uk.ac.warwick.plus

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.material3.MaterialTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.*
import uk.ac.warwick.plus.ui.*
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking

class AppearanceUiTest {
    @get:Rule val compose = createComposeRule()
    private fun fixture(): TimetableState {
        val tomorrow = ZonedDateTime.now(WarwickZone).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        val event = EventEntity().apply {
            id = "theme-class"; title = "EX101L"; module = "EX101"; moduleName = "Compiler Design"
            location = "Example lecture theatre"; startMillis = tomorrow.toInstant().toEpochMilli()
            endMillis = tomorrow.plusHours(1).toInstant().toEpochMilli()
        }
        val assignment = CourseworkEntity().apply {
            id = "theme-assignment"; title = "Example assignment"; dueMillis = tomorrow.plusDays(2).toInstant().toEpochMilli()
        }
        return TimetableState(name = "Avery Smith", signedIn = true, lastSynced = System.currentTimeMillis(), events = listOf(event),
            coursework = CourseworkState(listOf(assignment), System.currentTimeMillis()))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(400) // Let the platform ripple finish before capturing static theme colours.
        val instrument = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir, "ui-06-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }; bitmap.recycle()
    }
    private fun settings() {
        compose.onNodeWithTag("more-tab").performClick()
        compose.onNodeWithTag("appearance-settings").performScrollTo().performClick()
    }
    private fun waitForBackground(appearance: Appearance) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("background-ready-${appearance.theme.id}-${appearance.texture}").fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun settingsSwitchAllFiveColoursWithoutRefreshingAndRetainChoiceAcrossPages() {
        var appearance by mutableStateOf(Appearance())
        var refreshes = 0
        compose.setContent { PlusTheme(appearance, { appearance = it }) { PlusScreen(fixture(), { refreshes++ }, {}) } }
        ColourTheme.entries.forEach { theme ->
            settings()
            compose.onNodeWithTag("theme-${theme.id}").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText("Home").performClick()
            waitForBackground(Appearance(theme))
            compose.onNodeWithTag("home-greeting").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals("Greeting must use the selected palette on the gradient", theme.palette().text, layouts.first().layoutInput.style.color)
            }
            compose.onNodeWithText("Compiler Design").assertIsDisplayed()
            capture("home-${theme.id}")
        }
        settings()
        compose.onNodeWithTag("fine-texture").performScrollTo().performClick().assertIsOn()
        waitForBackground(Appearance(ColourTheme.ROSEWOOD, true))
        capture("settings")
        compose.onNodeWithTag("appearance-list").performTouchInput { swipeDown() }
        assertEquals(0, refreshes)
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithTag("appearance-settings").assertExists()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithTag("next-class-card").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
        assertEquals(Appearance(ColourTheme.ROSEWOOD, true), appearance)
    }
    @Test fun fixedColourIgnoresSystemNightModeAndLargeFontSettingsRemainUsable() {
        var systemNight by mutableStateOf(false)
        var appearance by mutableStateOf(Appearance(ColourTheme.LAKE))
        var observed = ColourTheme.LAKE.palette().background
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemNight) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, 1.8f)) {
                PlusTheme(appearance, { appearance = it }) {
                    observed = MaterialTheme.colorScheme.background
                    PlusScreen(fixture(), {}, {})
                }
            }
        }
        compose.runOnIdle { systemNight = true }
        compose.runOnIdle { assertEquals(ColourTheme.LAKE.palette().background, observed) }
        settings()
        compose.onNodeWithTag("theme-rosewood").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("fine-texture").performScrollTo().performClick().assertIsOn()
        waitForBackground(Appearance(ColourTheme.ROSEWOOD, true))
        capture("settings-large-font")
    }
    @Test fun isolatedPreferencesPersistBothChoicesAndHandleUnknownTheme() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val name = "appearance-test-${UUID.randomUUID()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            val store = AppearancePreferences(preferences)
            store.update(Appearance(ColourTheme.HEATHER, true))
            assertEquals(Appearance(ColourTheme.HEATHER, true), AppearancePreferences(preferences).state.value)
            preferences.edit().putString("colour_theme", "unknown").commit()
            assertEquals(ColourTheme.FOREST, AppearancePreferences(preferences).state.value.theme)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun backgroundCacheReusesBitmapsEvictsOldEntriesAndKeepsTextureDeterministic() = runBlocking {
        val cache = ThemeBackgroundCache()
        val forest = backgroundKey(Appearance(), .45f)
        val image = cache.get(forest)
        repeat(10) { assertSame(image, cache.get(forest)) }
        cache.get(backgroundKey(Appearance(ColourTheme.LAKE), .45f))
        cache.get(backgroundKey(Appearance(ColourTheme.HEATHER), .45f))
        assertNotSame(image, cache.get(forest))
        val textured = forest.copy(texture = true)
        val first = cache.get(textured)
        assertNotEquals(image.getPixel(100,100), first.getPixel(100,100))
        assertTrue(first.sameAs(ThemeBackgroundCache().get(textured)))
        assertFalse(first.getPixel(10,10) == first.getPixel(400,800))
        ColourTheme.entries.forEach { theme ->
            val background = cache.get(backgroundKey(Appearance(theme, true), .45f))
            val palette = theme.palette()
            listOf("body" to palette.text, "secondary" to palette.muted, "link" to palette.accent, "error" to palette.error)
                .forEach { (label, foreground) ->
                    for (y in 0 until background.height step 32) for (x in 0 until background.width step 32) {
                        assertTrue("${theme.id} $label on native gradient must remain readable",
                            ColorUtils.calculateContrast(foreground.toArgb(), background.getPixel(x,y)) >= 4.5)
                    }
                }
        }
        println("Background bitmap: ${first.width}x${first.height}; bytes=${first.allocationByteCount}; cache reuse and eviction verified")
    }
}
