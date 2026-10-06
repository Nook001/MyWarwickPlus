package uk.ac.warwick.plus.ui
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusScreen(state: TimetableState, onRefresh: () -> Unit, onLogin: () -> Unit,
    probe: ((ProbeEndpoint) -> ProbeResult)? = null, onCourseworkLink: ((String) -> Unit)? = null,
    onSignOut: () -> Unit = {}, onFeedRefresh: ((FeedKind) -> Unit)? = null,
    onMoreMessages: () -> Unit = {}, onExternalLink: ((String) -> Unit)? = null,
    onNoticeConsumed: (Long) -> Unit = {}, onResourceRefresh: ((SyncResource) -> Unit)? = null) {
    var tab by rememberSaveable(stateSaver = AppTab.saver) { mutableStateOf(AppTab.HOME) }
    var meRoute by rememberSaveable(stateSaver = MeRoute.saver) { mutableStateOf<MeRoute>(MeRoute.Overview) }
    var detail by rememberSaveable(stateSaver = DetailSelection.saver) { mutableStateOf<DetailSelection>(DetailSelection.None) }
    var courseworkFilter by rememberSaveable(stateSaver = CourseworkFilter.saver) { mutableStateOf(CourseworkFilter.UPCOMING) }
    val selectedId = (detail as? DetailSelection.Class)?.id
    val courseworkId = (detail as? DetailSelection.Task)?.id
    val feedEntryId = (detail as? DetailSelection.Feed)?.id
    val route = (meRoute as? MeRoute.Feed)?.kind
    val showProbe = meRoute == MeRoute.DeveloperTools
    val showAppearance = meRoute == MeRoute.Settings
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val currentState = rememberUpdatedState(state)
    val refreshResource: (SyncResource) -> Unit = remember(onResourceRefresh, onFeedRefresh, onRefresh) {
        { resource ->
            if (onResourceRefresh != null) onResourceRefresh(resource)
            else resource.feed?.let { onFeedRefresh?.invoke(it) ?: onRefresh() } ?: onRefresh()
        }
    }
    val recoverResource: (SyncResource) -> Unit = remember(currentState, onMoreMessages, refreshResource) {
        { resource ->
            val messages = currentState.value.feed(FeedKind.MESSAGES)
            if (resource == SyncResource.MESSAGES && messages.olderPageFailed && messages.hasMore && messages.entries.isNotEmpty()) onMoreMessages()
            else refreshResource(resource)
        }
    }
    val dispatchRecovery: (RecoveryAction) -> Unit = { action ->
        when (val resolved = action.resolve(currentState.value)) {
            RecoveryAction.SignIn -> onLogin()
            RecoveryAction.RefreshAll -> onRefresh()
            RecoveryAction.OlderMessages -> onMoreMessages()
            is RecoveryAction.Refresh -> refreshResource(resolved.resource)
            null -> Unit
        }
    }
    val currentRecovery = rememberUpdatedState(dispatchRecovery)
    val consumeNotice = rememberUpdatedState(onNoticeConsumed)
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice ->
            try {
                val result = snackbar.showSnackbar(notice.message,
                    actionLabel = if (currentState.value.needsLogin) AppActions.SIGN_IN else AppActions.RETRY,
                    duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    currentRecovery.value(notice.action)
                }
            } finally { consumeNotice.value(notice.id) }
        }
    }
    val launchBrowser = rememberBrowserOpener(onExternalLink)
    val openLink: (String) -> Unit = { raw ->
        if (!launchBrowser(raw)) scope.launch { snackbar.showSnackbar("Couldn't open this link. Try again.") }
    }
    var previousAccount by rememberSaveable { mutableStateOf(state.accountCode) }
    LaunchedEffect(state.accountCode, state.busy, state.signingOut) {
        // Empty identity during initial cache loading is not an account switch.
        if (state.accountCode.isNotBlank() || !state.busy || state.signingOut) {
            if (previousAccount.isNotBlank() && previousAccount != state.accountCode) {
                detail = DetailSelection.None
                meRoute = MeRoute.Overview
                courseworkFilter = CourseworkFilter.UPCOMING
            }
            previousAccount = state.accountCode
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }
    val conflicts = remember(state.events) { conflictingEventIds(state.events) }
    val timetableRecovery = state.recovery(SyncResource.TIMETABLE)
    val courseworkRecovery = state.recovery(SyncResource.COURSEWORK)
    val home = remember(state.events, state.lastSynced, state.busy, state.coursework,
        timetableRecovery, courseworkRecovery) { state.homePage() }
    val schedule = remember(state.events, state.lastSynced, state.busy, state.needsLogin, state.message) {
        state.schedulePage()
    }
    val today = remember(now) { atWarwick(now).toLocalDate() }
    val greeting = greeting(state.name, now)
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var followToday by rememberSaveable { mutableStateOf(true) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val scheduleListState = rememberLazyListState()
    val scheduleDate = remember(followToday, today, selectedDay) {
        if (followToday) today else LocalDate.ofEpochDay(selectedDay)
    }
    val scheduleScrolled by remember { derivedStateOf { scheduleListState.firstVisibleItemIndex > 0 || scheduleListState.firstVisibleItemScrollOffset > 0 } }
    val chooseScheduleDate: (LocalDate) -> Unit = { date ->
        selectedDay = date.toEpochDay(); followToday = date == today
        scheduleListState.requestScrollToItem(0)
    }
    LaunchedEffect(state.events, state.coursework, state.feeds, state.busy, detail, meRoute) {
        detail = detail.validated(state, meRoute)
    }
    LaunchedEffect(probe, meRoute) {
        if (probe == null && meRoute == MeRoute.DeveloperTools) meRoute = MeRoute.Overview
    }
    BackHandler(tab != AppTab.HOME && detail == DetailSelection.None && !showProbe) {
        if (tab == AppTab.ME && meRoute != MeRoute.Overview) meRoute = MeRoute.Overview
        else tab = AppTab.HOME
    }

    Box(Modifier.fillMaxSize()) {
        ThemeBackground(Modifier.matchParentSize())
        Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                AppNavigation(tab, state.needsLogin && state.hasSavedData, state.logoutFailed) { selected ->
                    tab = selected
                    if (selected == AppTab.ME) { meRoute = MeRoute.Overview; detail = DetailSelection.None }
                }
            }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                AppPageHeader(
                    title = when (tab) {
                        AppTab.HOME -> greeting
                        AppTab.ME -> meRoute.title
                        else -> tab.label
                    },
                    subtitle = if (state.busy) state.syncProgress?.description ?: AppLabels.BRAND else AppLabels.BRAND,
                    titleModifier = if (tab == AppTab.HOME) Modifier.testTag("home-greeting") else Modifier) {
                    if (tab == AppTab.CLASSES) {
                        if (scheduleDate != today || scheduleScrolled) TextButton(onClick = { chooseScheduleDate(today) },
                            modifier = Modifier.testTag("schedule-today")) { Text(AppLabels.TODAY) }
                        IconButton(onClick = { showDatePicker = true }, modifier = Modifier.testTag("schedule-date-picker")) {
                            Icon(CalendarPickerIcon, contentDescription = "Choose date", modifier = Modifier.size(20.dp))
                        }
                    }
                    if (tab == AppTab.ME && (route != null || showAppearance)) TextButton(onClick = {
                        meRoute = MeRoute.Overview; detail = DetailSelection.None
                    }) { Text(AppActions.BACK) }
                }
                SyncProgressBar(state.syncProgress)
                if (tab == AppTab.ME && showAppearance && !state.signingOut) AppearanceContent() else {
                    PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                        onRefresh = {
                            if (!state.signingOut && !state.logoutFailed) {
                                if (tab == AppTab.ME && route != null && onFeedRefresh != null) onFeedRefresh(route) else onRefresh()
                            }
                        }, indicator = {}, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
                        when {
                            state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signing out…") }
                            tab == AppTab.ME -> {
                                if (route == null) MoreContent(state, onLogin, onSignOut, { meRoute = MeRoute.Feed(it) }, openLink,
                                    if (probe != null && !state.logoutFailed) ({ meRoute = MeRoute.DeveloperTools }) else null,
                                    onSettings = { meRoute = MeRoute.Settings }, onResourceRefresh = recoverResource)
                                else FeedContent(route, state.feed(route), state.busy, state.needsLogin,
                                    { recoverResource(SyncResource.forFeed(route)) }, onMoreMessages, { detail = DetailSelection.Feed(route, it.id) }, openLink, onLogin)
                            }
                            !state.hasSavedData && state.busy && !state.signedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Loading your saved data…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            !state.hasSavedData && state.logoutFailed -> Column(Modifier.padding(Spacing.page)) {
                                Text(state.message.orEmpty())
                                Button(onClick = onSignOut) { Text(AppActions.RETRY_SIGN_OUT) }
                            }
                            !state.hasSavedData && state.needsLogin -> Welcome(onLogin)
                            else -> {
                                when (tab) {
                                AppTab.HOME -> HomeContent(home, today, now,
                                    { detail = DetailSelection.Class(it.id) }, { detail = DetailSelection.Task(it.id) },
                                    { courseworkFilter = CourseworkFilter.UPCOMING; tab = AppTab.TASKS },
                                    { courseworkFilter = CourseworkFilter.PAST; tab = AppTab.TASKS }, onLogin, recoverResource, openLink)
                                AppTab.TASKS -> CourseworkContent(state.coursework, now, state.busy,
                                    feedback = { ResourceRecoveryRow(courseworkRecovery, onLogin) { recoverResource(SyncResource.COURSEWORK) } },
                                    showFeedback = state.needsLogin || state.coursework.message != null,
                                    filterOverride = courseworkFilter, onFilterChanged = { courseworkFilter = it }) { detail = DetailSelection.Task(it.id) }
                                AppTab.CLASSES -> ScheduleContent(schedule, today, now, scheduleDate, scheduleListState,
                                    feedback = { ResourceRecoveryRow(timetableRecovery, onLogin) { recoverResource(SyncResource.TIMETABLE) } }) { detail = DetailSelection.Class(it.id) }
                                AppTab.ME -> Unit // Handled before the common data states above.
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (tab == AppTab.CLASSES && showDatePicker && !state.signingOut) ScheduleDateDialog(scheduleDate,
        onDismiss = { showDatePicker = false }, onDate = { chooseScheduleDate(it); showDatePicker = false })
    if (!state.signingOut) state.events.firstOrNull { it.id == selectedId }?.let { event ->
        EventDetails(event, event.id in conflicts, onDismiss = { detail = DetailSelection.None }, onOpen = onExternalLink)
    }
    if (!state.signingOut) state.coursework.entries.firstOrNull { it.id == courseworkId }?.let { entry ->
        CourseworkDetails(entry, { detail = DetailSelection.None }, onCourseworkLink ?: onExternalLink)
    }
    if (!state.signingOut && route != null && (detail as? DetailSelection.Feed)?.kind == route) {
        state.feed(route).entries.firstOrNull { it.id == feedEntryId }?.let {
            FeedDetails(route, it, onExternalLink, { detail = DetailSelection.None })
        }
    }
    if (showProbe && probe != null && !state.signingOut) ApiProbeSheet(probe, onLogin, { meRoute = MeRoute.Overview })
}
