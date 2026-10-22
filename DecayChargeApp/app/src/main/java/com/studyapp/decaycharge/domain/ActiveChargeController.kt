package com.studyapp.decaycharge.domain

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChargeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val progress: Float
        get() = if (targetSeconds <= 0) 0f
        else (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)

    val projectedBattery: Float
        get() = min(100f, startBattery + progress * (100f - startBattery))

    val remainingSeconds: Int
        get() = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
}

@Singleton
class ActiveChargeController @Inject constructor(
    private val clock: AppClock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow<ActiveChargeState?>(null)
    val state: StateFlow<ActiveChargeState?> = mutableState.asStateFlow()

    private var runStartedAtMs: Long = 0L
    private var accumulatedMs: Long = 0L
    private var ticker: Job? = null

    fun start(chapter: Chapter, mode: ChargeMode) {
        stopTicker()
        accumulatedMs = 0L
        runStartedAtMs = clock.elapsedRealtimeMs()
        val targetMinutes = if (mode == ChargeMode.QUICK_TOP_UP) 15 else chapter.targetMinutes
        mutableState.value = ActiveChargeState(
            chapterId = chapter.id,
            chapterTitle = chapter.title,
            subjectName = chapter.subjectName,
            subjectShortName = chapter.subjectShortName,
            subjectColorHex = chapter.subjectColorHex,
            mode = mode,
            startedAtEpochMs = clock.nowEpochMs(),
            startBattery = chapter.battery,
            targetSeconds = targetMinutes * 60,
            isRunning = true,
        )
        startTicker()
    }

    fun togglePause() {
        val current = mutableState.value ?: return
        if (current.isComplete) return
        if (current.isRunning) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        val current = mutableState.value ?: return
        if (!current.isRunning) return
        accumulatedMs += clock.elapsedRealtimeMs() - runStartedAtMs
        stopTicker()
        val elapsed = (accumulatedMs / 1000L).toInt().coerceAtMost(current.targetSeconds)
        mutableState.value = current.copy(
            elapsedSeconds = elapsed,
            isRunning = false,
        )
    }

    fun resume() {
        val current = mutableState.value ?: return
        if (current.isComplete || current.isRunning) return
        runStartedAtMs = clock.elapsedRealtimeMs()
        mutableState.value = current.copy(isRunning = true)
        startTicker()
    }

    fun finishEarly() {
        val current = mutableState.value ?: return
        if (current.isRunning) {
            accumulatedMs += clock.elapsedRealtimeMs() - runStartedAtMs
        }
        stopTicker()
        val elapsed = (accumulatedMs / 1000L).toInt().coerceAtMost(current.targetSeconds)
        mutableState.value = current.copy(
            elapsedSeconds = elapsed,
            isRunning = false,
            isComplete = true,
        )
    }

    fun clear() {
        stopTicker()
        accumulatedMs = 0L
        mutableState.value = null
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                delay(500)
                val current = mutableState.value ?: break
                if (!current.isRunning) break
                val elapsedMs = accumulatedMs + (clock.elapsedRealtimeMs() - runStartedAtMs)
                val elapsedSeconds = (elapsedMs / 1000L).toInt()
                if (elapsedSeconds >= current.targetSeconds) {
                    accumulatedMs = current.targetSeconds * 1000L
                    mutableState.value = current.copy(
                        elapsedSeconds = current.targetSeconds,
                        isRunning = false,
                        isComplete = true,
                    )
                    break
                }
                mutableState.value = current.copy(
                    elapsedSeconds = elapsedSeconds,
                    isRunning = true,
                )
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }
}
