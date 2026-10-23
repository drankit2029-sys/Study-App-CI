package com.studyapp.decaycharge.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.studyapp.decaycharge.MainActivity
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerForegroundService : Service() {
    @Inject
    lateinit var controller: ActiveChargeController

    @Inject
    lateinit var notifications: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collector: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (controller.state.value == null) {
            startInForeground(notifications.preparingNotification())
        }
        collector = scope.launch {
            combine(controller.state, controller.restoreComplete) { state, restored ->
                state to restored
            }.collectLatest { (state, restored) ->
                if (state == null) {
                    if (restored) stopService()
                } else {
                    startInForeground(notifications.timerNotification(state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        controller.restore()
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopService()
            return START_NOT_STICKY
        }
        if (controller.state.value == null) {
            startInForeground(notifications.preparingNotification())
        }
        scope.launch {
            controller.awaitRestore()
            when (action) {
                ACTION_TOGGLE_PAUSE -> controller.togglePause()
                ACTION_FINISH -> {
                    controller.finishEarly()
                    openCompletedChapter()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun stopService() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.ID_TIMER,
                notification,
                type,
            )
        }
    }

    private fun openCompletedChapter() {
        val state = controller.state.value ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_AUTO_LAUNCH_CHAPTER_ID, state.chapterId)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        collector?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.studyapp.decaycharge.action.START_TIMER"
        const val ACTION_TOGGLE_PAUSE = "com.studyapp.decaycharge.action.TOGGLE_PAUSE"
        const val ACTION_FINISH = "com.studyapp.decaycharge.action.FINISH"
        const val ACTION_STOP = "com.studyapp.decaycharge.action.STOP"

        fun intent(context: Context): Intent = Intent(context, TimerForegroundService::class.java)

        fun stop(context: Context) {
            runCatching { context.stopService(intent(context)) }
        }
    }
}
