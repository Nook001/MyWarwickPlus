package uk.ac.warwick.plus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusScreen(state: TimetableState, onRefresh: () -> Unit, onLogin: () -> Unit,
    probe: ((ProbeEndpoint) -> ProbeResult)? = null, onCourseworkLink: ((String) -> Unit)? = null,
    onSignOut: () -> Unit = {}, onFeedRefresh: ((FeedKind) -> Unit)? = null,
    onMoreMessages: () -> Unit = {}, onExternalLink: ((String) -> Unit)? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var courseworkId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProbe by rememberSaveable { mutableStateOf(false) }
    var feedRoute by rememberSaveable { mutableStateOf<Int?>(null) }
    var feedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val route = FeedKind.entries.firstOrNull { it.key == feedRoute }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
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
    var weekView by rememberSaveable { mutableStateOf(false) }
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
        if (tab == 3 && route != null) feedRoute = null else tab = 0
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                icon = { Icon(HomeIcon, null) }, label = { Text("Home") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                icon = { Icon(ScheduleIcon, null) }, label = { Text("Schedule") })
            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                icon = { Icon(CourseworkIcon, null) }, label = { Text("Coursework", maxLines = 1, overflow = TextOverflow.Ellipsis) })
            NavigationBarItem(selected = tab == 3, onClick = { tab = 3; feedRoute = null; feedEntryId = null }, modifier = Modifier.testTag("more-tab"),
                icon = { Text("•••", style = MaterialTheme.typography.titleLarge) }, label = { Text("More") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = if (tab == 0) 16.dp else Spacing.page,
                vertical = if (tab == 0) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (tab == 0) 2.dp else 4.dp)) {
                    Text("MY WARWICK +", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (tab == 3) route?.label ?: "More" else if (tab == 1) "Timetable" else if (tab == 2) "Coursework deadlines" else greeting,
                        style = if (tab == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold, modifier = if (tab == 0) Modifier.testTag("home-greeting") else Modifier)
                }
                if (tab == 3 && route != null) TextButton(onClick = { feedRoute = null; feedEntryId = null }) { Text("Back") }
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!state.signingOut && state.message != null &&
                (tab == 3 && (state.needsLogin || state.logoutFailed) || tab != 3 && (state.hasSavedData || !state.needsLogin))) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = Spacing.page)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        when {
                            state.logoutFailed -> TextButton(onClick = onSignOut) { Text("Retry sign-out") }
                            state.needsLogin -> TextButton(onClick = onLogin) { Text("Sign in") }
                            else -> TextButton(onClick = onRefresh, enabled = !state.busy) { Text("Try again") }
                        }
                    }
                }
            }
            PullToRefreshBox(isRefreshing = state.busy && !state.signingOut,
                onRefresh = { if (!state.signingOut && !state.logoutFailed) {
                    if (tab == 3 && route != null && onFeedRefresh != null) onFeedRefresh(route) else onRefresh()
                } }, modifier = Modifier.fillMaxSize().testTag("refresh-container")) {
            when {
                state.signingOut -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Signing out…") }
                tab == 3 -> {
                    if (route == null) MoreContent(state, onLogin, onSignOut, { feedRoute = it.key }, openLink,
                        if (probe != null && !state.logoutFailed) ({ showProbe = true }) else null)
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
                    if (tab == 0) Home(state, today, now, { selectedId = it.id }, { courseworkId = it.id }, { tab = 2 },
                        if (probe != null) ({ showProbe = true }) else null)
                    else if (tab == 2) CourseworkContent(state.coursework, now, state.busy, onRefresh, { courseworkId = it.id })
                    else Schedule(state, today, now, LocalDate.ofEpochDay(selectedDay), weekView,
                        { selectedDay = it.toEpochDay() }, { weekView = it }, { selectedId = it.id })
                }
            }
            }
        }
    }
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
    onCoursework: (CourseworkEntity) -> Unit, onAllCoursework: () -> Unit, onProbe: (() -> Unit)?) {
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
        if (todayEvents.isEmpty() && state.lastSynced != null) item { EmptyCard("No classes today", "Your saved timetable is clear for today.") }
        items(todayEvents, key = { it.id }) { EventRow(it, it.id in conflictingEventIds(todayEvents)) { onSelect(it) } }
        item {
            SectionLabel("DEADLINES")
            TextButton(onClick = onAllCoursework) { Text("View all coursework") }
            state.coursework.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        val upcoming = state.coursework.entries.filter { it.dueMillis >= now }.sortedBy { it.dueMillis }.take(3)
        if (upcoming.isEmpty()) item {
            EmptyCard(if (state.coursework.lastSynced == null) "Coursework hasn't loaded yet" else "No upcoming deadlines returned",
                "Check Coursework for the saved feed and source-service links.")
        }
        items(upcoming, key = { "coursework/${it.id}" }) { entry -> CourseworkRow(entry, now) { onCoursework(entry) } }
        item { CourseworkSyncNote(state.coursework) }
        item { SyncNote(state, now) }
        if (onProbe != null) item { TextButton(onClick = onProbe) { Text("Developer tools") } }
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
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("next-class-card")) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(event.location.ifBlank { "Location not provided" }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("next-location"))
                TextButton(onClick = onSelect, modifier = Modifier.widthIn(max = 128.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Text("View class details", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun Schedule(state: TimetableState, today: LocalDate, now: Long, selected: LocalDate, weekView: Boolean,
    onDate: (LocalDate) -> Unit, onMode: (Boolean) -> Unit, onSelect: (EventEntity) -> Unit) {
    val start = monday(selected)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onDate(selected.minusWeeks(1)) }) { Text("Previous") }
                TextButton(onClick = { onDate(today) }) { Text("Today") }
                TextButton(onClick = { onDate(selected.plusWeeks(1)) }) { Text("Next") }
            }
            Text("${start.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK))} – ${start.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK))}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { offset ->
                    val date = start.plusDays(offset.toLong())
                    val active = date == selected
                    Surface(onClick = { onDate(date); onMode(false) },
                        modifier = Modifier.width(48.dp).height(68.dp).semantics { contentDescription = dateLabel(date) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(date.format(DateTimeFormatter.ofPattern("EEE", Locale.UK)), style = MaterialTheme.typography.labelSmall)
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
                            if (date == today) Text("•", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !weekView, onClick = { onMode(false) }, label = { Text("Day") })
                FilterChip(selected = weekView, onClick = { onMode(true) }, label = { Text("Week") })
            }
        }
        if (state.lastSynced == null) item { EmptyCard("Your timetable hasn't loaded yet", "Refresh to try again.") }
        else if (!weekView) {
            val events = eventsOnDate(state.events, selected)
            item { SectionLabel(dateLabel(selected), classCountLabel(events.size)) }
            if (events.isEmpty()) item { EmptyCard("No classes on this day", "Your saved timetable has no events for this date.") }
            items(events, key = { it.id }) { EventRow(it, it.id in conflictingEventIds(events)) { onSelect(it) } }
        } else {
            val days = (0..6).map { start.plusDays(it.toLong()) to eventsOnDate(state.events, start.plusDays(it.toLong())) }
            if (days.all { it.second.isEmpty() }) item { EmptyCard("No classes this week", "Your saved timetable has no events for these dates.") }
            days.filter { it.second.isNotEmpty() }.forEach { (date, events) ->
                item(key = date.toString()) { SectionLabel(dateLabel(date), classCountLabel(events.size)) }
                items(events, key = { "${date}/${it.id}" }) { EventRow(it, it.id in conflictingEventIds(events)) { onSelect(it) } }
            }
        }
        item { SyncNote(state, now) }
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
