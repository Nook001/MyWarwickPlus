package uk.ac.warwick.plus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate

/** UI transitions and saved keys, independent of network clients and cache ownership. */
@Stable
internal class PlusNavigator(today: LocalDate) {
    var tab by mutableStateOf(AppTab.HOME)
    var meRoute by mutableStateOf<MeRoute>(MeRoute.Overview)
    var detail by mutableStateOf<DetailSelection>(DetailSelection.None)
    var courseworkFilter by mutableStateOf(CourseworkFilter.UPCOMING)
    var selectedDay by mutableLongStateOf(today.toEpochDay())
    var followToday by mutableStateOf(true)
    var showDatePicker by mutableStateOf(false)

    fun select(next: AppTab) {
        tab = next
        if (next == AppTab.ME) {
            meRoute = MeRoute.Overview
            detail = DetailSelection.None
        }
    }

    fun openTasks(filter: CourseworkFilter) {
        courseworkFilter = filter
        tab = AppTab.TASKS
    }

    fun back() {
        if (tab == AppTab.ME && meRoute != MeRoute.Overview) meRoute = MeRoute.Overview
        else tab = AppTab.HOME
        detail = DetailSelection.None
    }

    fun chooseDate(date: LocalDate, today: LocalDate) {
        selectedDay = date.toEpochDay()
        followToday = date == today
    }

    fun resetForAccountChange() {
        detail = DetailSelection.None
        meRoute = MeRoute.Overview
        courseworkFilter = CourseworkFilter.UPCOMING
        showDatePicker = false
    }

    // Nested lists preserve arbitrary IDs without delimiters; only Bundle-saveable primitives.
    fun savedValues(): List<Any> = listOf(tab.key, meRoute.savedValues(), detail.savedValues(),
        courseworkFilter.key, selectedDay, followToday, showDatePicker)

    companion object {
        fun restore(values: List<Any>): PlusNavigator {
            fun strings(index: Int) = (values.getOrNull(index) as? List<*>)
                ?.filterIsInstance<String>().orEmpty()
            return PlusNavigator(LocalDate.ofEpochDay(0)).apply {
                tab = AppTab.restore(values.getOrNull(0) as? String)
                meRoute = MeRoute.restore(strings(1))
                detail = DetailSelection.restore(strings(2))
                courseworkFilter = CourseworkFilter.restore(values.getOrNull(3) as? String)
                selectedDay = (values.getOrNull(4) as? Long)?.takeIf {
                    it in LocalDate.MIN.toEpochDay()..LocalDate.MAX.toEpochDay()
                } ?: 0
                followToday = values.getOrNull(5) as? Boolean ?: true
                showDatePicker = values.getOrNull(6) as? Boolean ?: false
            }
        }
        val saver = listSaver<PlusNavigator, Any>(save = { it.savedValues() }, restore = ::restore)
    }
}

@Composable
internal fun rememberPlusNavigator(today: LocalDate) =
    rememberSaveable(saver = PlusNavigator.saver) { PlusNavigator(today) }
