package uk.ac.warwick.plus.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

enum class AppTab(val key: String, val labelRes: Int, val tag: String, val titleRes: Int = labelRes) {
    HOME("home", AppLabels.HOME, "tab-home"),
    CLASSES("classes", AppLabels.CLASSES, "schedule-tab"),
    TASKS("tasks", AppLabels.TASKS, "tab-coursework"),
    MESSAGES("messages", AppLabels.INBOX, "tab-messages", AppLabels.MESSAGES),
    ME("me", AppLabels.ME, "more-tab");

    companion object {
        fun restore(key: String?): AppTab = entries.firstOrNull { it.key == key } ?: HOME
        val saver = Saver<AppTab, String>(save = { it.key }, restore = { restore(it) })
    }
}

enum class CourseworkFilter(val key: String, val labelRes: Int) {
    UPCOMING("upcoming", AppLabels.UPCOMING), PAST("past", AppLabels.PAST);

    companion object {
        fun restore(key: String?): CourseworkFilter = entries.firstOrNull { it.key == key } ?: UPCOMING
        val saver = Saver<CourseworkFilter, String>(save = { it.key }, restore = { restore(it) })
    }
}

sealed interface MeRoute {
    data object Overview : MeRoute
    data object Settings : MeRoute
    data class Feed(val kind: FeedKind) : MeRoute

    val titleRes: Int get() = when (this) {
        Overview -> AppLabels.ME
        Settings -> AppLabels.SETTINGS
        is Feed -> kind.labelRes
    }

    fun savedValues(): List<String> = when (this) {
        Overview -> listOf("overview")
        Settings -> listOf("settings")
        is Feed -> listOf("feed", kind.key.toString())
    }

    companion object {
        fun restore(values: List<String>): MeRoute = when (values.firstOrNull()) {
            "settings" -> Settings
            "feed" -> savedFeed(values.getOrNull(1))?.takeUnless { it == FeedKind.MESSAGES }?.let(::Feed) ?: Overview
            else -> Overview
        }
        val saver = listSaver<MeRoute, String>(save = { it.savedValues() }, restore = { restore(it) })
    }
}

sealed interface DetailSelection {
    data object None : DetailSelection
    data class Class(val id: String) : DetailSelection
    data class Task(val id: String) : DetailSelection
    data class CampusEvent(val id: String) : DetailSelection
    data class Feed(val kind: FeedKind, val id: String) : DetailSelection

    fun savedValues(): List<String> = when (this) {
        None -> listOf("none")
        is Class -> listOf("class", id)
        is Task -> listOf("task", id)
        is CampusEvent -> listOf("campus-event", id)
        is Feed -> listOf("feed", kind.key.toString(), id)
    }

    companion object {
        fun restore(values: List<String>): DetailSelection {
            val id = values.getOrNull(if (values.firstOrNull() == "feed") 2 else 1)
                ?.takeIf { it.isNotBlank() } ?: return None
            return when (values.firstOrNull()) {
                "class" -> Class(id)
                "task" -> Task(id)
                "campus-event" -> CampusEvent(id)
                "feed" -> savedFeed(values.getOrNull(1))?.let { Feed(it, id) } ?: None
                else -> None
            }
        }
        val saver = listSaver<DetailSelection, String>(save = { it.savedValues() }, restore = { restore(it) })
    }
}

private fun savedFeed(key: String?): FeedKind? = FeedKind.entries.firstOrNull { it.key.toString() == key }

// A process restart can briefly expose an empty, still-loading cache. Validate after it is known.
internal fun DetailSelection.Feed.visibleOn(tab: AppTab, route: MeRoute): Boolean =
    (kind == FeedKind.MESSAGES && (tab == AppTab.HOME || tab == AppTab.MESSAGES)) ||
        (kind != FeedKind.MESSAGES && tab == AppTab.ME && route == MeRoute.Feed(kind))

internal fun DetailSelection.validated(state: TimetableState, route: MeRoute, tab: AppTab): DetailSelection = when (this) {
    DetailSelection.None -> this
    is DetailSelection.Class -> if ((state.lastSynced != null || !state.busy) && state.events.none { it.id == id }) DetailSelection.None else this
    is DetailSelection.Task -> if ((state.coursework.lastSynced != null || !state.busy) && state.coursework.entries.none { it.id == id }) DetailSelection.None else this
    is DetailSelection.CampusEvent -> if (tab != AppTab.HOME ||
        (state.service(uk.ac.warwick.plus.data.ServiceKind.EVENTS).lastSynced != null || !state.busy) &&
        state.service(uk.ac.warwick.plus.data.ServiceKind.EVENTS).events.none { it.id == id }) DetailSelection.None else this
    is DetailSelection.Feed -> if (!visibleOn(tab, route) ||
        (state.feed(kind).lastSynced != null || !state.busy) && state.feed(kind).entries.none { it.id == id }) DetailSelection.None else this
}
