package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import uk.ac.warwick.plus.ui.components.*
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
internal fun PlusScreen(state: TimetableState, actions: PlusActions) {
    val navigator = rememberPlusNavigator(atWarwick(System.currentTimeMillis()).toLocalDate())
    val route = (navigator.meRoute as? MeRoute.Feed)?.kind
    val showAppearance = navigator.meRoute == MeRoute.Settings
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val currentState = rememberUpdatedState(state)
    val dispatchRecovery: (RecoveryAction) -> Unit = { action ->
        when (val resolved = action.resolve(currentState.value)) {
            RecoveryAction.SignIn -> actions.signIn()
            RecoveryAction.RefreshAll -> actions.refresh()
            RecoveryAction.OlderMessages -> actions.loadOlderMessages()
            is RecoveryAction.Refresh -> actions.refreshResource(resolved.resource)
            null -> Unit
        }
    }
    val recoverResource: (SyncResource) -> Unit = { resource ->
        val action = if (resource == SyncResource.MESSAGES && currentState.value.feed(FeedKind.MESSAGES).olderPageFailed)
            RecoveryAction.OlderMessages else RecoveryAction.Refresh(resource)
        dispatchRecovery(action)
    }
    val currentRecovery = rememberUpdatedState(dispatchRecovery)
    val consumeNotice = rememberUpdatedState(actions.consumeNotice)
    val resources = androidx.compose.ui.platform.LocalResources.current
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice ->
            try {
                val result = snackbar.showSnackbar(notice.message.resolve(resources),
                    actionLabel = resources.getString(if (currentState.value.needsLogin) AppActions.SIGN_IN else AppActions.RETRY),
                    duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    currentRecovery.value(notice.action)
                }
            } finally { consumeNotice.value(notice.id) }
        }
    }
    val launchBrowser = rememberBrowserOpener(actions.openExternal)
    val openLink: (String) -> Unit = { raw ->
        if (!launchBrowser(raw)) scope.launch { snackbar.showSnackbar(resources.getString(R.string.link_open_failed)) }
    }
    var previousAccount by rememberSaveable { mutableStateOf(state.accountCode) }
    LaunchedEffect(state.accountCode, state.busy, state.signingOut) {
        // Empty identity during initial cache loading is not an account switch.
        if (state.accountCode.isNotBlank() || !state.busy || state.signingOut) {
            if (previousAccount.isNotBlank() && previousAccount != state.accountCode) {
                navigator.resetForAccountChange()
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
                delay(30_000 - now % 30_000)
            }
        }
    }
    val conflicts = remember(state.events) { conflictingEventIds(state.events) }
    val timetableRecovery = state.recovery(SyncResource.TIMETABLE)
    val courseworkRecovery = state.recovery(SyncResource.COURSEWORK)
    val messages = state.feed(FeedKind.MESSAGES)
    val messagesRecovery = state.recovery(SyncResource.MESSAGES)
    val home = remember(state.events, state.lastSynced, state.busy, state.coursework,
        timetableRecovery, courseworkRecovery, messages, messagesRecovery) { state.homePage(conflicts) }
    val schedule = remember(state.events, state.lastSynced, state.busy, state.needsLogin, state.message) {
        state.schedulePage(conflicts)
    }
    val today = remember(now) { atWarwick(now).toLocalDate() }
    val greeting = greeting(state.name, now).render()
    val scheduleListState = rememberLazyListState()
    val scheduleDate = remember(navigator.followToday, today, navigator.selectedDay) {
        if (navigator.followToday) today else LocalDate.ofEpochDay(navigator.selectedDay)
    }
    val scheduleScrolled by remember { derivedStateOf { scheduleListState.firstVisibleItemIndex > 0 || scheduleListState.firstVisibleItemScrollOffset > 0 } }
    val chooseScheduleDate: (LocalDate) -> Unit = { date ->
        navigator.chooseDate(date, today)
        scheduleListState.requestScrollToItem(0)
    }
    LaunchedEffect(state.events, state.coursework, state.feeds, state.busy, navigator.detail, navigator.meRoute, navigator.tab) {
        navigator.detail = navigator.detail.validated(state, navigator.meRoute, navigator.tab)
    }
    BackHandler(navigator.tab != AppTab.HOME && navigator.detail == DetailSelection.None) {
        navigator.back()
    }

    val feedTextCache = remember(state.accountCode) { FeedTextCache() }
    CompositionLocalProvider(LocalFeedTextCache provides feedTextCache) {
        Box(Modifier.fillMaxSize()) {
            ThemeBackground(Modifier.matchParentSize())
            Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                    AppNavigation(navigator.tab, state.needsLogin && state.hasSavedData, state.logoutFailed) { selected ->
                        navigator.select(selected)
                    }
                }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    AppPageHeader(
                        title = when (navigator.tab) {
                            AppTab.HOME -> greeting
                            AppTab.ME -> stringResource(navigator.meRoute.titleRes)
                            else -> stringResource(navigator.tab.titleRes)
                        },
                        subtitle = if (state.busy) state.syncProgress?.description?.render() ?: stringResource(AppLabels.BRAND) else stringResource(AppLabels.BRAND),
                        titleTag = if (navigator.tab == AppTab.HOME) "home-greeting" else null) {
                        if (navigator.tab == AppTab.CLASSES) {
                            if (scheduleDate != today || scheduleScrolled) TextButton(onClick = { chooseScheduleDate(today) },
                                modifier = Modifier.testTag("schedule-today")) { Text(stringResource(AppLabels.TODAY)) }
                            IconButton(onClick = { navigator.showDatePicker = true }, modifier = Modifier.testTag("schedule-date-picker")) {
                                Icon(CalendarPickerIcon, contentDescription = stringResource(R.string.choose_date), modifier = Modifier.size(20.dp))
                            }
                        }
                        if (navigator.tab == AppTab.ME && (route != null || showAppearance)) TextButton(onClick = {
                            navigator.meRoute = MeRoute.Overview; navigator.detail = DetailSelection.None
                        }) { Text(stringResource(AppActions.BACK)) }
                    }
                    SyncProgressBar(state.syncProgress)
                    if (navigator.tab == AppTab.ME && showAppearance && !state.signingOut) AppearanceContent() else {
                        PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                            onRefresh = {
                                if (!state.signingOut && !state.logoutFailed) {
                                    when {
                                        navigator.tab == AppTab.MESSAGES -> actions.refreshResource(SyncResource.MESSAGES)
                                        navigator.tab == AppTab.ME && route != null -> actions.refreshResource(SyncResource.forFeed(route))
                                        else -> actions.refresh()
                                    }
                                }
                            }, indicator = {}, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
                            when {
                                state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.signing_out)) }
                                navigator.tab == AppTab.ME -> MeTab(state, navigator, actions, recoverResource, openLink)
                                !state.hasSavedData && state.busy && !state.signedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.loading_saved_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                !state.hasSavedData && state.logoutFailed -> Column(Modifier.padding(Spacing.page)) {
                                    Text(state.globalMessage?.render().orEmpty())
                                    Button(onClick = actions.signOut) { Text(stringResource(AppActions.RETRY_SIGN_OUT)) }
                                }
                                !state.hasSavedData && state.needsLogin -> Welcome(actions.signIn)
                                else -> {
                                    when (navigator.tab) {
                                    AppTab.HOME -> HomeContent(home, today, now,
                                        { navigator.detail = DetailSelection.Class(it.id) }, { navigator.detail = DetailSelection.Task(it.id) },
                                        { navigator.openTasks(CourseworkFilter.UPCOMING) },
                                        { navigator.openTasks(CourseworkFilter.PAST) }, actions.signIn, recoverResource, openLink,
                                        { navigator.detail = DetailSelection.Feed(FeedKind.MESSAGES, it.id) },
                                        { navigator.select(AppTab.MESSAGES) })
                                    AppTab.MESSAGES -> FeedContent(FeedKind.MESSAGES, messages, state.busy, state.needsLogin,
                                        { recoverResource(SyncResource.MESSAGES) }, actions.loadOlderMessages,
                                        { navigator.detail = DetailSelection.Feed(FeedKind.MESSAGES, it.id) }, openLink, actions.signIn, today)
                                    AppTab.TASKS -> CourseworkContent(state.coursework, now, state.busy,
                                        feedback = { ResourceRecoveryRow(courseworkRecovery, actions.signIn) { recoverResource(SyncResource.COURSEWORK) } },
                                        showFeedback = state.needsLogin || state.coursework.message != null,
                                        filterOverride = navigator.courseworkFilter, onFilterChanged = { navigator.courseworkFilter = it }) { navigator.detail = DetailSelection.Task(it.id) }
                                    AppTab.CLASSES -> ScheduleContent(schedule, today, now, scheduleDate, scheduleListState,
                                        feedback = { ResourceRecoveryRow(timetableRecovery, actions.signIn) { recoverResource(SyncResource.TIMETABLE) } }) { navigator.detail = DetailSelection.Class(it.id) }
                                    AppTab.ME -> Unit // Handled before the common data states above.
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (navigator.tab == AppTab.CLASSES && navigator.showDatePicker && !state.signingOut) ScheduleDateDialog(scheduleDate,
            onDismiss = { navigator.showDatePicker = false }, onDate = { chooseScheduleDate(it); navigator.showDatePicker = false })
        if (!state.signingOut) when (val selection = navigator.detail) {
            is DetailSelection.Class -> state.events.firstOrNull { it.id == selection.id }?.let {
                EventDetails(it, it.id in conflicts, { navigator.detail = DetailSelection.None }, actions.openExternal)
            }
            is DetailSelection.Task -> state.coursework.entries.firstOrNull { it.id == selection.id }?.let {
                CourseworkDetails(it, { navigator.detail = DetailSelection.None }, actions.openExternal)
            }
            is DetailSelection.Feed -> if (selection.visibleOn(navigator.tab, navigator.meRoute)) state.feed(selection.kind).entries.firstOrNull { it.id == selection.id }?.let {
                FeedDetails(selection.kind, it, actions.openExternal, { navigator.detail = DetailSelection.None })
            }
            DetailSelection.None -> Unit
        }
    }
}
