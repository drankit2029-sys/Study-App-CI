package com.studyapp.pomodoro.nativeapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.PomodoroNotifications
import com.studyapp.pomodoro.nativeapp.PomodoroTimerEngine
import com.studyapp.pomodoro.nativeapp.PomodoroUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PomodoroTimerService : Service() {

    private val engine by lazy { PomodoroTimerEngine.getInstance(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var stateJob: Job? = null
    private var foregroundStarted = false
    private var lastSignature: NotificationSignature? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        PomodoroNotifications.createChannels(this)
        stateJob = serviceScope.launch {
            engine.state.collect { state ->
                if (!foregroundStarted) return@collect
                if (!state.isRunning || state.completion != null) {
                    stopSelf()
                    return@collect
                }

                val signature = NotificationSignature.from(state)
                if (signature != lastSignature) {
                    lastSignature = signature
                    updateForeground(state)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> engine.toggle()
            ACTION_RESET -> engine.reset()
            ACTION_START_NEXT -> engine.startNext(shouldStartService = false)
            ACTION_START, null -> engine.ensureTicker()
        }

        val state = engine.state.value
        if (state.isRunning && state.completion == null) {
            ensureForeground(state)
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    private fun ensureForeground(state: PomodoroUiState) {
        val notification = PomodoroNotifications.buildOngoing(this, state)
        ServiceCompat.startForeground(
            this,
            PomodoroNotifications.ONGOING_NOTIFICATION_ID,
            notification,
            foregroundServiceType(),
        )
        foregroundStarted = true
        lastSignature = NotificationSignature.from(state)
        acquireWakeLock()
    }

    private fun updateForeground(state: PomodoroUiState) {
        val notification = PomodoroNotifications.buildOngoing(this, state)
        ServiceCompat.startForeground(
            this,
            PomodoroNotifications.ONGOING_NOTIFICATION_ID,
            notification,
            foregroundServiceType(),
        )
        foregroundStarted = true
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Pomodoro::Timer",
        ).apply {
            setReferenceCounted(false)
            acquire(MAX_WAKE_LOCK_MILLIS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }

    private data class NotificationSignature(
        val phase: Phase,
        val isRunning: Boolean,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val hasCompletion: Boolean,
    ) {
        companion object {
            fun from(state: PomodoroUiState): NotificationSignature = NotificationSignature(
                phase = state.phase,
                isRunning = state.isRunning,
                remainingSeconds = state.remaining,
                totalSeconds = state.totalSeconds,
                hasCompletion = state.completion != null,
            )
        }
    }

    companion object {
        const val ACTION_START = "com.studyapp.pomodoro.nativeapp.action.START"
        const val ACTION_TOGGLE = "com.studyapp.pomodoro.nativeapp.action.TOGGLE"
        const val ACTION_RESET = "com.studyapp.pomodoro.nativeapp.action.RESET"
        const val ACTION_START_NEXT = "com.studyapp.pomodoro.nativeapp.action.START_NEXT"

        private const val MAX_WAKE_LOCK_MILLIS = 2L * 60L * 60L * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, PomodoroTimerService::class.java).setAction(ACTION_START),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PomodoroTimerService::class.java))
        }
    }
}
