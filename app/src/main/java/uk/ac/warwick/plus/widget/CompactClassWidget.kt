package uk.ac.warwick.plus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.CachedTimetable
import uk.ac.warwick.plus.ui.*

/** 2×1 picker entry; it renders and resizes exactly like the 2×2 one. */
class MiniClassWidget : CompactClassWidget()

/** Small next class: heading, time, name and location from the cached timetable. */
open class CompactClassWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        NextClassWidget.refresh(this, context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) =
        NextClassWidget.refresh(this, context)

    override fun onDisabled(context: Context) = NextClassWidget.refresh(this, context)

    companion object {
        private const val SQUARE_MIN_HEIGHT = 130

        /** Android 12+ launchers pick the layout per size; older ones report the portrait height in options. */
        internal fun render(context: Context, cache: CachedTimetable, now: Long, theme: ColourTheme,
            options: Bundle, defaultSquare: Boolean): RemoteViews {
            val square = views(context, R.layout.widget_compact_class, cache, now, theme)
            val row = views(context, R.layout.widget_compact_class_row, cache, now, theme)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return RemoteViews(mapOf(
                SizeF(100f, 40f) to row, SizeF(100f, SQUARE_MIN_HEIGHT.toFloat()) to square))
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            return if (if (height > 0) height >= SQUARE_MIN_HEIGHT else defaultSquare) square else row
        }

        private fun views(context: Context, layout: Int, cache: CachedTimetable, now: Long, theme: ColourTheme): RemoteViews {
            val colours = theme.emphasisColours()
            val foreground = colours.foreground.toArgb()
            val muted = ColorUtils.setAlphaComponent(foreground, 0xB3)
            val views = RemoteViews(context.packageName, layout)
            views.setInt(R.id.widget_background, "setColorFilter", colours.background.toArgb())
            listOf(R.id.widget_label, R.id.widget_start, R.id.widget_title).forEach { views.setTextColor(it, foreground) }
            listOf(R.id.widget_end, R.id.widget_location).forEach { views.setTextColor(it, muted) }
            views.setOnClickPendingIntent(R.id.widget_root, NextClassWidget.openApp(context))
            val next = currentOrNextClass(cache.events, now)
            if (cache.sync == null || next == null) {
                views.setTextViewText(R.id.widget_label, context.getString(AppLabels.NEXT))
                views.setTextViewText(R.id.widget_title, context.getString(
                    if (cache.sync == null) R.string.widget_not_loaded else R.string.no_upcoming_classes))
                listOf(R.id.widget_start, R.id.widget_end, R.id.widget_location).forEach { views.setViewVisibility(it, View.GONE) }
                return views
            }
            val today = atWarwick(now).toLocalDate()
            val start = atWarwick(next.startMillis).toLocalDate()
            val end = atWarwick(next.endMillis).toLocalDate()
            views.setTextViewText(R.id.widget_label, nextClassHeading(next, today, now).resolve(context.resources))
            views.setTextViewText(R.id.widget_start, if (next.allDay) context.getString(R.string.all_day) else timeLabel(next.startMillis))
            views.setTextViewText(R.id.widget_end, when {
                next.allDay -> ""
                end == start -> "– ${timeLabel(next.endMillis)}"
                else -> "– ${weekdayDateLabel(end)} ${timeLabel(next.endMillis)}"
            })
            views.setViewVisibility(R.id.widget_start, View.VISIBLE)
            views.setViewVisibility(R.id.widget_end, if (next.allDay) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widget_title, classIdentity(next, context.getString(R.string.class_fallback)).name)
            views.setTextViewText(R.id.widget_location, next.location)
            views.setViewVisibility(R.id.widget_location, if (next.location.isBlank()) View.GONE else View.VISIBLE)
            return views
        }
    }
}
