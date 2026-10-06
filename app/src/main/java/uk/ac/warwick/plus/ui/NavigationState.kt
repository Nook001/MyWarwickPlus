package uk.ac.warwick.plus.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

enum class AppTab(val key: String, val label: String, val tag: String) {
    HOME("home", AppLabels.HOME, "tab-home"),
    CLASSES("classes", AppLabels.CLASSES, "schedule-tab"),
    TASKS("tasks", AppLabels.TASKS, "tab-coursework"),
    ME("me", AppLabels.ME, "more-tab");

    companion object {
        fun restore(value: Any?): AppTab = when (value) {
            0, "home" -> HOME
            1, "classes", "schedule" -> CLASSES
            2, "tasks", "coursework" -> TASKS
            3, "me", "more" -> ME
            else -> HOME
        }
        val saver = Saver<AppTab, Any>(save = { it.key }, restore = { restore(it) })
    }
}

enum class CourseworkFilter(val key: String, val label: String) {
    UPCOMING("upcoming", AppLabels.UPCOMING), PAST("past", AppLabels.PAST);

    companion object {
        fun restore(value: Any?): CourseworkFilter =
            if (value is String && value.equals("past", ignoreCase = true)) PAST else UPCOMING
        val saver = Saver<CourseworkFilter, Any>(save = { it.key }, restore = { restore(it) })
    }
}

sealed interface MeRoute {
    data object Overview : MeRoute
    data object Settings : MeRoute
    data object DeveloperTools : MeRoute
    data class Feed(val kind: FeedKind) : MeRoute

    val title: String get() = when (this) {
        Overview -> AppLabels.ME
        Settings -> AppLabels.SETTINGS
        DeveloperTools -> AppLabels.DEVELOPER_TOOLS
        is Feed -> kind.label
    }

    fun savedValues(): List<String> = when (this) {
        Overview -> listOf("overview")
        Settings -> listOf("settings")
        DeveloperTools -> listOf("developer")
        is Feed -> listOf("feed", kind.key.toString())
    }

    companion object {
        fun restore(values: List<String>): MeRoute = when (values.firstOrNull()) {
            "settings" -> Settings
            "developer" -> DeveloperTools
            "feed" -> savedFeed(values.getOrNull(1))?.let(::Feed) ?: Overview
            else -> Overview
        }
        val saver = listSaver<MeRoute, String>(save = { it.savedValues() }, restore = { restore(it) })
    }
}

sealed interface DetailSelection {
    data object None : DetailSelection
    data class Class(val id: String) : DetailSelection
    data class Task(val id: String) : DetailSelection
    data class Feed(val kind: FeedKind, val id: String) : DetailSelection

    fun savedValues(): List<String> = when (this) {
        None -> listOf("none")
        is Class -> listOf("class", id)
        is Task -> listOf("task", id)
        is Feed -> listOf("feed", kind.key.toString(), id)
    }

    companion object {
        fun restore(values: List<String>): DetailSelection {
            val id = values.getOrNull(if (values.firstOrNull() == "feed") 2 else 1)
                ?.takeIf { it.isNotBlank() } ?: return None
            return when (values.firstOrNull()) {
                "class" -> Class(id)
                "task" -> Task(id)
                "feed" -> savedFeed(values.getOrNull(1))?.let { Feed(it, id) } ?: None
                else -> None
            }
        }
        val saver = listSaver<DetailSelection, String>(save = { it.savedValues() }, restore = { restore(it) })
    }
}

private fun savedFeed(key: String?): FeedKind? = FeedKind.entries.firstOrNull { it.key.toString() == key }

// A process restart can briefly expose an empty, still-loading cache. Validate after it is known.
internal fun DetailSelection.validated(state: TimetableState, route: MeRoute): DetailSelection = when (this) {
    DetailSelection.None -> this
    is DetailSelection.Class -> if ((state.lastSynced != null || !state.busy) && state.events.none { it.id == id }) DetailSelection.None else this
    is DetailSelection.Task -> if ((state.coursework.lastSynced != null || !state.busy) && state.coursework.entries.none { it.id == id }) DetailSelection.None else this
    is DetailSelection.Feed -> if (route != MeRoute.Feed(kind) ||
        (state.feed(kind).lastSynced != null || !state.busy) && state.feed(kind).entries.none { it.id == id }) DetailSelection.None else this
}
