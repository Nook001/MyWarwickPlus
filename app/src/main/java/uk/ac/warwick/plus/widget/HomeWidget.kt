package uk.ac.warwick.plus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import uk.ac.warwick.plus.R

enum class HomeWidget(val key: String, val provider: Class<out AppWidgetProvider>, val nameRes: Int, val detailRes: Int) {
    LARGE("large", NextClassWidget::class.java, R.string.widget_name, R.string.widget_large_detail),
    COMPACT("compact", CompactClassWidget::class.java, R.string.widget_compact_name, R.string.widget_compact_detail),
    MINI("mini", MiniClassWidget::class.java, R.string.widget_mini_name, R.string.widget_mini_detail);

    companion object {
        fun canPin(context: Context) = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    /** Asks the launcher to show its own add dialog; false when it refuses outright. */
    fun pin(context: Context): Boolean =
        AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, provider), null, null)
}
