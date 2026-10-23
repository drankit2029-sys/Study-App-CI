package com.studyapp.decaycharge.domain

import android.content.Context
import androidx.core.content.ContextCompat
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.notification.DndController
import com.studyapp.decaycharge.notification.NotificationHelper
import com.studyapp.decaycharge.service.TimerForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ActiveChargeState(
    val chapterId: Long,
    val chapterTitle: String,
    val subjectName: String,
    val subjectShortName: String,
    val subjectColorHex: Long,
    val mode: ChargeMode,
    val startedAtEpochMs: Long,
    val startBattery: Float,
    val targetSeconds: Int,
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
) {
    val isStopwatch: Boolean
        get() = mode == ChargeMode.STOPWATCH

    val progress: Float
        get() = if (targetSeconds <= 0) 0f
        else (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)

    val projectedBattery: Float
        get() = min(100f, startBattery + progress * (100f - startBattery))

    val remainingSeconds: Int
        get() = if (isStopwatch) 0 else (targetSeconds - elapsedSeconds).coerceAtLeast(0)
}

@Serializable
private data class PersistedTimer(
    val chapterId: Long,
    val chapterTitle: String,
    val subjectName: String,
    val subjectShortName: String,
    val subjectColorHex: Long,
    val mode: ChargeMode,
    val startedAtEpochMs: Long,
    val startBattery: Float,
    val targetSeconds: Int,
    val accumulatedActiveMs: Long = 0L,
    val runningSinceEpochMs: Long? = null,
    val isComplete: Boolean = false,
    val completionNotified: Boolean = false,
) {
    val isStopwatch: Boolean
        get() = mode == ChargeMode.STOPWATCH
}

