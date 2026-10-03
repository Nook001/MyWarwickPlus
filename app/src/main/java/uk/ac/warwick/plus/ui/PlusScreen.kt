package uk.ac.warwick.plus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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

@Composable
fun PlusScreen(state: TimetableState, onRefresh: () -> Unit, onLogin: () -> Unit,
    probe: ((ProbeEndpoint) -> ProbeResult)? = null) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProbe by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val today = atWarwick(now).toLocalDate()
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var weekView by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.events, selectedId) {
        if (selectedId != null && state.events.none { it.id == selectedId }) selectedId = null
    }
    BackHandler(tab != 0 && selectedId == null && !showProbe) { tab = 0 }

    Scaffold(bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                icon = { Icon(HomeIcon, null) }, label = { Text("Home") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                icon = { Icon(ScheduleIcon, null) }, label = { Text("Schedule") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MY WARWICK +", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (tab == 1) "Timetable" else when (atWarwick(now).hour) {
                        in 0..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
                    }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onRefresh, enabled = !state.busy) { Text("Refresh") }
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                state.lastSynced == null && state.busy && !state.signedIn -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading your timetable…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.lastSynced == null && state.needsLogin -> Welcome(onLogin)
                else -> {
                    if (state.message != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(horizontal = Spacing.page)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            if (state.needsLogin) TextButton(onClick = onLogin) { Text("Sign in") }
                            else TextButton(onClick = onRefresh, enabled = !state.busy) { Text("Try again") }
                        }
                    }
                    if (tab == 0) Home(state, today, now, { selectedId = it.id },
                        if (probe != null) ({ showProbe = true }) else null)
                    else Schedule(state, today, now, LocalDate.ofEpochDay(selectedDay), weekView,
                        { selectedDay = it.toEpochDay() }, { weekView = it }, { selectedId = it.id })
                }
            }
        }
    }
    state.events.firstOrNull { it.id == selectedId }?.let { event ->
        EventDetails(event, onDismiss = { selectedId = null })
    }
    if (showProbe && probe != null) ApiProbeSheet(probe, onLogin, { showProbe = false })
}

@Composable
private fun Welcome(onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
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
    onProbe: (() -> Unit)?) {
    val next = state.events.filter { it.endMillis > now }.minByOrNull { it.startMillis }
    val todayEvents = eventsOnDate(state.events, today)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.page),
        verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
        item {
            Text(dateLabel(today), style = MaterialTheme.typography.titleMedium)
            Text("Times shown in Warwick time", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SectionLabel("UP NEXT")
            Spacer(Modifier.height(12.dp))
            when {
                state.lastSynced == null -> EmptyCard("Your timetable hasn't loaded yet", "Refresh to try again.")
                next == null -> EmptyCard("Nothing coming up", "There are no upcoming classes in your saved timetable.")
                else -> Card(onClick = { onSelect(next) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(nextClassLabel(next, now), style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        Text(if (next.allDay) "All day" else "${timeLabel(next.startMillis)} – ${timeLabel(next.endMillis)}",
                            style = MaterialTheme.typography.titleMedium)
                        Text(next.moduleName.ifBlank { next.title }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        if (next.moduleName.isNotBlank() && next.moduleName != next.title) Text(next.title, style = MaterialTheme.typography.labelLarge)
                        else if (next.module.isNotBlank()) Text(next.module, style = MaterialTheme.typography.labelLarge)
                        if (next.location.isNotBlank()) Text(next.location, style = MaterialTheme.typography.bodyMedium)
                        Text("View class details", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item { SectionLabel("TODAY", if (state.lastSynced != null) classCountLabel(todayEvents.size) else null) }
        if (todayEvents.isEmpty() && state.lastSynced != null) item { EmptyCard("No classes today", "Your saved timetable is clear for today.") }
        items(todayEvents, key = { it.id }) { EventRow(it) { onSelect(it) } }
        item { SyncNote(state, now) }
        if (onProbe != null) item { TextButton(onClick = onProbe) { Text("Developer tools") } }
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
            items(events, key = { it.id }) { EventRow(it) { onSelect(it) } }
        } else {
            val days = (0..6).map { start.plusDays(it.toLong()) to eventsOnDate(state.events, start.plusDays(it.toLong())) }
            if (days.all { it.second.isEmpty() }) item { EmptyCard("No classes this week", "Your saved timetable has no events for these dates.") }
            days.filter { it.second.isNotEmpty() }.forEach { (date, events) ->
                item(key = date.toString()) { SectionLabel(dateLabel(date), classCountLabel(events.size)) }
                items(events, key = { "${date}/${it.id}" }) { EventRow(it) { onSelect(it) } }
            }
        }
        item { SyncNote(state, now) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetails(event: EventEntity, onDismiss: () -> Unit) {
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
