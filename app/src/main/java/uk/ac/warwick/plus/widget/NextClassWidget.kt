package uk.ac.warwick.plus.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.MainActivity
import uk.ac.warwick.plus.PlusApplication
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.data.CachedTimetable
import uk.ac.warwick.plus.data.EventContentItem
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.ui.*

/** Read-only view of the cached timetable; it never signs in or fetches by itself. */
class NextClassWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        refresh(context)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) refresh(context) else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) = cancelRefresh(context)

    private fun refresh(context: Context) {
        val app = context.applicationContext as PlusApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { update(context, app.repository.cached()) } finally { pending.finish() }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "uk.ac.warwick.plus.widget.REFRESH"
        private const val STEP_MILLIS = 15 * 60_000L

        fun update(context: Context, cache: CachedTimetable, now: Long = System.currentTimeMillis()) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextClassWidget::class.java))
            if (ids.isEmpty()) { cancelRefresh(context); return }
            val theme = (context.applicationContext as PlusApplication).appearance.state.value.theme
            ids.forEach { id ->
                val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                manager.updateAppWidget(id, render(context, cache, now, theme, width))
            }
            scheduleRefresh(context, nextRefresh(cache.events, now))
        }

        /** The Now marker moves every quarter hour during the day; otherwise wait for the next class edge. */
        internal fun nextRefresh(events: List<EventContentItem>, now: Long): Long {
            val today = atWarwick(now).toLocalDate()
            val edges = events.asSequence().filter { !it.allDay }
                .flatMap { sequenceOf(it.startMillis, it.endMillis) }.filter { it > now }
            val midnight = today.plusDays(1).atStartOfDay(WarwickZone).toInstant().toEpochMilli()
            val step = if (dayTimeline(events, today, now)?.now?.let { it > 0f && it < 1f } == true) now + STEP_MILLIS else Long.MAX_VALUE
            return minOf(edges.minOrNull() ?: Long.MAX_VALUE, midnight, step)
        }

        private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(context, 1,
            Intent(context, NextClassWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        private fun scheduleRefresh(context: Context, at: Long) {
            // RTC without wake-up: an asleep phone has no visible widget to refresh.
            context.getSystemService(AlarmManager::class.java)
                .setWindow(AlarmManager.RTC, at, 60_000L, refreshIntent(context))
        }

        private fun cancelRefresh(context: Context) =
            context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))

        private fun render(context: Context, cache: CachedTimetable, now: Long, theme: ColourTheme, widthDp: Int): RemoteViews {
            val colours = theme.emphasisColours()
            val background = colours.background.toArgb()
            val foreground = colours.foreground.toArgb()
            val muted = ColorUtils.setAlphaComponent(foreground, 0xB3)
            val views = RemoteViews(context.packageName, R.layout.widget_next_class)
            views.setInt(R.id.widget_background, "setColorFilter", background)
            listOf(R.id.widget_label, R.id.widget_time, R.id.widget_title).forEach { views.setTextColor(it, foreground) }
            views.setTextColor(R.id.widget_detail, muted)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            val today = atWarwick(now).toLocalDate()
            val next = currentOrNextClass(cache.events, now)
            if (cache.sync == null || next == null) {
                views.setTextViewText(R.id.widget_label, context.getString(AppLabels.NEXT))
                views.setTextViewText(R.id.widget_time, "")
                views.setTextViewText(R.id.widget_title, context.getString(
                    if (cache.sync == null) R.string.widget_not_loaded else R.string.no_upcoming_classes))
                views.setViewVisibility(R.id.widget_detail, View.GONE)
                views.setViewVisibility(R.id.widget_timeline, View.GONE)
                return views
            }
            val date = atWarwick(next.startMillis).toLocalDate()
            val label = when {
                isClassNow(next, now) -> context.getString(AppLabels.NOW)
                date <= today -> context.getString(AppLabels.NEXT)
                date == today.plusDays(1) -> context.getString(R.string.tomorrow)
                else -> weekdayDateLabel(date, includeYear = date.year != today.year)
            }
            val identity = classIdentity(next, context.getString(R.string.class_fallback))
            views.setTextViewText(R.id.widget_label, label)
            views.setTextViewText(R.id.widget_time, classTimeRange(next, includeWeekday = true, context.getString(R.string.all_day)))
            views.setTextViewText(R.id.widget_title, identity.name)
            val detail = listOf(identity.code, next.location).filter { it.isNotBlank() }.joinToString(" · ")
            views.setTextViewText(R.id.widget_detail, detail)
            views.setViewVisibility(R.id.widget_detail, if (detail.isBlank()) View.GONE else View.VISIBLE)
            val timeline = if (date == today) dayTimeline(cache.events, today, now) else null
            if (timeline == null) views.setViewVisibility(R.id.widget_timeline, View.GONE) else {
                val onLight = colours.background.luminance() > .5f
                val indices = moduleColourIndex(cache.events)
                val bitmap = drawTimeline(context, timeline, next.id, now, foreground, widthDp - 28) { event ->
                    moduleColour(indices[moduleKey(event)] ?: 0, onLight).toArgb()
                }
                views.setImageViewBitmap(R.id.widget_timeline, bitmap)
                views.setViewVisibility(R.id.widget_timeline, View.VISIBLE)
            }
            return views
        }

        private fun drawTimeline(context: Context, timeline: DayTimeline, highlightId: String, now: Long,
            foreground: Int, widthDp: Int, colourOf: (EventContentItem) -> Int): Bitmap {
            val metrics = context.resources.displayMetrics
            fun dp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics)
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, metrics)
            }
            val track = dp(8f)
            val labelTop = track + dp(4f)
            val width = dp(widthDp.coerceAtLeast(120).toFloat()).toInt()
            val height = (labelTop + text.fontSpacing).toInt() + 1
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            fun alpha(colour: Int, value: Float) = ColorUtils.setAlphaComponent(colour, (value * 255).toInt())
            paint.color = alpha(foreground, .1f)
            canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), track), track / 2, track / 2, paint)
            timeline.now?.let {
                paint.color = alpha(foreground, .16f)
                canvas.drawRoundRect(RectF(0f, 0f, it * width, track), track / 2, track / 2, paint)
            }
            val gap = dp(2f)
            val lane = (track - gap * (timeline.lanes - 1)) / timeline.lanes
            timeline.blocks.forEach { block ->
                paint.color = alpha(colourOf(block.event), when {
                    block.event.id == highlightId -> 1f
                    block.event.endMillis <= now -> .3f
                    else -> .55f
                })
                val top = block.lane * (lane + gap)
                val right = maxOf(block.end * width - gap, block.start * width + dp(4f))
                canvas.drawRoundRect(RectF(block.start * width, top, right, top + lane), lane / 2, lane / 2, paint)
            }
            val placed = mutableListOf<ClosedFloatingPointRange<Float>>()
            fun place(label: String, x: Float, colour: Int, bold: Boolean) {
                text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                val labelWidth = text.measureText(label)
                val left = x.coerceIn(0f, width - labelWidth)
                val span = left - dp(6f)..left + labelWidth + dp(6f)
                if (placed.any { it.start < span.endInclusive && span.start < it.endInclusive }) return
                placed += span
                text.color = colour
                canvas.drawText(label, left, labelTop - text.ascent(), text)
            }
            timeline.now?.let { position ->
                val x = position * width
                paint.color = foreground
                paint.strokeWidth = dp(2f)
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(x, dp(1f), x, track - dp(1f), paint)
                val label = context.getString(AppLabels.NOW)
                place(label, x - text.measureText(label) / 2, foreground, bold = true)
            }
            timeline.blocks.filter { it.event.endMillis > now }.forEach {
                place(timeLabel(it.event.startMillis), it.start * width, alpha(foreground, .7f), bold = false)
            }
            return bitmap
        }
    }
}