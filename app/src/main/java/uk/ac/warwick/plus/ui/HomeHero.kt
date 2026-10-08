package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.ui.components.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.EventContentItem

internal fun moduleKey(event: EventContentItem) = event.module.ifBlank { classIdentity(event).name }

/** Sorted keys give a term's modules distinct colours instead of risking hash collisions. */
internal fun moduleColourIndex(events: List<EventContentItem>): Map<String, Int> =
    events.map(::moduleKey).distinct().sorted().withIndex().associate { (index, key) -> key to index }

@Composable
internal fun rememberModuleColours(events: List<EventContentItem>, onLight: Boolean): (EventContentItem) -> Color {
    val indices = remember(events) { moduleColourIndex(events) }
    return remember(indices, onLight) { { event -> moduleColour(indices[moduleKey(event)] ?: 0, onLight) } }
}

@Composable
internal fun NextClassCard(event: EventContentItem, timeline: DayTimeline?, today: LocalDate, now: Long,
    colourOf: (EventContentItem) -> Color, onSelect: () -> Unit) {
    val identity = classIdentity(event)
    val time = classTimeRange(event, includeWeekday = true)
    val date = atWarwick(event.startMillis).toLocalDate()
    val whenLabel = when {
        date <= today -> time
        date == today.plusDays(1) -> "${stringResource(R.string.tomorrow)} · $time"
        else -> "${weekdayDateLabel(date, includeYear = date.year != today.year)} · $time"
    }
    val colours = appCardColours(CardTone.Featured)
    AppCard(onClick = onSelect, tone = CardTone.Featured, shape = AppShapes.featured,
        modifier = Modifier.fillMaxWidth().testTag("next-class-card"), actionLabel = stringResource(R.string.view_class_details)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(ContentIcons.clock, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (isClassNow(event, now)) stringResource(AppLabels.NOW) else stringResource(AppLabels.NEXT), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium)
                Text(whenLabel, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).testTag("next-when"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(identity.name, style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp), fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).alignByBaseline().testTag("next-name"))
                if (identity.code.isNotBlank()) Text(identity.code, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp).alignByBaseline().testTag("next-code"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                LocationLabel(event.location, modifier = Modifier.weight(1f).testTag("next-location"),
                    colour = colours.foreground)
                if (date == today) Text(nextClassLabel(event, now).render(), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.testTag("next-countdown"))
                DetailsArrow(Modifier.testTag("next-details-chevron"), size = 20.dp, tint = colours.foreground)
            }
            if (timeline != null) DayTimelineBar(timeline, event.id, now, colourOf, colours.foreground,
                Modifier.fillMaxWidth().padding(top = 7.dp))
        }
    }
}

private val TrackHeight = 8.dp

/** Decorative day shape: the card and agenda rows carry the accessible class details. */
@Composable
internal fun DayTimelineBar(timeline: DayTimeline, highlightId: String, now: Long,
    colourOf: (EventContentItem) -> Color, foreground: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall
    val nowText = stringResource(AppLabels.NOW)
    val nowLabel = remember(nowText, style, measurer) {
        measurer.measure(nowText, style.copy(fontWeight = FontWeight.Bold))
    }
    // Only classes still to come get a start time, so the labels count what is left today.
    val startLabels = remember(timeline, now, style, measurer) {
        timeline.blocks.filter { it.event.endMillis > now }
            .map { it.start to measurer.measure(timeLabel(it.event.startMillis), style) }
    }
    val labelHeight = with(LocalDensity.current) { nowLabel.size.height.toDp() }
    Canvas(modifier.height(TrackHeight + 4.dp + labelHeight).clearAndSetSemantics {}) {
        val track = TrackHeight.toPx()
        val gap = 2.dp.toPx()
        val trackSize = Size(size.width, track)
        drawRoundRect(foreground.copy(alpha = .1f), size = trackSize, cornerRadius = CornerRadius(track / 2))
        timeline.now?.let { position ->
            drawRoundRect(foreground.copy(alpha = .16f), size = Size(position * size.width, track),
                cornerRadius = CornerRadius(track / 2))
        }
        val laneHeight = (track - gap * (timeline.lanes - 1)) / timeline.lanes
        timeline.blocks.forEach { block ->
            val alpha = when {
                block.event.id == highlightId -> 1f
                block.event.endMillis <= now -> .3f
                else -> .55f
            }
            drawRoundRect(colourOf(block.event).copy(alpha = alpha),
                topLeft = Offset(block.start * size.width, block.lane * (laneHeight + gap)),
                size = Size(maxOf((block.end - block.start) * size.width - gap, 4.dp.toPx()), laneHeight),
                cornerRadius = CornerRadius(laneHeight / 2))
        }
        val labelTop = track + 4.dp.toPx()
        val spacing = 6.dp.toPx()
        val placed = mutableListOf<ClosedFloatingPointRange<Float>>()
        fun place(layout: TextLayoutResult, x: Float): Float? {
            val left = x.coerceIn(0f, size.width - layout.size.width)
            val span = left - spacing..left + layout.size.width + spacing
            if (placed.any { it.start < span.endInclusive && span.start < it.endInclusive }) return null
            placed += span
            return left
        }
        timeline.now?.let { position ->
            val x = position * size.width
            drawLine(foreground, Offset(x, -3.dp.toPx()), Offset(x, track + 3.dp.toPx()),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            place(nowLabel, x - nowLabel.size.width / 2f)?.let {
                drawText(nowLabel, color = foreground, topLeft = Offset(it, labelTop))
            }
        }
        startLabels.forEach { (position, layout) ->
            place(layout, position * size.width)?.let {
                drawText(layout, color = foreground.copy(alpha = .7f), topLeft = Offset(it, labelTop))
            }
        }
    }
}
