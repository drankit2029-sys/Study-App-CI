package com.studyapp.pomodoro.nativeapp

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object PomodoroAlarmPlayer {

    private const val LOG_TAG = "PomodoroAlarmPlayer"
    private const val PREVIEW_SECONDS = 5
    private const val WAKE_LOCK_EXTRA_MILLIS = 5_000L

    private val handler = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stopRunnable: Runnable? = null

    @Volatile
    var isRinging: Boolean = false
        private set

    fun play(context: Context, settings: AlarmSettings, preview: Boolean = false) {
        stop()
        if (!settings.enabled && !preview) return

        val appContext = context.applicationContext
        val durationSeconds = if (preview) {
            minOf(PREVIEW_SECONDS, settings.safeDurationSeconds)
        } else {
            settings.safeDurationSeconds
        }
        val soundStarted = if (settings.sound == SILENT_ALARM_SOUND) {
            false
        } else {
            startSound(appContext, settings.sound)
        }
        val vibrationStarted = if (settings.vibration) {
            startVibration(appContext)
        } else {
            false
        }
        if (!soundStarted && !vibrationStarted) return

        isRinging = true
        acquireWakeLock(appContext, durationSeconds)
        val runnable = Runnable { stop() }
        stopRunnable = runnable
        handler.postDelayed(runnable, durationSeconds * 1000L)
    }

    fun stop() {
        stopRunnable?.let(handler::removeCallbacks)
        stopRunnable = null
        player?.let { mediaPlayer ->
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
        isRinging = false
    }

    fun resolveAlarmUri(context: Context, sound: String): Uri? = when (sound) {
        SILENT_ALARM_SOUND -> null
        DEFAULT_ALARM_SOUND -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        else -> runCatching { Uri.parse(sound) }.getOrNull()
    }

    fun alarmSoundTitle(context: Context, sound: String): String = when (sound) {
        SILENT_ALARM_SOUND -> "Silent"
        DEFAULT_ALARM_SOUND -> "Default alarm"
        else -> runCatching {
            val uri = Uri.parse(sound)
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Alarm sound"
    }

    private fun startSound(context: Context, sound: String): Boolean {
        val primary = resolveAlarmUri(context, sound) ?: return false
        if (startMediaPlayer(context, primary)) return true
        if (sound == DEFAULT_ALARM_SOUND) return false

        val fallback = resolveAlarmUri(context, DEFAULT_ALARM_SOUND) ?: return false
        return startMediaPlayer(context, fallback)
    }

    private fun startMediaPlayer(context: Context, uri: Uri): Boolean = runCatching {
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            isLooping = true
            setOnErrorListener { _, what, extra ->
                Log.w(LOG_TAG, "Alarm playback error: what=$what extra=$extra")
                true
            }
            prepare()
            start()
        }
        player = mediaPlayer
        true
    }.getOrElse { error ->
        Log.w(LOG_TAG, "Unable to play alarm sound", error)
        false
    }

    @Suppress("DEPRECATION")
    private fun startVibration(context: Context): Boolean {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return false

        if (!device.hasVibrator()) return false
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0L, 700L, 250L, 700L, 300L, 1000L),
            0,
        )
        device.vibrate(effect)
        vibrator = device
        return true
    }

    private fun acquireWakeLock(context: Context, durationSeconds: Int) {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return
        runCatching {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Pomodoro::CompletionAlarm",
            ).apply {
                setReferenceCounted(false)
                acquire(durationSeconds * 1000L + WAKE_LOCK_EXTRA_MILLIS)
            }
        }.onFailure { error ->
            Log.w(LOG_TAG, "Unable to hold alarm wake lock", error)
        }
    }
}
