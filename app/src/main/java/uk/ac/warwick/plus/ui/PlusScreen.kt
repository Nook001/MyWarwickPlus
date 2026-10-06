package uk.ac.warwick.plus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
private fun RowScope.CompactTab(label: String, selected: Boolean, onSelect: () -> Unit,
    tag: String = "tab-${label.lowercase()}", icon: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val highlight = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Box(Modifier.weight(1f).selectable(selected, interactionSource = interactionSource, indication = null,
        role = Role.Tab, onClick = onSelect).testTag(tag)
        .heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = highlight,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
            Column(Modifier.padding(horizontal = 2.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

internal val DetailsChevron = ImageVector.Builder("DetailsChevron", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(9f, 6f); lineTo(7.6f, 7.4f); lineTo(12.2f, 12f)
        lineTo(7.6f, 16.6f); lineTo(9f, 18f); lineTo(15f, 12f); close()
    }
}.build()

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
    val context = androidx.compose.ui.platform.LocalContext.current
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
    val openLink: (String) -> Unit = { raw ->
        val url = safeExternalUrl(raw)
        val failed = url == null || runCatching {
            if (onExternalLink != null) onExternalLink(url)
            else androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(context, android.net.Uri.parse(url))
        }.isFailure
        if (failed) scope.launch { snackbar.showSnackbar("Couldn't open this link. Try again.") }
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
    val firstName = state.name.trim().split(Regex("\\s+"), limit = 2).firstOrNull().orEmpty()
    val greeting = when (atWarwick(now).hour) {
        in 0..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
    } + if (firstName.isNotBlank()) ", $firstName" else ""
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
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp).selectableGroup().testTag("compact-tab-bar"), verticalAlignment = Alignment.CenterVertically) {
                CompactTab("Home", tab == 0, { tab = 0 }) {
                    Icon(if (tab == 0) NavigationIcons.homeFilled else NavigationIcons.homeOutline, null, Modifier.size(24.dp))
                }
                CompactTab("Classes", tab == 1, { tab = 1 }, "schedule-tab") {
                    Icon(if (tab == 1) NavigationIcons.scheduleFilled else NavigationIcons.scheduleOutline, null, Modifier.size(24.dp))
                }
                CompactTab("Tasks", tab == 2, { tab = 2 }, "tab-coursework") {
                    Icon(if (tab == 2) NavigationIcons.courseworkFilled else NavigationIcons.courseworkOutline, null, Modifier.size(24.dp))
                }
                CompactTab("Me", tab == 3, { tab = 3; feedRoute = null; feedEntryId = null; showAppearance = false }, "more-tab") {
                    Box(Modifier.size(24.dp)) {
                        Icon(if (tab == 3) NavigationIcons.meFilled else NavigationIcons.meOutline, null, Modifier.size(24.dp))
                        if ((state.needsLogin && state.hasSavedData) || state.logoutFailed) Box(Modifier.align(Alignment.TopEnd)
                            .size(5.dp).background(MaterialTheme.colorScheme.error, CircleShape).semantics {
                                contentDescription = if (state.logoutFailed) "Sign-out needs attention" else "Sign in required"
                            })
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().testTag("page-header").padding(horizontal = 16.dp, vertical = 2.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (state.busy) state.syncProgress?.description ?: "MY WARWICK +" else "MY WARWICK +",
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (tab == 3) (if (showAppearance) "Appearance" else route?.label ?: "Me") else if (tab == 1) "Schedule" else if (tab == 2) "Coursework deadlines" else greeting,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold, modifier = if (tab == 0) Modifier.testTag("home-greeting") else Modifier)
                }
                if (tab == 1) {
                    if (scheduleDate != today || scheduleScrolled) TextButton(onClick = { chooseScheduleDate(today) },
                        modifier = Modifier.testTag("schedule-today")) { Text("Today") }
                    IconButton(onClick = { showDatePicker = true }, modifier = Modifier.testTag("schedule-date-picker")) {
                        Icon(CalendarPickerIcon, contentDescription = "Choose date", modifier = Modifier.size(20.dp))
                    }
                }
                if (tab == 3 && (route != null || showAppearance)) TextButton(onClick = { feedRoute = null; feedEntryId = null; showAppearance = false }) { Text("Back") }
            }
            SyncProgressBar(state.syncProgress)
            if (tab == 3 && showAppearance && !state.signingOut) AppearanceContent() else {
            PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                onRefresh = { if (!state.signingOut && !state.logoutFailed) {
                    if (tab == 3 && route != null && onFeedRefresh != null) onFeedRefresh(route) else onRefresh()
                } }, indicator = {}, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
            when {
                state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signing out…") }
                tab == 3 -> {
                    if (route == null) MoreContent(state, onLogin, onSignOut, { feedRoute = it.key }, openLink,
                        if (probe != null && !state.logoutFailed) ({ showProbe = true }) else null,
                        onSettings = { showAppearance = true }, onResourceRefresh = recoverResource)
                    else {
                        FeedContent(route, state.feed(route), state.busy, state.needsLogin,
                            { recoverResource(SyncResource.forFeed(route)) }, onMoreMessages, { feedEntryId = it.id }, openLink, onLogin)
                    }
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
                    if (tab == 0) Home(state, today, now, { selectedId = it.id }, { courseworkId = it.id },
                        { courseworkFilter = "Upcoming"; tab = 2 }, { courseworkFilter = "Past"; tab = 2 }, onLogin, recoverResource)
                    else if (tab == 2) CourseworkContent(state.coursework, now, state.busy,
                        { refreshResource(SyncResource.COURSEWORK) },
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
        EventDetails(event, event.id in conflictingEventIds(state.events), onDismiss = { selectedId = null })
    }
    if (!state.signingOut) state.coursework.entries.firstOrNull { it.id == courseworkId }?.let { entry ->
        CourseworkDetails(entry, { courseworkId = null }, onCourseworkLink)
    }
    if (!state.signingOut && route != null) state.feed(route).entries.firstOrNull { it.id == feedEntryId }?.let {
        FeedDetails(route, it, openLink, { feedEntryId = null })
    }
    if (showProbe && probe != null && !state.signingOut) ApiProbeSheet(probe, onLogin, { showProbe = false })
}

@Composable
private fun SyncProgressBar(progress: SyncProgress?) {
    if (progress == null) return
    key(progress.id) {
        var visible by remember { mutableStateOf(!progress.finished) }
        LaunchedEffect(progress.finished) {
            if (progress.finished) { delay(250); visible = false }
        }
        val fraction by animateFloatAsState(progress.fraction, animationSpec = tween(200), label = "API progress")
        if (visible) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = progress.description },
            color = if (progress.finished && progress.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Welcome(onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("A little more clarity.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Text("Your next class. Your week ahead.\nA calmer place to start your day.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Sign in with Warwick") }
        Spacer(Modifier.height(16.dp))
        Text("Sign in on Warwick's official website. Your timetable stays on this device.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("An independent student app", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Home(state: TimetableState, today: LocalDate, now: Long, onSelect: (EventEntity) -> Unit,
    onCoursework: (CourseworkEntity) -> Unit, onAllCoursework: () -> Unit, onPastCoursework: () -> Unit,
    onLogin: () -> Unit, onRecover: (SyncResource) -> Unit) {
    val next = currentOrNextClass(state.events, now)
    val nextId = nextTimedClass(state.events, now)?.id
    val todayEvents = eventsOnDate(state.events, today)
    val conflicts = remember(state.events) { conflictingEventIds(state.events) }
    val upcoming = state.coursework.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }.take(3)
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            when {
                state.lastSynced == null -> HomeSection("Next") {
                    HomeEmptyRow(if (state.busy) "Loading timetable…" else "Timetable hasn't loaded yet")
                }
                next == null -> HomeSection("Next") { HomeEmptyRow("No upcoming classes") }
                else -> NextClassCard(next, now) { onSelect(next) }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                ResourceRecoveryRow(state, SyncResource.TIMETABLE, onLogin) { onRecover(SyncResource.TIMETABLE) }
            }
        }
        if (state.lastSynced != null) item {
            HomeSection("Today", trailing = {
                Text(classCountLabel(todayEvents.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) {
                if (todayEvents.isEmpty()) HomeEmptyRow("No classes today", Modifier.testTag("today-empty"))
                else todayEvents.forEachIndexed { index, event ->
                    if (index > 0) HomeRowDivider()
                    val status = when {
                        !event.allDay && event.startMillis <= now && event.endMillis > now -> "Now"
                        event.id == nextId -> "Next"
                        else -> null
                    }
                    ScheduleClassRow(event, today, status, event.id in conflicts) { onSelect(event) }
                }
            }
        }
        item {
            HomeSection("Deadlines", trailing = {
                TextButton(onClick = onAllCoursework, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.semantics { onClick(label = "View all coursework", action = null) }) {
                    Text("All", style = MaterialTheme.typography.bodySmall)
                    Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }) {
                when {
                    state.coursework.lastSynced == null -> HomeEmptyRow(if (state.busy) "Loading coursework…" else "Coursework hasn't loaded yet")
                    upcoming.isEmpty() -> HomeEmptyRow("No upcoming deadlines in this feed")
                    else -> upcoming.forEachIndexed { index, entry ->
                        if (index > 0) HomeRowDivider()
                        HomeDeadlineRow(entry, now) { onCoursework(entry) }
                    }
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    ResourceRecoveryRow(state, SyncResource.COURSEWORK, onLogin) { onRecover(SyncResource.COURSEWORK) }
                }
                val recentStart = atWarwick(now).minusDays(7).toInstant().toEpochMilli()
                val recentPast = state.coursework.entries.count { it.dueMillis in recentStart until now }
                if (state.coursework.lastSynced != null && recentPast > 0) TextButton(onClick = onPastCoursework,
                    modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Recently passed · $recentPast", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, trailing: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("home-${title.lowercase()}-section"), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            Row(Modifier.fillMaxWidth().testTag("${title.lowercase()}-header").heightIn(min = 48.dp)
                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).testTag("${title.lowercase()}-heading"))
                trailing()
            }
            content()
        }
    }
}

@Composable
private fun HomeEmptyRow(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp))
}

@Composable
private fun HomeRowDivider() = HorizontalDivider(Modifier.padding(horizontal = 12.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))

@Composable
private fun HomeDeadlineRow(entry: CourseworkEntity, now: Long, onSelect: () -> Unit) {
    val due = atWarwick(entry.dueMillis).toLocalDate()
    val today = atWarwick(now).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due)
    val date = due.format(DateTimeFormatter.ofPattern(if (due.year > today.year) "d MMM yyyy" else "d MMM", Locale.UK))
    val countdownWidth = with(LocalDensity.current) { 48.sp.toDp() }
    Surface(onClick = onSelect, color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().testTag("home-deadline-${entry.id}")
            .semantics { onClick(label = "View coursework details", action = null) }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(countdownWidth), verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(days.toString(), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                    softWrap = false, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false, maxLines = 1)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(DetailsChevron, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NextClassCard(event: EventEntity, now: Long, onSelect: () -> Unit) {
    val name = event.moduleName.ifBlank { event.title }
    val code = if (event.moduleName.isNotBlank() && event.title != name) event.title
        else event.module.takeUnless { it == name }.orEmpty()
    val endDate = atWarwick(event.endMillis).toLocalDate()
    val end = if (endDate != atWarwick(event.startMillis).toLocalDate())
        "${endDate.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))} ${timeLabel(event.endMillis)}"
        else timeLabel(event.endMillis)
    val time = if (event.allDay) "All day" else "${timeLabel(event.startMillis)} – $end"
    Card(onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("next-class-card")
            .semantics { onClick(label = "View class details", action = null) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (event.startMillis <= now) "Now" else "Next", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                    Text(if (event.startMillis <= now) time else "${nextClassLabel(event, now)} · $time",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).alignByBaseline().testTag("next-when"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).alignByBaseline().testTag("next-name"))
                    if (code.isNotBlank()) Text(code, style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 112.dp).alignByBaseline().testTag("next-code"))
                }
                Text(event.location.ifBlank { "Location not provided" }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().testTag("next-location"))
            }
            Icon(DetailsChevron, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp).testTag("next-details-chevron"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetails(event: EventEntity, conflict: Boolean, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(modifier = Modifier.testTag("class-details"), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text("CLASS DETAILS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(event.moduleName.ifBlank { event.title }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            if (event.module.isNotBlank()) item { DetailField("Module", event.module) }
            if (event.moduleName.isNotBlank() && event.moduleName != event.title) item { DetailField("Timetable entry", event.title) }
            item {
                DetailField("When", "${dateLabel(atWarwick(event.startMillis).toLocalDate())}\n" +
                    if (event.allDay) "All day · Warwick time" else
                        "${timeLabel(event.startMillis)} – ${atWarwick(event.endMillis).let { if (it.toLocalDate() != atWarwick(event.startMillis).toLocalDate()) dateLabel(it.toLocalDate()) + " · " else "" }}${timeLabel(event.endMillis)} · Warwick time")
            }
            item { DetailField("Location", event.location.ifBlank { "Location hasn't been provided" }) }
            if (conflict) item { Text("Another class overlaps this time in your saved timetable.", color = MaterialTheme.colorScheme.error) }
            if (event.academicWeek > 0) item { DetailField("Academic week", event.academicWeek.toString()) }
            item { LocationLink(event) }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close details") } }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
