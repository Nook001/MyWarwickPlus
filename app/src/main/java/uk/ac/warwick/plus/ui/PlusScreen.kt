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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.data.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusScreen(state: TimetableState, onRefresh: () -> Unit, onLogin: () -> Unit,
    probe: ((ProbeEndpoint) -> ProbeResult)? = null, onCourseworkLink: ((String) -> Unit)? = null,
    onSignOut: () -> Unit = {}, onFeedRefresh: ((FeedKind) -> Unit)? = null,
    onMoreMessages: () -> Unit = {}, onExternalLink: ((String) -> Unit)? = null,
    onNoticeConsumed: (Long) -> Unit = {}, onResourceRefresh: ((SyncResource) -> Unit)? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var courseworkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProbe by rememberSaveable { mutableStateOf(false) }
    var feedRoute by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var courseworkFilter by rememberSaveable { mutableStateOf("Upcoming") }
    var feedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val route = FeedKind.entries.firstOrNull { it.key == feedRoute }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val refreshResource: (SyncResource) -> Unit = { resource ->
        if (onResourceRefresh != null) onResourceRefresh(resource)
        else resource.feed?.let { onFeedRefresh?.invoke(it) ?: onRefresh() } ?: onRefresh()
    }
    val recoverResource: (SyncResource) -> Unit = { resource ->
        val messages = state.feed(FeedKind.MESSAGES)
        if (resource == SyncResource.MESSAGES && messages.olderPageFailed && messages.hasMore && messages.entries.isNotEmpty()) onMoreMessages()
        else refreshResource(resource)
    }
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice ->
            try {
                val result = snackbar.showSnackbar(notice.message,
                    actionLabel = if (state.needsLogin) "Sign in" else "Retry", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    if (state.needsLogin) onLogin()
                    else if (notice.olderMessages) recoverResource(SyncResource.MESSAGES)
                    else if (notice.resource != null) refreshResource(notice.resource)
                    else notice.feed?.let { kind -> onFeedRefresh?.invoke(kind) ?: onRefresh() } ?: onRefresh()
                }
            } finally { onNoticeConsumed(notice.id) }
        }
    }
    val launchBrowser = rememberBrowserOpener(onExternalLink)
    val openLink: (String) -> Unit = { raw ->
        if (!launchBrowser(raw)) scope.launch { snackbar.showSnackbar("Couldn't open this link. Try again.") }
    }
    var previousAccount by rememberSaveable { mutableStateOf(state.accountCode) }
    LaunchedEffect(state.accountCode) {
        if (previousAccount.isNotBlank() && previousAccount != state.accountCode) {
            selectedId = null; courseworkId = null; feedEntryId = null; feedRoute = null; showProbe = false
            courseworkFilter = "Upcoming"
        }
        previousAccount = state.accountCode
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val today = atWarwick(now).toLocalDate()
    val greeting = greeting(state.name, now)
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var followToday by rememberSaveable { mutableStateOf(true) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val scheduleListState = rememberLazyListState()
    val scheduleDate = if (followToday) today else LocalDate.ofEpochDay(selectedDay)
    val scheduleScrolled by remember { derivedStateOf { scheduleListState.firstVisibleItemIndex > 0 || scheduleListState.firstVisibleItemScrollOffset > 0 } }
    val chooseScheduleDate: (LocalDate) -> Unit = { date ->
        selectedDay = date.toEpochDay(); followToday = date == today
        scheduleListState.requestScrollToItem(0)
    }
    LaunchedEffect(state.events, selectedId) {
        if (selectedId != null && state.events.none { it.id == selectedId }) selectedId = null
    }
    LaunchedEffect(state.coursework.entries, courseworkId) {
        if (courseworkId != null && state.coursework.entries.none { it.id == courseworkId }) courseworkId = null
    }
    LaunchedEffect(state.feeds, feedEntryId) {
        if (feedEntryId != null && (route == null || state.feed(route).entries.none { it.id == feedEntryId })) feedEntryId = null
    }
    BackHandler(tab != 0 && selectedId == null && courseworkId == null && feedEntryId == null && !showProbe) {
        if (tab == 3 && showAppearance) showAppearance = false
        else if (tab == 3 && route != null) feedRoute = null else tab = 0
    }

    Box(Modifier.fillMaxSize()) {
        ThemeBackground(Modifier.matchParentSize())
        Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
                AppNavigation(tab, state.needsLogin && state.hasSavedData, state.logoutFailed) { selected ->
                    tab = selected
                    if (selected == 3) { feedRoute = null; feedEntryId = null; showAppearance = false }
                }
            }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                AppPageHeader(
                    title = if (tab == 3) (if (showAppearance) "Appearance" else route?.label ?: "Me")
                        else if (tab == 1) "Schedule" else if (tab == 2) "Coursework deadlines" else greeting,
                    subtitle = if (state.busy) state.syncProgress?.description ?: "MY WARWICK +" else "MY WARWICK +",
                    titleModifier = if (tab == 0) Modifier.testTag("home-greeting") else Modifier) {
                    if (tab == 1) {
                        if (scheduleDate != today || scheduleScrolled) TextButton(onClick = { chooseScheduleDate(today) },
                            modifier = Modifier.testTag("schedule-today")) { Text("Today") }
                        IconButton(onClick = { showDatePicker = true }, modifier = Modifier.testTag("schedule-date-picker")) {
                            Icon(CalendarPickerIcon, contentDescription = "Choose date", modifier = Modifier.size(20.dp))
                        }
                    }
                    if (tab == 3 && (route != null || showAppearance)) TextButton(onClick = {
                        feedRoute = null; feedEntryId = null; showAppearance = false
                    }) { Text("Back") }
                }
                SyncProgressBar(state.syncProgress)
                if (tab == 3 && showAppearance && !state.signingOut) AppearanceContent() else {
                    PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                        onRefresh = {
                            if (!state.signingOut && !state.logoutFailed) {
                                if (tab == 3 && route != null && onFeedRefresh != null) onFeedRefresh(route) else onRefresh()
                            }
                        }, indicator = {}, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
                        when {
                            state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signing out…") }
                            tab == 3 -> {
                                if (route == null) MoreContent(state, onLogin, onSignOut, { feedRoute = it.key }, openLink,
                                    if (probe != null && !state.logoutFailed) ({ showProbe = true }) else null,
                                    onSettings = { showAppearance = true }, onResourceRefresh = recoverResource)
                                else FeedContent(route, state.feed(route), state.busy, state.needsLogin,
                                    { recoverResource(SyncResource.forFeed(route)) }, onMoreMessages, { feedEntryId = it.id }, openLink, onLogin)
                            }
                            !state.hasSavedData && state.busy && !state.signedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Loading your saved data…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            !state.hasSavedData && state.logoutFailed -> Column(Modifier.padding(Spacing.page)) {
                                Text(state.message.orEmpty())
                                Button(onClick = onSignOut) { Text("Retry sign-out") }
                            }
                            !state.hasSavedData && state.needsLogin -> Welcome(onLogin)
                            else -> {
                                if (tab == 0) HomeContent(state, today, now, { selectedId = it.id }, { courseworkId = it.id },
                                    { courseworkFilter = "Upcoming"; tab = 2 }, { courseworkFilter = "Past"; tab = 2 }, onLogin, recoverResource, openLink)
                                else if (tab == 2) CourseworkContent(state.coursework, now, state.busy,
                                    feedback = { ResourceRecoveryRow(state, SyncResource.COURSEWORK, onLogin) { recoverResource(SyncResource.COURSEWORK) } },
                                    showFeedback = state.needsLogin || state.coursework.message != null,
                                    filterOverride = courseworkFilter, onFilterChanged = { courseworkFilter = it }) { courseworkId = it.id }
                                else ScheduleContent(state, today, now, scheduleDate, scheduleListState,
                                    feedback = { ResourceRecoveryRow(state, SyncResource.TIMETABLE, onLogin) { recoverResource(SyncResource.TIMETABLE) } }) { selectedId = it.id }
                            }
                        }
                    }
                }
            }
        }
    }
    if (tab == 1 && showDatePicker && !state.signingOut) ScheduleDateDialog(scheduleDate,
        onDismiss = { showDatePicker = false }, onDate = { chooseScheduleDate(it); showDatePicker = false })
    if (!state.signingOut) state.events.firstOrNull { it.id == selectedId }?.let { event ->
        EventDetails(event, event.id in conflictingEventIds(state.events), onDismiss = { selectedId = null }, onOpen = onExternalLink)
    }
    if (!state.signingOut) state.coursework.entries.firstOrNull { it.id == courseworkId }?.let { entry ->
        CourseworkDetails(entry, { courseworkId = null }, onCourseworkLink ?: onExternalLink)
    }
    if (!state.signingOut && route != null) state.feed(route).entries.firstOrNull { it.id == feedEntryId }?.let {
        FeedDetails(route, it, onExternalLink, { feedEntryId = null })
    }
    if (showProbe && probe != null && !state.signingOut) ApiProbeSheet(probe, onLogin, { showProbe = false })
}
