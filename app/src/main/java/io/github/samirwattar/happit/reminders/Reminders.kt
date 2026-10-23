package io.github.samirwattar.happit.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.samirwattar.happit.MainActivity
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.logic.Streaks
import io.github.samirwattar.happit.repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime

object Reminders {
    const val CHANNEL = "reminders"
    const val EXTRA_HABIT = "habit_id"
    private const val DONE_REQUEST_OFFSET = 1_000_000

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                localized(ctx).getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun schedule(ctx: Context, habit: Habit) {
        cancel(ctx, habit.id)
        if (habit.reminder < 0) return
        val at = nextTrigger(habit, ZonedDateTime.now()) ?: return
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx, habit.id)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Next reminder time on a day the habit is scheduled, within the coming week. */
    fun nextTrigger(habit: Habit, now: ZonedDateTime): Long? {
        val time = LocalTime.of(habit.reminder / 60, habit.reminder % 60)
        for (i in 0..7) {
            val date = now.toLocalDate().plusDays(i.toLong())
            val at = date.atTime(time).atZone(now.zone)
            if (at.isAfter(now) && Streaks.scheduled(habit, date.toEpochDay())) {
                return at.toInstant().toEpochMilli()
            }
        }
        return null
    }

    fun cancel(ctx: Context, habitId: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx, habitId))
    }

    suspend fun rescheduleAll(ctx: Context) {
        ctx.repo.habits().forEach { schedule(ctx, it) }
    }

    private fun alarmIntent(ctx: Context, habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        habitId.toInt(),
        Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_HABIT, habitId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canNotify(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun notify(ctx: Context, habit: Habit, streak: Int) {
        if (!canNotify(ctx)) return
        val lc = localized(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (streak > 0) {
            lc.resources.getQuantityString(R.plurals.notif_streak, streak, streak)
        } else {
            lc.getString(R.string.notif_start)
        }
        val builder = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF7F77DD.toInt())
            .setContentTitle(lc.getString(R.string.notif_title, habit.name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (habit.type != HabitType.DURATION) {
            val done = PendingIntent.getBroadcast(
                ctx,
                DONE_REQUEST_OFFSET + habit.id.toInt(),
                Intent(ctx, DoneReceiver::class.java).putExtra(EXTRA_HABIT, habit.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, lc.getString(R.string.notif_done), done)
        }
        NotificationManagerCompat.from(ctx).notify(habit.id.toInt(), builder.build())
    }

    /** Receivers don't get the in-app language on older Android versions, so apply it here. */
    fun localized(ctx: Context): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return ctx
        val conf = Configuration(ctx.resources.configuration)
        conf.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
        return ctx.createConfigurationContext(conf)
    }
}

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_HABIT, -1)
        runAsync {
            val repo = ctx.repo
            val habit = repo.habit(id) ?: return@runAsync
            val today = Days.today()
            val summary = repo.summaryNow(today)
            if (Streaks.scheduled(habit, today) && !summary.isDone(habit.id, today)) {
                Reminders.notify(ctx, habit, summary.habitStats[habit.id]?.current ?: 0)
            }
            Reminders.schedule(ctx, habit)
        }
    }
}

class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_HABIT, -1)
        NotificationManagerCompat.from(ctx).cancel(id.toInt())
        runAsync {
            val habit = ctx.repo.habit(id) ?: return@runAsync
            ctx.repo.setProgress(habit, Days.today(), habit.target)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        runAsync { Reminders.rescheduleAll(ctx) }
    }
}
