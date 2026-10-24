package com.studyapp.pomodoro.nativeapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.studyapp.pomodoro.nativeapp.service.PomodoroTimerService
import java.util.Locale
import kotlin.math.roundToInt

object PomodoroNotifications {

    const val CHANNEL_TIMER = "pomodoro_timer"
    const val CHANNEL_COMPLETION = "pomodoro_completion"
    const val ONGOING_NOTIFICATION_ID = 4101
    const val COMPLETION_NOTIFICATION_ID = 4102

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.notification_channel_timer),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_timer_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            context.getString(R.string.notification_channel_completion),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_completion_description)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(listOf(timerChannel, completionChannel))
    }

    fun buildOngoing(context: Context, state: PomodoroUiState): Notification {
        val phaseLabel = state.phase.heading.uppercase(Locale.getDefault())
        val clock = formatClock(state.remaining)
        val status = context.getString(R.string.notification_status_running)
        val openIntent = openAppIntent(context)
        val toggleIntent = servicePendingIntent(
            context = context,
            action = PomodoroTimerService.ACTION_TOGGLE,
            requestCode = 11,
            foreground = false,
        )
        val resetIntent = servicePendingIntent(
            context = context,
            action = PomodoroTimerService.ACTION_RESET,
            requestCode = 12,
            foreground = false,
        )

        val compact = RemoteViews(context.packageName, R.layout.notification_pomodoro_compact).apply {
            setTextViewText(R.id.notification_phase, phaseLabel)
            setTextViewText(R.id.notification_title, clock)
            setTextViewText(R.id.notification_body, status)
        }

        val expanded =
            RemoteViews(context.packageName, R.layout.notification_pomodoro_expanded).apply {
                setTextViewText(R.id.notification_phase, phaseLabel)
                setTextViewText(R.id.notification_title, clock)
                setTextViewText(R.id.notification_body, status)
                setProgressBar(
                    R.id.notification_progress,
                    1000,
                    (state.progress * 1000f).roundToInt().coerceIn(0, 1000),
                    false,
                )
                setViewVisibility(R.id.notification_progress, View.VISIBLE)
                setTextViewText(
                    R.id.notification_action_primary,
                    context.getString(
                        if (state.isRunning) {
                            R.string.notification_action_pause
                        } else {
                            R.string.notification_action_resume
                        },
                    ),
                )
                setTextViewText(
                    R.id.notification_action_secondary,
                    context.getString(R.string.notification_action_reset),
                )
                setTextViewText(
                    R.id.notification_action_tertiary,
                    context.getString(R.string.notification_action_open),
                )
                setViewVisibility(R.id.notification_action_tertiary, View.VISIBLE)
                setOnClickPendingIntent(R.id.notification_action_primary, toggleIntent)
                setOnClickPendingIntent(R.id.notification_action_secondary, resetIntent)
                setOnClickPendingIntent(R.id.notification_action_tertiary, openIntent)
            }

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_pomodoro)
            .setContentTitle("$phaseLabel · $clock")
            .setContentText(status)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(ContextCompat.getColor(context, R.color.forest))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .build()
    }

    fun showCompletion(context: Context, state: PomodoroUiState) {
        val completion = state.completion ?: return
        if (!canPostNotifications(context)) return

        val title = context.getString(
            if (completion.finished == Phase.Focus) {
                R.string.notification_complete_focus
            } else {
                R.string.notification_complete_break
            },
        )
        val nextLabel = when (completion.next) {
            Phase.Focus -> context.getString(R.string.notification_next_focus)
            Phase.ShortBreak -> context.getString(R.string.notification_next_short_break)
            Phase.LongBreak -> context.getString(R.string.notification_next_long_break)
        }
        val body = when {
            completion.finished == Phase.Focus && completion.next == Phase.LongBreak ->
                context.getString(R.string.notification_completion_long_break, nextLabel)

            completion.finished == Phase.Focus ->
                context.getString(
                    R.string.notification_completion_focus,
                    completion.completedFocus,
                    SessionsPerCycle,
                    nextLabel,
                )

            else ->
                context.getString(R.string.notification_completion_break, nextLabel)
        }
        val compactBody =
            context.getString(R.string.notification_completion_compact, nextLabel)
        val openIntent = openAppIntent(context)
        val startNextIntent = servicePendingIntent(
            context = context,
            action = PomodoroTimerService.ACTION_START_NEXT,
            requestCode = 21,
            foreground = true,
        )

        val compact = RemoteViews(context.packageName, R.layout.notification_pomodoro_compact).apply {
            setTextViewText(
                R.id.notification_phase,
                context.getString(R.string.notification_phase_complete),
            )
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_body, compactBody)
        }

        val expanded =
            RemoteViews(context.packageName, R.layout.notification_pomodoro_expanded).apply {
                setTextViewText(
                    R.id.notification_phase,
                    context.getString(R.string.notification_phase_complete),
                )
                setTextViewText(R.id.notification_title, title)
                setTextViewText(R.id.notification_body, body)
                setViewVisibility(R.id.notification_progress, View.GONE)
                setTextViewText(
                    R.id.notification_action_primary,
                    context.getString(R.string.notification_action_start_next),
                )
                setTextViewText(
                    R.id.notification_action_secondary,
                    context.getString(R.string.notification_action_open),
                )
                setViewVisibility(R.id.notification_action_tertiary, View.GONE)
                setOnClickPendingIntent(R.id.notification_action_primary, startNextIntent)
                setOnClickPendingIntent(R.id.notification_action_secondary, openIntent)
            }

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_stat_pomodoro)
            .setContentTitle(title)
            .setContentText(compactBody)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(context, R.color.forest))
            .setCustomContentView(compact)
            .setCustomBigContentView(expanded)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(COMPLETION_NOTIFICATION_ID, notification)
        }
    }

    fun cancelCompletion(context: Context) {
        NotificationManagerCompat.from(context).cancel(COMPLETION_NOTIFICATION_ID)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            31,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun servicePendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        foreground: Boolean,
    ): PendingIntent {
        val intent = Intent(context, PomodoroTimerService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (foreground) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }
}
