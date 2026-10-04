package uk.ac.warwick.plus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HomeIcon = ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 10f); lineTo(12f, 2f); lineTo(21f, 10f); lineTo(21f, 22f)
        lineTo(14f, 22f); lineTo(14f, 15f); lineTo(10f, 15f); lineTo(10f, 22f); lineTo(3f, 22f); close()
    }
}.build()
private val ScheduleIcon = ImageVector.Builder("Schedule", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 3f); lineTo(20f, 3f); lineTo(20f, 21f); lineTo(4f, 21f); close()
        moveTo(6f, 8f); lineTo(6f, 19f); lineTo(18f, 19f); lineTo(18f, 8f); close()
    }
}.build()

private val CourseworkIcon = ImageVector.Builder("Coursework", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 8f); lineTo(4f, 8f); close()
        moveTo(4f, 11f); lineTo(20f, 11f); lineTo(20f, 14f); lineTo(4f, 14f); close()
        moveTo(4f, 17f); lineTo(16f, 17f); lineTo(16f, 20f); lineTo(4f, 20f); close()
    }
}.build()

private val MoreIcon = ImageVector.Builder("More", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        for (x in listOf(5f, 12f, 19f)) {
            moveTo(x - 2f, 12f); curveTo(x - 2f, 9.3f, x + 2f, 9.3f, x + 2f, 12f)
            curveTo(x + 2f, 14.7f, x - 2f, 14.7f, x - 2f, 12f); close()
        }
    }
}.build()

@Composable
private fun RowScope.CompactTab(label: String, selected: Boolean, onSelect: () -> Unit,
    tag: String = "tab-${label.lowercase()}", icon: @Composable () -> Unit) {
    Column(Modifier.weight(1f).selectable(selected, role = Role.Tab, onClick = onSelect).testTag(tag)
        .heightIn(min = 64.dp).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Surface(shape = RoundedCornerShape(50), color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 3.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
    onNoticeConsumed: (Long) -> Unit = {}) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var courseworkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProbe by rememberSaveable { mutableStateOf(false) }
    var feedRoute by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var feedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val route = FeedKind.entries.firstOrNull { it.key == feedRoute }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice?.id) {
        state.notice?.let { notice ->
            try {
                val result = snackbar.showSnackbar(notice.message,
                    actionLabel = if (state.needsLogin) "Sign in" else "Retry", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) {
                    if (state.needsLogin) onLogin()
                    else if (notice.olderMessages) onMoreMessages()
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
                CompactTab("Home", tab == 0, { tab = 0 }) { Icon(HomeIcon, null, Modifier.size(22.dp)) }
                CompactTab("Schedule", tab == 1, { tab = 1 }, "schedule-tab") { Icon(ScheduleIcon, null, Modifier.size(22.dp)) }
                CompactTab("Coursework", tab == 2, { tab = 2 }) { Icon(CourseworkIcon, null, Modifier.size(22.dp)) }
                CompactTab("More", tab == 3, { tab = 3; feedRoute = null; feedEntryId = null; showAppearance = false }, "more-tab") {
                    Icon(MoreIcon, null, Modifier.size(22.dp))
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().testTag("page-header").padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("MY WARWICK +", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (tab == 3) (if (showAppearance) "Appearance" else route?.label ?: "More") else if (tab == 1) "Schedule" else if (tab == 2) "Coursework deadlines" else greeting,
                        style = MaterialTheme.typography.titleMedium,
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
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (tab == 3 && showAppearance && !state.signingOut) AppearanceContent() else {
            PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                onRefresh = { if (!state.signingOut && !state.logoutFailed) {
                    if (tab == 3 && route != null && onFeedRefresh != null) onFeedRefresh(route) else onRefresh()
                } }, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
            when {
                state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signing out…") }
                tab == 3 -> {
                    if (route == null) MoreContent(state, onLogin, onSignOut, { feedRoute = it.key }, openLink,
                        if (probe != null && !state.logoutFailed) ({ showProbe = true }) else null,
                        onSettings = { showAppearance = true })
                    else {
                        FeedContent(route, state.feed(route), state.busy, state.needsLogin,
                            { onFeedRefresh?.invoke(route) ?: onRefresh() }, onMoreMessages, { feedEntryId = it.id }, openLink)
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
                    if (tab == 0) Home(state, today, now, { selectedId = it.id }, { courseworkId = it.id }, { tab = 2 })
                    else if (tab == 2) CourseworkContent(state.coursework, now, state.busy, onRefresh, { courseworkId = it.id })
                    else ScheduleContent(state, today, now, scheduleDate, scheduleListState) { selectedId = it.id }
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
    onCoursework: (CourseworkEntity) -> Unit, onAllCoursework: () -> Unit) {
    val next = state.events.filter { it.endMillis > now }.minByOrNull { it.startMillis }
    val todayEvents = eventsOnDate(state.events, today)
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionLabel("NEXT")
            Spacer(Modifier.height(6.dp))
            when {
                state.lastSynced == null -> EmptyCard("Your timetable hasn't loaded yet", "Refresh to try again.")
                next == null -> EmptyCard("Nothing coming up", "There are no upcoming classes in your saved timetable.")
                else -> NextClassCard(next, now) { onSelect(next) }
            }
        }
        item { SectionLabel("TODAY", if (state.lastSynced != null) classCountLabel(todayEvents.size) else null) }
        if (todayEvents.isEmpty() && state.lastSynced != null) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("today-empty")) {
                Text("No classes today", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
            }
        }
        items(todayEvents, key = { it.id }) { EventRow(it, it.id in conflictingEventIds(todayEvents)) { onSelect(it) } }
        item {
            Row(Modifier.fillMaxWidth().testTag("deadlines-header"), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.width(IntrinsicSize.Min).testTag("deadlines-heading")) { SectionLabel("DEADLINES") }
                TextButton(onClick = onAllCoursework, modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp).widthIn(max = 168.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("View all coursework")
                }
            }
        }
        val upcoming = state.coursework.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }.take(3)
        if (upcoming.isEmpty()) item {
            EmptyCard(if (state.coursework.lastSynced == null) "Coursework hasn't loaded yet" else "No upcoming deadlines returned",
                "Check Coursework for the saved feed and source-service links.")
        }
        items(upcoming, key = { "coursework/${it.id}" }) { entry -> CourseworkRow(entry, now) { onCoursework(entry) } }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("next-class-card")
            .semantics { onClick(label = "View class details", action = null) }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${nextClassLabel(event, now)} · $time", style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium, modifier = Modifier.testTag("next-when"))
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
            Icon(DetailsChevron, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