@Singleton
class ActiveChargeController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: AppClock,
    private val preferences: UserPreferencesRepository,
    private val notifications: NotificationHelper,
    private val dnd: DndController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutableState = MutableStateFlow<ActiveChargeState?>(null)
    val state: StateFlow<ActiveChargeState?> = mutableState.asStateFlow()

    private var session: PersistedTimer? = null
    private var ticker: Job? = null
    private var restoreJob: Job? = null
    private var restored = false
    private val mutableRestoreComplete = MutableStateFlow(false)
    val restoreComplete: StateFlow<Boolean> = mutableRestoreComplete.asStateFlow()

    @Synchronized
    fun restore() {
        if (restored) return
        restored = true
        restoreJob = scope.launch {
            try {
                val prefs = preferences.preferences.first()
                val persisted = prefs.activeTimerSessionJson?.let { raw ->
                    runCatching { json.decodeFromString<PersistedTimer>(raw) }.getOrNull()
                }
                if (persisted == null) {
                    if (prefs.dndSuppressed) dnd.restore()
                    return@launch
                }
                val now = clock.nowEpochMs()
                val reachedTarget = persisted.reachedTarget(now)
                val complete = persisted.isComplete || reachedTarget
                val elapsed = persisted.elapsedMs(now)
                val active = persisted.copy(
                    accumulatedActiveMs = if (complete && !persisted.isStopwatch) {
                        min(elapsed, persisted.targetSeconds * 1000L)
                    } else {
                        elapsed
                    },
                    runningSinceEpochMs = if (complete) null else persisted.runningSinceEpochMs,
                    isComplete = complete,
                )
                session = active
                publish(active, persist = false)
                if (active.isComplete) {
                    dnd.restore()
                } else if (active.runningSinceEpochMs != null) {
                    startTicker()
                    dnd.reapplyIfSuppressed()
                }
            } finally {
                mutableRestoreComplete.value = true
            }
        }
    }

    suspend fun awaitRestore() {
        restore()
        restoreComplete.first { it }
    }

    @Synchronized
    fun start(chapter: Chapter, mode: ChargeMode) {
        restored = true
        restoreJob?.cancel()
        stopTicker()
        val now = clock.nowEpochMs()
        val targetMinutes = if (mode == ChargeMode.QUICK_TOP_UP) 15 else chapter.targetMinutes
        val next = PersistedTimer(
            chapterId = chapter.id,
            chapterTitle = chapter.title,
            subjectName = chapter.subjectName,
            subjectShortName = chapter.subjectShortName,
            subjectColorHex = chapter.subjectColorHex,
            mode = mode,
            startedAtEpochMs = now,
            startBattery = chapter.battery,
            targetSeconds = targetMinutes * 60,
            runningSinceEpochMs = now,
        )
        session = next
        publish(next)
        startTicker()
        startService()
        scope.launch { dnd.suppress() }
    }

    @Synchronized
    fun togglePause() {
        val current = session ?: return
        if (current.isComplete) return
        if (current.runningSinceEpochMs != null) pauseLocked(current) else resumeLocked(current)
    }

    @Synchronized
    fun pause() {
        val current = session ?: return
        if (current.runningSinceEpochMs == null || current.isComplete) return
        pauseLocked(current)
    }

    private fun pauseLocked(current: PersistedTimer) {
        val now = clock.nowEpochMs()
        val elapsed = current.elapsedMs(now)
        val next = current.copy(
            accumulatedActiveMs = if (current.isStopwatch) elapsed else min(elapsed, current.targetSeconds * 1000L),
            runningSinceEpochMs = null,
        )
        if (!current.isStopwatch && next.reachedTarget(now)) {
            finishLocked(next, notify = true)
            return
        }
        session = next
        stopTicker()
        publish(next)
        scope.launch { dnd.restore() }
    }

    @Synchronized
    fun resume() {
        val current = session ?: return
        if (current.isComplete || current.runningSinceEpochMs != null) return
        resumeLocked(current)
    }

    private fun resumeLocked(current: PersistedTimer) {
        val next = current.copy(
            runningSinceEpochMs = clock.nowEpochMs(),
            completionNotified = false,
        )
        session = next
        publish(next)
        startTicker()
        startService()
        scope.launch { dnd.suppress() }
    }

    @Synchronized
    fun finishEarly() {
        val current = session ?: return
        if (current.isComplete) return
        val now = clock.nowEpochMs()
        val elapsed = current.elapsedMs(now)
        val next = current.copy(
            accumulatedActiveMs = if (current.isStopwatch) elapsed else min(elapsed, current.targetSeconds * 1000L),
            runningSinceEpochMs = null,
            isComplete = true,
            completionNotified = true,
        )
        session = next
        stopTicker()
        publish(next)
        scope.launch { dnd.restore() }
    }

    @Synchronized
    fun clear() {
        stopTicker()
        restoreJob?.cancel()
        session = null
        mutableState.value = null
        scope.launch {
            preferences.setActiveTimerSession(null)
            dnd.restore()
        }
        notifications.cancelTimer()
        TimerForegroundService.stop(context)
    }

    fun ensureForegroundService() {
        if (session != null) startService()
    }

    private fun startService() {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                TimerForegroundService.intent(context).setAction(TimerForegroundService.ACTION_START),
            )
        }
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                delay(500)
                tick()
            }
        }
    }

    @Synchronized
    private fun tick() {
        val current = session ?: run {
            stopTicker()
            return
        }
        if (current.runningSinceEpochMs == null || current.isComplete) {
            stopTicker()
            return
        }
        val now = clock.nowEpochMs()
        val elapsed = current.elapsedMs(now)
        if (!current.isStopwatch && current.targetSeconds > 0 && elapsed >= current.targetSeconds * 1000L) {
            val finished = current.copy(
                accumulatedActiveMs = current.targetSeconds * 1000L,
                runningSinceEpochMs = null,
                isComplete = true,
            )
            finishLocked(finished, notify = true)
            return
        }
        publish(current, persist = false)
    }

    private fun finishLocked(finished: PersistedTimer, notify: Boolean) {
        stopTicker()
        val shouldNotify = notify && !finished.completionNotified
        val next = finished.copy(completionNotified = finished.completionNotified || shouldNotify)
        session = next
        publish(next)
        scope.launch { dnd.restore() }
        if (shouldNotify) {
            next.toState().let { completed ->
                scope.launch { notifications.showCompletion(completed) }
            }
        }
    }

    private fun publish(source: PersistedTimer, persist: Boolean = true) {
        val next = source.toState()
        mutableState.value = next
        if (persist) {
            val raw = json.encodeToString(PersistedTimer.serializer(), source)
            scope.launch { preferences.setActiveTimerSession(raw) }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun PersistedTimer.elapsedMs(now: Long): Long {
        val runningMs = runningSinceEpochMs?.let { max(0L, now - it) } ?: 0L
        return (accumulatedActiveMs + runningMs).coerceAtLeast(0L)
    }

    private fun PersistedTimer.reachedTarget(now: Long): Boolean =
        !isStopwatch && targetSeconds > 0 && elapsedMs(now) >= targetSeconds * 1000L

    private fun PersistedTimer.toState(): ActiveChargeState {
        val elapsedMs = if (runningSinceEpochMs != null) {
            elapsedMs(clock.nowEpochMs())
        } else {
            accumulatedActiveMs
        }
        val elapsedSeconds = (elapsedMs / 1000L).toInt()
        val complete = isComplete || (!isStopwatch && elapsedSeconds >= targetSeconds)
        val displaySeconds = if (!isStopwatch && complete) targetSeconds else elapsedSeconds
        return ActiveChargeState(
            chapterId = chapterId,
            chapterTitle = chapterTitle,
            subjectName = subjectName,
            subjectShortName = subjectShortName,
            subjectColorHex = subjectColorHex,
            mode = mode,
            startedAtEpochMs = startedAtEpochMs,
            startBattery = startBattery,
            targetSeconds = targetSeconds,
            elapsedSeconds = displaySeconds,
            isRunning = runningSinceEpochMs != null && !complete,
            isComplete = complete,
        )
    }
}
