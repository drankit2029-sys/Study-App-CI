package com.studyapp.pomodoro.nativeapp

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
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
    val sessionTotalMillisByPhase: Map<Phase, Long> = defaultDurationMillisMap(DefaultSettings),
    val remainingMillisByPhase: Map<Phase, Long> = defaultDurationMillisMap(DefaultSettings),
    val isRunning: Boolean = false,
    val completedFocus: Int = 0,
    val focusedMinutes: Int = 0,
    val completion: CompletionInfo? = null,
) {
    val configuredMillis: Long
        get() = millisFor(phase, settings)

    val totalMillis: Long
        get() = sessionTotalMillisByPhase[phase] ?: configuredMillis

    val totalSeconds: Int
        get() = (totalMillis / 1000L).toInt()

    val remainingMillis: Long
        get() = (remainingMillisByPhase[phase] ?: totalMillis).coerceIn(0L, totalMillis)

    val remaining: Int
        get() = ((remainingMillis + 999L) / 1000L).toInt()

    val isMidSession: Boolean
        get() = remainingMillis in 1L until totalMillis

    val progress: Float
        get() = if (totalMillis <= 0L) {
            0f
        } else {
            (1f - remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
        }

    val canReset: Boolean
        get() = isRunning || isMidSession || totalMillis != configuredMillis

    val sessionNumber: Int
        get() = (completedFocus % SessionsPerCycle) + 1

    val filledDots: Int
        get() = if (
            phase != Phase.Focus &&
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
            isMidSession -> "Resume"
            phase == Phase.Focus -> "Start focus"
            else -> "Start break"
        }

    val statusText: String
        get() = when {
            isRunning -> "The room is quiet. Your focus is protected."
            isMidSession -> "Paused. Begin again when you are ready."
            else -> "Begin gently. The screen will stay awake while you focus."
        }
}

class PomodoroViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreInitialState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var deadlineElapsed: Long? = savedStateHandle.get<Long>(KEY_DEADLINE)
    private var ticker: Job? = null

    init {
        val current = _uiState.value
        val savedDeadline = deadlineElapsed
        if (current.isRunning && savedDeadline != null) {
            val now = SystemClock.elapsedRealtime()
            val remainingMs = savedDeadline - now
            if (remainingMs <= 0L) {
                finishPhase()
            } else if (remainingMs <= current.totalMillis) {
                updateActiveRemainingMillis(remainingMs, forcePersist = true)
                startTicker()
            } else {
                // Device rebooted or monotonic clock reset; pause safely with saved remaining time
                deadlineElapsed = null
                _uiState.update { it.copy(isRunning = false) }
                persistState()
            }
        }
    }

    fun toggle() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    fun reset() {
        val current = _uiState.value
        deadlineElapsed = null
        stopTicker()
        val newDurationMs = millisFor(current.phase, current.settings)
        val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
            this[current.phase] = newDurationMs
        }
        val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
            this[current.phase] = newDurationMs
        }
        _uiState.update {
            it.copy(
                isRunning = false,
                sessionTotalMillisByPhase = updatedTotals,
                remainingMillisByPhase = updatedRemaining,
                completion = null,
            )
        }
        persistState()
    }

    fun selectPhase(phase: Phase) {
        val current = _uiState.value
        if (phase == current.phase || current.isRunning) return
        deadlineElapsed = null
        stopTicker()
        // Switching phases preserves each phase's paused remainingMillisByPhase & sessionTotalMillisByPhase
        _uiState.update {
            it.copy(
                phase = phase,
                isRunning = false,
                completion = null,
            )
        }
        persistState()
    }

    fun updateSetting(phase: Phase, value: Int) {
        val current = _uiState.value
        if (current.isRunning) return
        val bounds = SettingBounds.getValue(phase)
        val nextValue = value.coerceIn(bounds.min, bounds.max)
        if (nextValue == current.settings.minutesFor(phase)) return

        val nextSettings = when (phase) {
            Phase.Focus -> current.settings.copy(focus = nextValue)
            Phase.ShortBreak -> current.settings.copy(short = nextValue)
            Phase.LongBreak -> current.settings.copy(long = nextValue)
        }

        val phaseTotalMs = current.sessionTotalMillisByPhase[phase] ?: millisFor(phase, current.settings)
        val phaseRemainingMs = (current.remainingMillisByPhase[phase] ?: phaseTotalMs)
            .coerceIn(0L, phaseTotalMs)
        val isPhasePausedMidSession = phaseRemainingMs in 1L until phaseTotalMs

        // If the timer for this phase is paused mid-session, do NOT update the timer in real time;
        // it will only adopt the new duration when Reset is pressed (or when the session finishes).
        if (isPhasePausedMidSession) {
            _uiState.update { it.copy(settings = nextSettings) }
        } else {
            val newTotalMillis = millisFor(phase, nextSettings)
            val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
                this[phase] = newTotalMillis
            }
            val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
                this[phase] = newTotalMillis
            }
            _uiState.update {
                it.copy(
                    settings = nextSettings,
                    sessionTotalMillisByPhase = updatedTotals,
                    remainingMillisByPhase = updatedRemaining,
                )
            }
        }
        persistState()
    }

    fun dismissCompletion() {
        _uiState.update { it.copy(completion = null) }
        persistState()
    }

    fun startNext() {
        _uiState.update { it.copy(completion = null) }
        start()
    }

    private fun start() {
        val current = _uiState.value
        if (current.isRunning) return
        val currentRemainingMs = current.remainingMillis
        if (currentRemainingMs <= 0L) return

        val now = SystemClock.elapsedRealtime()
        deadlineElapsed = now + currentRemainingMs
        _uiState.update { it.copy(isRunning = true, completion = null) }
        persistState()
        startTicker()
    }

    private fun pause() {
        val now = SystemClock.elapsedRealtime()
        val current = _uiState.value
        val deadline = deadlineElapsed
        if (deadline != null) {
            val nextRemainingMs = (deadline - now).coerceAtLeast(0L)
            deadlineElapsed = null
            if (nextRemainingMs <= 0L) {
                finishPhase()
                return
            }
            val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
                this[current.phase] = nextRemainingMs
            }
            stopTicker()
            _uiState.update {
                it.copy(
                    remainingMillisByPhase = updatedRemaining,
                    isRunning = false,
                )
            }
            persistState()
            return
        }
        stopTicker()
        _uiState.update { it.copy(isRunning = false) }
        persistState()
    }

    private fun finishPhase() {
        val current = _uiState.value

        val nextCompletedFocus = if (current.phase == Phase.Focus) {
            current.completedFocus + 1
        } else {
            current.completedFocus
        }

        val addedFocusMinutes = if (current.phase == Phase.Focus) {
            val sessionMinutes = (current.totalMillis / 60_000L).toInt()
            sessionMinutes.coerceAtLeast(1)
        } else {
            0
        }

        val nextFocusedMinutes = current.focusedMinutes + addedFocusMinutes

        val nextPhase = if (current.phase == Phase.Focus) {
            if (nextCompletedFocus % SessionsPerCycle == 0) Phase.LongBreak else Phase.ShortBreak
        } else {
            Phase.Focus
        }

        // Reset both the finished phase and the incoming next phase to their configured durations
        val finishedPhaseNewDuration = millisFor(current.phase, current.settings)
        val nextPhaseNewDuration = millisFor(nextPhase, current.settings)

        val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
            this[current.phase] = finishedPhaseNewDuration
            this[nextPhase] = nextPhaseNewDuration
        }
        val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
            this[current.phase] = finishedPhaseNewDuration
            this[nextPhase] = nextPhaseNewDuration
        }

        deadlineElapsed = null
        stopTicker()
        _uiState.update {
            it.copy(
                phase = nextPhase,
                sessionTotalMillisByPhase = updatedTotals,
                remainingMillisByPhase = updatedRemaining,
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
        persistState()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(250)
                val now = SystemClock.elapsedRealtime()
                val deadline = deadlineElapsed ?: break
                val nextRemainingMs = (deadline - now).coerceAtLeast(0L)
                if (nextRemainingMs <= 0L) {
                    finishPhase()
                    break
                }
                updateActiveRemainingMillis(nextRemainingMs, forcePersist = false)
            }
        }
    }

    private fun updateActiveRemainingMillis(remainingMs: Long, forcePersist: Boolean) {
        val previousSeconds = _uiState.value.remaining
        _uiState.update { state ->
            val currentMs = state.remainingMillisByPhase[state.phase]
            if (currentMs == remainingMs) {
                state
            } else {
                val updatedMap = state.remainingMillisByPhase.toMutableMap().apply {
                    this[state.phase] = remainingMs
                }
                state.copy(remainingMillisByPhase = updatedMap)
            }
        }
        val currentSeconds = _uiState.value.remaining
        if (forcePersist || currentSeconds != previousSeconds) {
            savedStateHandle[remainingKeyFor(_uiState.value.phase)] = remainingMs
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun restoreInitialState(): PomodoroUiState {
        val focusMin = savedStateHandle.get<Int>(KEY_SETTING_FOCUS) ?: DefaultSettings.focus
        val shortMin = savedStateHandle.get<Int>(KEY_SETTING_SHORT) ?: DefaultSettings.short
        val longMin = savedStateHandle.get<Int>(KEY_SETTING_LONG) ?: DefaultSettings.long
        val settings = SessionSettings(focus = focusMin, short = shortMin, long = longMin)

        val phaseName = savedStateHandle.get<String>(KEY_PHASE)
        val phase = phaseName?.let { runCatching { Phase.valueOf(it) }.getOrNull() } ?: Phase.Focus

        val totalsMap = Phase.entries.associateWith { p ->
            savedStateHandle.get<Long>(totalKeyFor(p)) ?: millisFor(p, settings)
        }
        val remainingMap = Phase.entries.associateWith { p ->
            val totalForPhase = totalsMap.getValue(p)
            (savedStateHandle.get<Long>(remainingKeyFor(p)) ?: totalForPhase)
                .coerceIn(0L, totalForPhase)
        }

        val isRunning = savedStateHandle.get<Boolean>(KEY_IS_RUNNING) ?: false
        val completedFocus = savedStateHandle.get<Int>(KEY_COMPLETED_FOCUS) ?: 0
        val focusedMinutes = savedStateHandle.get<Int>(KEY_FOCUSED_MINUTES) ?: 0

        return PomodoroUiState(
            settings = settings,
            phase = phase,
            sessionTotalMillisByPhase = totalsMap,
            remainingMillisByPhase = remainingMap,
            isRunning = isRunning,
            completedFocus = completedFocus,
            focusedMinutes = focusedMinutes,
        )
    }

    private fun persistState() {
        val state = _uiState.value
        savedStateHandle[KEY_SETTING_FOCUS] = state.settings.focus
        savedStateHandle[KEY_SETTING_SHORT] = state.settings.short
        savedStateHandle[KEY_SETTING_LONG] = state.settings.long
        savedStateHandle[KEY_PHASE] = state.phase.name
        Phase.entries.forEach { p ->
            val totalMs = state.sessionTotalMillisByPhase[p] ?: millisFor(p, state.settings)
            savedStateHandle[totalKeyFor(p)] = totalMs
            savedStateHandle[remainingKeyFor(p)] =
                (state.remainingMillisByPhase[p] ?: totalMs).coerceIn(0L, totalMs)
        }
        savedStateHandle[KEY_IS_RUNNING] = state.isRunning
        savedStateHandle[KEY_COMPLETED_FOCUS] = state.completedFocus
        savedStateHandle[KEY_FOCUSED_MINUTES] = state.focusedMinutes
        savedStateHandle[KEY_DEADLINE] = deadlineElapsed
    }

    private fun totalKeyFor(phase: Phase): String = "total_ms_${phase.name}"
    private fun remainingKeyFor(phase: Phase): String = "remaining_ms_${phase.name}"

    companion object {
        private const val KEY_SETTING_FOCUS = "setting_focus"
        private const val KEY_SETTING_SHORT = "setting_short"
        private const val KEY_SETTING_LONG = "setting_long"
        private const val KEY_PHASE = "current_phase"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_COMPLETED_FOCUS = "completed_focus"
        private const val KEY_FOCUSED_MINUTES = "focused_minutes"
        private const val KEY_DEADLINE = "deadline_elapsed"
    }
}
