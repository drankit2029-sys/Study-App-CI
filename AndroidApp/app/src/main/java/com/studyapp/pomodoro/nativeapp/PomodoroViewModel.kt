package com.studyapp.pomodoro.nativeapp

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val settings: SessionSettings = DefaultSettings,
    val phase: Phase = Phase.Focus,
    val remaining: Int = secondsFor(Phase.Focus, DefaultSettings),
    val isRunning: Boolean = false,
    val completedFocus: Int = 0,
    val focusedMinutes: Int = 0,
    val completion: CompletionInfo? = null,
) {
    val totalSeconds: Int
        get() = secondsFor(phase, settings)

    val progress: Float
        get() = if (totalSeconds == 0) 0f else 1f - remaining.toFloat() / totalSeconds

    val canReset: Boolean
        get() = isRunning || remaining < totalSeconds

    val sessionNumber: Int
        get() = (completedFocus % SessionsPerCycle) + 1

    val filledDots: Int
        get() = if (
            phase == Phase.LongBreak &&
            completedFocus > 0 &&
            completedFocus % SessionsPerCycle == 0
        ) {
            SessionsPerCycle
        } else {
            completedFocus % SessionsPerCycle
        }

    val sessionLabel: String
        get() = when (phase) {
            Phase.Focus -> "Session $sessionNumber of $SessionsPerCycle"
            Phase.LongBreak -> "Long recovery"
            Phase.ShortBreak -> "Short recovery"
        }

    val actionLabel: String
        get() = when {
            isRunning -> "Pause"
            remaining < totalSeconds -> "Resume"
            phase == Phase.Focus -> "Start focus"
            else -> "Start break"
        }

    val statusText: String
        get() = when {
            isRunning -> "The room is quiet. Your focus is protected."
            remaining < totalSeconds -> "Paused. Begin again when you are ready."
            else -> "Begin gently. The screen will stay awake while you focus."
        }
}

class PomodoroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var deadlineElapsed: Long? = null
    private var ticker: Job? = null

    fun toggle() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    fun reset() {
        deadlineElapsed = null
        stopTicker()
        val current = _uiState.value
        _uiState.update {
            it.copy(
                isRunning = false,
                remaining = secondsFor(current.phase, current.settings),
                completion = null,
            )
        }
    }

    fun selectPhase(phase: Phase) {
        val current = _uiState.value
        if (phase == current.phase || current.isRunning) return
        deadlineElapsed = null
        stopTicker()
        _uiState.update {
            it.copy(
                phase = phase,
                remaining = secondsFor(phase, current.settings),
                isRunning = false,
                completion = null,
            )
        }
    }

    fun updateSetting(phase: Phase, value: Int) {
        val current = _uiState.value
        val bounds = SettingBounds.getValue(phase)
        val nextValue = value.coerceIn(bounds.min, bounds.max)
        if (nextValue == current.settings.minutesFor(phase)) return

        val nextSettings = when (phase) {
            Phase.Focus -> current.settings.copy(focus = nextValue)
            Phase.ShortBreak -> current.settings.copy(short = nextValue)
            Phase.LongBreak -> current.settings.copy(long = nextValue)
        }
        val wasFullLength = current.remaining == secondsFor(current.phase, current.settings)
        _uiState.update {
            it.copy(
                settings = nextSettings,
                remaining = if (
                    phase == current.phase &&
                    !current.isRunning &&
                    wasFullLength
                ) {
                    secondsFor(current.phase, nextSettings)
                } else {
                    it.remaining
                },
            )
        }
    }

    fun dismissCompletion() {
        _uiState.update { it.copy(completion = null) }
    }

    fun startNext() {
        _uiState.update { it.copy(completion = null) }
        start()
    }

    private fun start() {
        val current = _uiState.value
        if (current.remaining <= 0) return
        deadlineElapsed = SystemClock.elapsedRealtime() + current.remaining * 1000L
        _uiState.update { it.copy(isRunning = true) }
        startTicker()
    }

    private fun pause() {
        val deadline = deadlineElapsed
        if (deadline != null) {
            val nextRemaining = remainingSeconds(deadline)
            deadlineElapsed = null
            if (nextRemaining <= 0) {
                finishPhase()
                return
            }
            _uiState.update { it.copy(remaining = nextRemaining) }
        }
        stopTicker()
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun finishPhase() {
        val current = _uiState.value
        val nextCompletedFocus = if (current.phase == Phase.Focus) {
            current.completedFocus + 1
        } else {
            current.completedFocus
        }
        val nextFocusedMinutes = if (current.phase == Phase.Focus) {
            current.focusedMinutes + current.settings.focus
        } else {
            current.focusedMinutes
        }
        val nextPhase = if (current.phase == Phase.Focus) {
            if (nextCompletedFocus % SessionsPerCycle == 0) Phase.LongBreak else Phase.ShortBreak
        } else {
            Phase.Focus
        }

        deadlineElapsed = null
        stopTicker()
        _uiState.update {
            it.copy(
                phase = nextPhase,
                remaining = secondsFor(nextPhase, current.settings),
                isRunning = false,
                completedFocus = nextCompletedFocus,
                focusedMinutes = nextFocusedMinutes,
                completion = CompletionInfo(
                    finished = current.phase,
                    next = nextPhase,
                    completedFocus = nextCompletedFocus,
                ),
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(250)
                val deadline = deadlineElapsed ?: break
                val nextRemaining = remainingSeconds(deadline)
                if (nextRemaining <= 0) {
                    finishPhase()
                    break
                }
                _uiState.update {
                    if (it.remaining == nextRemaining) it else it.copy(remaining = nextRemaining)
                }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun remainingSeconds(deadline: Long): Int {
        val remainingMillis = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        return ((remainingMillis + 999L) / 1000L).toInt()
    }
}
