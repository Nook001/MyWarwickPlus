package uk.ac.warwick.plus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import uk.ac.warwick.plus.data.EventEntity
import uk.ac.warwick.plus.ui.*
import java.time.*
import java.time.format.DateTimeFormatter

class ScheduleUiTest {
    @get:Rule val compose = createComposeRule()
    private val today get() = atWarwick(System.currentTimeMillis()).toLocalDate()
    private fun event(id: String, date: LocalDate, hour: Int, title: String = "Example course $id", location: String = "Example room") = EventEntity().apply {
        this.id=id; this.title="EX${id}L"; moduleName=title; module="EX$id"; this.location=location
        startMillis=date.atTime(hour,0).atZone(WarwickZone).toInstant().toEpochMilli(); endMillis=startMillis+3_600_000
    }
    private fun fixture(days: Int = 3) = TimetableState(name="Avery Smith",signedIn=true,lastSynced=System.currentTimeMillis(),
        events=(1..days).map { event(it.toString(),today.plusDays(it.toLong()),10) })
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(400)
        val instrument=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap=instrument.uiAutomation.takeScreenshot()
        java.io.File(instrument.targetContext.cacheDir,"ui-07-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }; bitmap.recycle()
    }
    private fun chooseDate(date: LocalDate) {
        compose.onNodeWithTag("schedule-date-picker").performClick()
        capture("schedule-date-input")
        compose.onNode(hasSetTextAction()).performTextReplacement(date.format(DateTimeFormatter.ofPattern("ddMMyyyy")))
        compose.onNodeWithTag("schedule-date-confirm").assertIsEnabled().performClick()
    }
    @Test fun emptyTodayAndFollowingDatesUseConsistentGroupsInAllFiveColours() {
        var appearance by mutableStateOf(Appearance())
        val state=fixture().copy(events=listOf(event("1",today.plusDays(1),10,"Compiler Design","Example lecture theatre"),
            event("2",today.plusDays(1),13,"Computer Graphics","Example laboratory"),event("3",today.plusDays(3),11)))
        compose.setContent { PlusTheme(appearance) { PlusScreen(state,{}, {}) } }
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNode(hasText("No classes today") and hasAnyAncestor(hasTestTag("schedule-day-$today"))).assertIsDisplayed()
        compose.onNodeWithText(scheduleDateLabel(today.plusDays(1),today)).assertIsDisplayed()
        compose.onNodeWithTag("schedule-class-${today.plusDays(1)}-1").assertHasClickAction()
        val row=compose.onNodeWithTag("schedule-class-${today.plusDays(1)}-1").getUnclippedBoundsInRoot()
        assertTrue("Ordinary course should stay compact",row.bottom-row.top<=88.dp)
        ColourTheme.entries.forEach { theme ->
            compose.runOnIdle { appearance=Appearance(theme) }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("background-ready-${theme.id}-false").fetchSemanticsNodes().isNotEmpty() }
            capture("schedule-${theme.id}")
        }
        compose.onNodeWithText("Compiler Design").performClick()
        compose.onNodeWithText("CLASS DETAILS").assertIsDisplayed()
        compose.onNode(hasText("Example lecture theatre") and hasAnyAncestor(hasTestTag("class-details"))).assertIsDisplayed()
    }
    @Test fun dateInputSupportsHistoryCancelAndReturningToTodayWithoutRequests() {
        var calls=0
        val historical=today.minusDays(2)
        compose.setContent { PlusTheme { PlusScreen(fixture().copy(events=fixture().events+event("past",historical,14,"Historical seminar")),{calls++},{}) } }
        compose.onNodeWithTag("schedule-tab").performClick()
        chooseDate(historical)
        compose.onNodeWithText(scheduleDateLabel(historical,today)).assertIsDisplayed()
        compose.onNodeWithText("Historical seminar").assertIsDisplayed()
        compose.onNodeWithTag("schedule-date-picker").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement(today.plusDays(9).format(DateTimeFormatter.ofPattern("ddMMyyyy")))
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Historical seminar").assertIsDisplayed()
        compose.onNodeWithTag("schedule-today").performClick()
        compose.onNodeWithText("No classes today").assertIsDisplayed()
        compose.onNodeWithText("Historical seminar").assertDoesNotExist()
        assertEquals(0,calls)
        chooseDate(today.plusDays(9))
        compose.onNodeWithText("No classes on this day").assertIsDisplayed()
        compose.onNodeWithTag("schedule-today").performClick()
        compose.onNodeWithTag("schedule-list").performTouchInput { swipeDown(startY=10f,endY=height-10f,durationMillis=1_000) }
        compose.waitUntil(5_000) { calls>0 }
    }
    @Test fun switchingTabsRefreshingAndRestoringKeepAgendaPosition() {
        var state by mutableStateOf(fixture(22))
        val restoration=StateRestorationTester(compose)
        restoration.setContent { PlusTheme { PlusScreen(state,{}, {}) } }
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithTag("schedule-list").performScrollToIndex(12)
        compose.onNodeWithText("Example course 12").assertIsDisplayed()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithTag("schedule-tab").performClick()
        compose.onNodeWithText("Example course 12").assertIsDisplayed()
        compose.runOnIdle { state=state.copy(events=state.events.filter { it.id != "5" },lastSynced=System.currentTimeMillis()+1) }
        compose.onNodeWithText("Example course 12").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Example course 12").assertIsDisplayed()
        compose.onNodeWithTag("schedule-today").performClick()
        compose.onNodeWithText("No classes today").assertIsDisplayed()
    }
    @Test fun largeFontKeepsTimesConflictAndFullSelectionDataReadable() {
        val now=today.atTime(10,30).atZone(WarwickZone).toInstant().toEpochMilli()
        val title="A deliberately long course name that remains available in full through class details"
        val active=event("active",today,10,title,"A deliberately long building and room name")
        val overlap=event("overlap",today,10,"Overlapping seminar")
        val next=event("next",today,13,"Next seminar")
        var selected: EventEntity? by mutableStateOf(null)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density,1.8f)) {
                PlusTheme(Appearance(ColourTheme.LAKE)) { Surface(Modifier.width(320.dp).fillMaxHeight().windowInsetsPadding(WindowInsets.systemBars)) {
                    ScheduleContent(TimetableState(lastSynced=1L,events=listOf(active,overlap,next)),today,now,today,rememberLazyListState()) { selected=it }
                } }
            }
        }
        compose.onAllNodesWithText("10:00").assertCountEquals(2)
        compose.onAllNodesWithText("Now").assertCountEquals(2)
        compose.onAllNodesWithText("Overlaps another class").assertCountEquals(2)
        capture("schedule-large-font")
        val row=compose.onNodeWithTag("schedule-class-$today-active")
        row.performClick()
        compose.runOnIdle { assertEquals(title,selected?.moduleName) }
        compose.onNodeWithTag("schedule-class-$today-next").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Next").assertIsDisplayed()
    }
}
