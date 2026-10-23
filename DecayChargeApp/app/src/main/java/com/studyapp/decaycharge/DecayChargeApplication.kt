package com.studyapp.decaycharge

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.notification.NotificationHelper
import com.studyapp.decaycharge.work.BackgroundScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class DecayChargeApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var controller: ActiveChargeController

    @Inject
    lateinit var preferences: UserPreferencesRepository

    @Inject
    lateinit var notifications: NotificationHelper

    @Inject
    lateinit var scheduler: BackgroundScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var strictFocusJob: Job? = null
    private var appVisible = false

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        controller.restore()
        appScope.launch { scheduler.scheduleAll() }
        appScope.launch {
            controller.state
                .map { it?.isRunning == true }
                .distinctUntilChanged()
                .collect { running ->
                    if (running && !appVisible) {
                        scheduleStrictFocusCheck()
                    } else if (!running) {
                        strictFocusJob?.cancel()
                    }
                }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    appVisible = true
                    strictFocusJob?.cancel()
                }

                override fun onStop(owner: LifecycleOwner) {
                    appVisible = false
                    scheduleStrictFocusCheck()
                }
            },
        )
    }

    private fun scheduleStrictFocusCheck() {
        val active = controller.state.value
        if (active?.isRunning != true) return
        strictFocusJob?.cancel()
        strictFocusJob = appScope.launch {
            delay(15_000L)
            if (appVisible) return@launch
            val prefs = preferences.preferences.first()
            if (!prefs.strictFocusEnabled) return@launch
            val current = controller.state.value
            if (current?.isRunning != true) return@launch
            notifications.showFocusWarning(
                chapterTitle = current.chapterTitle,
                chapterId = current.chapterId,
                secondsUntilPause = 30,
            )
            delay(30_000L)
            if (!appVisible && controller.state.value?.isRunning == true) {
                controller.pause()
            }
        }
    }
}
