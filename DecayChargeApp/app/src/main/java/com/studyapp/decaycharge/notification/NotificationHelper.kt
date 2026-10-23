package com.studyapp.decaycharge.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.studyapp.decaycharge.MainActivity
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.domain.ActiveChargeState
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.service.TimerForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        createChannels()
    }

    fun timerNotification(state: ActiveChargeState): Notification {
        val contentIntent = chapterIntent(state.chapterId)
        val pauseIntent = serviceIntent(TimerForegroundService.ACTION_TOGGLE_PAUSE, 11)
        val finishIntent = serviceIntent(TimerForegroundService.ACTION_FINISH, 12)
        val progress = (state.progress * 1000).toInt().coerceIn(0, 1000)
        val clock = if (state.mode == ChargeMode.STOPWATCH) {
            formatClock(state.elapsedSeconds)
        } else {
            formatClock(state.remainingSeconds)
        }
        val modeLabel = when (state.mode) {
            ChargeMode.DEEP_CHARGE -> "Deep charge"
            ChargeMode.QUICK_TOP_UP -> "Quick top-up"
            ChargeMode.STOPWATCH -> "Stopwatch"
        }
        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("${state.chapterTitle} · $clock")
            .setContentText(
                "${state.startBattery.toInt()}% -> ${state.projectedBattery.toInt()}% · ${state.subjectName}",
            )
            .setSubText(if (state.isComplete) "Ready for recall check" else modeLabel)
            .setContentIntent(contentIntent)
            .setOngoing(!state.isComplete)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setProgress(1000, progress, false)
            .addAction(
                if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isRunning) "Pause" else "Resume",
                pauseIntent,
            )
            .addAction(android.R.drawable.ic_menu_save, "Finish & Save", finishIntent)
            .build()
    }

    fun preparingNotification(): Notification =
        NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Restoring your charge")
            .setContentText("A moment while the timer reconnects.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

    suspend fun showCompletion(state: ActiveChargeState) {
        val prefs = preferences.preferences.first()
        val vibration = VibrationEffect.createWaveform(longArrayOf(0L, 220L, 120L, 360L), -1)
        vibrate(vibration)
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Charge complete · ${state.chapterTitle}")
            .setContentText(
                "Held at ${state.projectedBattery.toInt()}%. Tap to check recall and save stability.",
            )
            .setContentIntent(chapterIntent(state.chapterId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0L, 220L, 120L, 360L))
        if (prefs.fullScreenAlertsEnabled && canUseFullScreen()) {
            builder.setFullScreenIntent(chapterIntent(state.chapterId), true)
        }
        notify(ID_COMPLETE, builder.build())
    }

    fun showFocusWarning(chapterTitle: String, chapterId: Long, secondsUntilPause: Int) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Your charge is still running")
            .setContentText(
                "Return to DecayCharge within ${secondsUntilPause}s or $chapterTitle will pause.",
            )
            .setContentIntent(chapterIntent(chapterId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        notify(ID_FOCUS, builder.build())
    }

    fun showDailyDigest(redCount: Int, lowestTitle: String?, lowestBattery: Float, chapterId: Long?) {
        if (redCount <= 0 || !canNotify()) return
        val text = if (lowestTitle != null) {
            "$redCount chapters need charge (lowest: $lowestTitle at ${lowestBattery.toInt()}%). Tap to start."
        } else {
            "$redCount chapters need charge. Tap to open Triage."
        }
        val intent = if (chapterId != null) {
            chapterIntent(chapterId)
        } else {
            PendingIntent.getActivity(
                context,
                30,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Morning triage")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        notify(ID_DIGEST, builder.build())
    }

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    fun cancelTimer() {
        NotificationManagerCompat.from(context).cancel(ID_TIMER)
    }

    private fun chapterIntent(chapterId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_AUTO_LAUNCH_CHAPTER_ID, chapterId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            (chapterId % 10_000).toInt() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun canUseFullScreen(): Boolean =
        Build.VERSION.SDK_INT < 34 ||
            manager.canUseFullScreenIntent()

    private fun vibrate(effect: VibrationEffect) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (vibrator.hasVibrator()) {
            runCatching { vibrator.vibrate(effect) }
        }
    }

    private fun createChannels() {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER,
                "Active charge",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Silent live timer and charge progress."
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETE,
                "Charge complete",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when a charge reaches its target."
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 220L, 120L, 360L)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOCUS,
                "Strict focus",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Warns when you leave an active charge."
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DIGEST,
                "Morning triage",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "One quiet daily summary of fading chapters."
            },
        )
    }

    private fun formatClock(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(safe / 60, safe % 60)
    }

    companion object {
        const val ID_TIMER = 1001
        const val ID_COMPLETE = 1002
        const val ID_FOCUS = 1003
        const val ID_DIGEST = 1004
        const val CHANNEL_TIMER = "charge_timer"
        const val CHANNEL_COMPLETE = "charge_complete"
        const val CHANNEL_FOCUS = "strict_focus"
        const val CHANNEL_DIGEST = "daily_digest"
    }
}
