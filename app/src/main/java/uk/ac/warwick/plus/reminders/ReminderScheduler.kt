package uk.ac.warwick.plus.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.ac.warwick.plus.MainActivity
import uk.ac.warwick.plus.PlusApplication
import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.data.CachedTimetable
import uk.ac.warwick.plus.ui.classIdentity
import uk.ac.warwick.plus.ui.classSummary
import uk.ac.warwick.plus.ui.deadlineDateLabel
import uk.ac.warwick.plus.ui.timeLabel

data class ReminderSettings(val classes: Boolean = false, val deadlines: Boolean = false)

/**
 * Local reminders from the cached timetable and coursework; nothing is fetched in the background.
 * Exactly one alarm is armed, for the earliest pending reminder, and re-armed after it fires.
 */
class ReminderScheduler(private val context: Context, private val preferences: SharedPreferences,
    private val cache: suspend () -> CachedTimetable) {
    private val current = MutableStateFlow(ReminderSettings(
        preferences.getBoolean(KEY_CLASSES, false), preferences.getBoolean(KEY_DEADLINES, false)))
    val settings = current.asStateFlow()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun update(settings: ReminderSettings) {
        preferences.edit {
            putBoolean(KEY_CLASSES, settings.classes)
            putBoolean(KEY_DEADLINES, settings.deadlines)
        }
        current.value = settings
        scope.launch { reschedule(cache()) }
    }

    suspend fun fire() = reschedule(cache())

    /** Anything that became due since the last pass is shown first, so a late alarm or a cache change never skips it. */
    suspend fun reschedule(snapshot: CachedTimetable) = mutex.withLock {
        val now = System.currentTimeMillis()
        val all = plan(snapshot)
        val after = preferences.getLong(KEY_CURSOR, now)
        if (canNotify()) dueReminders(all, after, now).forEach { show(it, snapshot, now) }
        preferences.edit { putLong(KEY_CURSOR, now) }
        arm(all, now)
    }

    private fun plan(cache: CachedTimetable): List<Reminder> {
        val settings = current.value
        return reminders(cache.events, cache.coursework, settings.classes, settings.deadlines)
    }

    private fun arm(all: List<Reminder>, now: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(context, 0,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val next = nextTrigger(all, now)
        if (next == null) { alarms.cancel(intent); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms())
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
        else alarms.setWindow(AlarmManager.RTC_WAKEUP, next, 5 * 60_000L, intent)
    }

    private fun show(reminder: Reminder, cache: CachedTimetable, now: Long) {
        val manager = NotificationManagerCompat.from(context)
        val (channel, title, text) = when (reminder.kind) {
            ReminderKind.CLASS -> {
                val event = cache.events.firstOrNull { it.id == reminder.id } ?: return
                val identity = classIdentity(event, context.getString(R.string.class_fallback))
                Triple(CHANNEL_CLASSES,
                    context.getString(R.string.reminder_class_title, identity.name, timeLabel(event.startMillis)),
                    classSummary(identity, event))
            }
            ReminderKind.DEADLINE -> {
                val entry = cache.coursework.firstOrNull { it.id == reminder.id } ?: return
                Triple(CHANNEL_DEADLINES, context.getString(R.string.reminder_deadline_title, entry.title),
                    context.getString(R.string.reminder_deadline_text, deadlineDateLabel(entry.dueMillis, now, includeTime = true)))
            }
        }
        ensureChannels()
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text)
            .setCategory(if (reminder.kind == ReminderKind.CLASS) NotificationCompat.CATEGORY_EVENT else NotificationCompat.CATEGORY_REMINDER)
            .setWhen(reminder.targetAt).setShowWhen(true)
            .setTimeoutAfter(reminder.targetAt - now + if (reminder.kind == ReminderKind.CLASS) 30 * 60_000L else 0L)
            .setContentIntent(open).setAutoCancel(true).build()
        try {
            manager.notify("${reminder.kind}:${reminder.id}".hashCode(), notification)
        } catch (_: SecurityException) { }
    }

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_CLASSES, context.getString(R.string.reminder_channel_classes), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_DEADLINES, context.getString(R.string.reminder_channel_deadlines), NotificationManager.IMPORTANCE_DEFAULT)))
    }

    private companion object {
        const val KEY_CLASSES = "class_reminders"
        const val KEY_DEADLINES = "deadline_reminders"
        const val KEY_CURSOR = "reminder_cursor"
        const val CHANNEL_CLASSES = "class_reminders"
        const val CHANNEL_DEADLINES = "deadline_reminders"
    }
}

internal const val ACTION_FIRE = "uk.ac.warwick.plus.reminders.FIRE"
private val REPLAN_ACTIONS = setOf(ACTION_FIRE, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)

/** Alarm, reboot, update and clock changes all re-plan from the cache. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REPLAN_ACTIONS) return
        val app = context.applicationContext as PlusApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.reminders.fire()
            } finally { pending.finish() }
        }
    }
}
