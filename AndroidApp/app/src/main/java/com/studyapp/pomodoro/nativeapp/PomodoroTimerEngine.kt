package com.studyapp.pomodoro.nativeapp

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.studyapp.pomodoro.nativeapp.service.PomodoroTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroTimerEngine private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var deadlineElapsed: Long? = null
    private var ticker: Job? = null

    private val _state: MutableStateFlow<PomodoroUiState>
    val state: StateFlow<PomodoroUiState>

    init {
        val restored = restoreState()
        _state = MutableStateFlow(restored)
        state = _state.asStateFlow()

        if (restored.isRunning && restored.remainingMillis <= 0L) {
            finishPhase()
        } else {
            persistState()
            if (restored.isRunning) {
                startTicker()
            }
        }
    }

    fun toggle() {
        if (_state.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    fun reset() {
        val current = _state.value
        deadlineElapsed = null
        stopTicker()

        val newDurationMillis = millisFor(current.phase, current.settings)
        val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
            this[current.phase] = newDurationMillis
        }
        val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
            this[current.phase] = newDurationMillis
        }

        _state.update {
            it.copy(
                isRunning = false,
                sessionTotalMillisByPhase = updatedTotals,
                remainingMillisByPhase = updatedRemaining,
                completion = null,
            )
        }
        PomodoroNotifications.cancelCompletion(appContext)
        persistState()
        PomodoroTimerService.stop(appContext)
    }

    fun selectPhase(phase: Phase) {
        val current = _state.value
        if (phase == current.phase || current.isRunning) return

        deadlineElapsed = null
        stopTicker()
        _state.update {
            it.copy(
                phase = phase,
                isRunning = false,
                completion = null,
            )
        }
        PomodoroNotifications.cancelCompletion(appContext)
        persistState()
        PomodoroTimerService.stop(appContext)
    }

    fun updateSetting(phase: Phase, value: Int) {
        val current = _state.value
        if (current.isRunning) return

        val bounds = SettingBounds.getValue(phase)
        val nextValue = value.coerceIn(bounds.min, bounds.max)
        if (nextValue == current.settings.minutesFor(phase)) return

        val nextSettings = when (phase) {
            Phase.Focus -> current.settings.copy(focus = nextValue)
            Phase.ShortBreak -> current.settings.copy(short = nextValue)
            Phase.LongBreak -> current.settings.copy(long = nextValue)
        }

        val phaseTotalMillis =
            current.sessionTotalMillisByPhase[phase] ?: millisFor(phase, current.settings)
        val phaseRemainingMillis = (current.remainingMillisByPhase[phase] ?: phaseTotalMillis)
            .coerceIn(0L, phaseTotalMillis)
        val isPhasePausedMidSession = phaseRemainingMillis in 1L until phaseTotalMillis

        if (isPhasePausedMidSession) {
            _state.update { it.copy(settings = nextSettings) }
        } else {
            val newTotalMillis = millisFor(phase, nextSettings)
            val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
                this[phase] = newTotalMillis
            }
            val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
                this[phase] = newTotalMillis
            }
            _state.update {
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
        PomodoroNotifications.cancelCompletion(appContext)
        _state.update { it.copy(completion = null) }
        persistState()
    }

    fun startNext(shouldStartService: Boolean = true) {
        PomodoroNotifications.cancelCompletion(appContext)
        _state.update { it.copy(completion = null) }
        start(shouldStartService)
    }

    fun ensureServiceIfRunning() {
        if (_state.value.isRunning) {
            startService()
        } else {
            PomodoroTimerService.stop(appContext)
        }
    }

    fun ensureTicker() {
        if (!_state.value.isRunning) return
        if (deadlineElapsed == null) {
            deadlineElapsed = SystemClock.elapsedRealtime() + _state.value.remainingMillis
            persistState()
        }
        if (ticker?.isActive != true) {
            startTicker()
        }
    }

    private fun start(shouldStartService: Boolean = true) {
        val current = _state.value
        if (current.isRunning) return

        val remainingMillis = current.remainingMillis
        if (remainingMillis <= 0L) return

        deadlineElapsed = SystemClock.elapsedRealtime() + remainingMillis
        _state.update { it.copy(isRunning = true, completion = null) }
        PomodoroNotifications.cancelCompletion(appContext)
        persistState()
        startTicker()
        if (shouldStartService) {
            startService()
        }
    }

    private fun pause() {
        val current = _state.value
        val deadline = deadlineElapsed

        if (deadline != null) {
            val remainingMillis = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            if (remainingMillis <= 0L) {
                finishPhase()
                return
            }

            deadlineElapsed = null
            stopTicker()
            updateRemaining(remainingMillis)
            _state.update { it.copy(isRunning = false) }
            persistState()
            PomodoroTimerService.stop(appContext)
            return
        }

        stopTicker()
        _state.update { it.copy(isRunning = false) }
        persistState()
        PomodoroTimerService.stop(appContext)
    }

    private fun finishPhase() {
        val current = _state.value

        val nextCompletedFocus = if (current.phase == Phase.Focus) {
            current.completedFocus + 1
        } else {
            current.completedFocus
        }

        val addedFocusMinutes = if (current.phase == Phase.Focus) {
            (current.totalMillis / 60_000L).toInt().coerceAtLeast(1)
        } else {
            0
        }

        val nextFocusedMinutes = current.focusedMinutes + addedFocusMinutes

        val nextPhase = if (current.phase == Phase.Focus) {
            if (nextCompletedFocus % SessionsPerCycle == 0) Phase.LongBreak else Phase.ShortBreak
        } else {
            Phase.Focus
        }

        val finishedPhaseDuration = millisFor(current.phase, current.settings)
        val nextPhaseDuration = millisFor(nextPhase, current.settings)

        val updatedTotals = current.sessionTotalMillisByPhase.toMutableMap().apply {
            this[current.phase] = finishedPhaseDuration
            this[nextPhase] = nextPhaseDuration
        }
        val updatedRemaining = current.remainingMillisByPhase.toMutableMap().apply {
            this[current.phase] = finishedPhaseDuration
            this[nextPhase] = nextPhaseDuration
        }

        deadlineElapsed = null
        stopTicker()

        val finishedState = current.copy(
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
        _state.value = finishedState
        persistState()
        PomodoroNotifications.showCompletion(appContext, finishedState)
        PomodoroTimerService.stop(appContext)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(250L)
                val deadline = deadlineElapsed ?: break
                val remainingMillis =
                    (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remainingMillis <= 0L) {
                    finishPhase()
                    break
                }
                updateRemaining(remainingMillis)
            }
        }
    }

    private fun updateRemaining(remainingMillis: Long) {
        _state.update { state ->
            val currentMillis = state.remainingMillisByPhase[state.phase]
            if (currentMillis == remainingMillis) {
                state
            } else {
                state.copy(
                    remainingMillisByPhase = state.remainingMillisByPhase.toMutableMap().apply {
                        this[state.phase] = remainingMillis
                    },
                )
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun restoreState(): PomodoroUiState {
        val settings = SessionSettings(
            focus = preferences.getInt(KEY_SETTING_FOCUS, DefaultSettings.focus),
            short = preferences.getInt(KEY_SETTING_SHORT, DefaultSettings.short),
            long = preferences.getInt(KEY_SETTING_LONG, DefaultSettings.long),
        )

        val phase = preferences.getString(KEY_PHASE, null)
            ?.let { runCatching { Phase.valueOf(it) }.getOrNull() }
            ?: Phase.Focus

        val totals = Phase.entries.associateWith { savedPhase ->
            preferences.getLong(totalKeyFor(savedPhase), millisFor(savedPhase, settings))
                .coerceAtLeast(1L)
        }
        val remaining = Phase.entries.associateWith { savedPhase ->
            val total = totals.getValue(savedPhase)
            preferences.getLong(remainingKeyFor(savedPhase), total).coerceIn(0L, total)
        }.toMutableMap()

        val savedRunning = preferences.getBoolean(KEY_IS_RUNNING, false)
        val completedFocus = preferences.getInt(KEY_COMPLETED_FOCUS, 0)
        val focusedMinutes = preferences.getInt(KEY_FOCUSED_MINUTES, 0)

        var running = savedRunning
        var finishImmediately = false

        if (savedRunning) {
            val savedDeadline = preferences.getLong(KEY_DEADLINE, -1L).takeIf { it > 0L }
            val savedBootCount = preferences.getInt(KEY_BOOT_COUNT, -1)
            val bootCountNow = currentBootCount()
            val rebooted = savedBootCount >= 0 &&
                bootCountNow >= 0 &&
                savedBootCount != bootCountNow

            if (savedDeadline == null || rebooted) {
                running = false
            } else {
                val remainingMillis = savedDeadline - SystemClock.elapsedRealtime()
                val totalMillis = totals.getValue(phase)
                when {
                    remainingMillis <= 0L -> {
                        remaining[phase] = 0L
                        finishImmediately = true
                    }

                    remainingMillis <= totalMillis -> {
                        remaining[phase] = remainingMillis
                        deadlineElapsed = savedDeadline
                    }

                    else -> {
                        running = false
                    }
                }
            }
        }

        if (!running && !finishImmediately) {
            deadlineElapsed = null
        }

        return PomodoroUiState(
            settings = settings,
            phase = phase,
            sessionTotalMillisByPhase = totals,
            remainingMillisByPhase = remaining,
            isRunning = running || finishImmediately,
            completedFocus = completedFocus,
            focusedMinutes = focusedMinutes,
        )
    }

    private fun persistState() {
        val current = _state.value
        val editor = preferences.edit()

        editor.putInt(KEY_SETTING_FOCUS, current.settings.focus)
        editor.putInt(KEY_SETTING_SHORT, current.settings.short)
        editor.putInt(KEY_SETTING_LONG, current.settings.long)
        editor.putString(KEY_PHASE, current.phase.name)

        Phase.entries.forEach { savedPhase ->
            val totalMillis = current.sessionTotalMillisByPhase[savedPhase]
                ?: millisFor(savedPhase, current.settings)
            editor.putLong(totalKeyFor(savedPhase), totalMillis)
            editor.putLong(
                remainingKeyFor(savedPhase),
                (current.remainingMillisByPhase[savedPhase] ?: totalMillis)
                    .coerceIn(0L, totalMillis),
            )
        }

        editor.putBoolean(KEY_IS_RUNNING, current.isRunning)
        editor.putInt(KEY_COMPLETED_FOCUS, current.completedFocus)
        editor.putInt(KEY_FOCUSED_MINUTES, current.focusedMinutes)
        editor.putLong(KEY_DEADLINE, deadlineElapsed ?: -1L)
        editor.putInt(KEY_BOOT_COUNT, currentBootCount())
        editor.apply()
    }

    private fun currentBootCount(): Int = runCatching {
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrDefault(-1)

    private fun startService() {
        runCatching {
            PomodoroTimerService.start(appContext)
        }.onFailure { error ->
            Log.w(LOG_TAG, "Unable to start the foreground timer service", error)
        }
    }

    private fun totalKeyFor(phase: Phase): String = "total_ms_${phase.name}"

    private fun remainingKeyFor(phase: Phase): String = "remaining_ms_${phase.name}"

    companion object {
        private const val LOG_TAG = "PomodoroTimerEngine"
        private const val PREFERENCES_NAME = "pomodoro_timer_state"
        private const val KEY_SETTING_FOCUS = "setting_focus"
        private const val KEY_SETTING_SHORT = "setting_short"
        private const val KEY_SETTING_LONG = "setting_long"
        private const val KEY_PHASE = "current_phase"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_COMPLETED_FOCUS = "completed_focus"
        private const val KEY_FOCUSED_MINUTES = "focused_minutes"
        private const val KEY_DEADLINE = "deadline_elapsed"
        private const val KEY_BOOT_COUNT = "boot_count"

        @Volatile
        private var instance: PomodoroTimerEngine? = null

        fun initialize(context: Context): PomodoroTimerEngine =
            instance ?: synchronized(this) {
                instance ?: PomodoroTimerEngine(context).also { instance = it }
            }

        fun getInstance(context: Context): PomodoroTimerEngine = initialize(context)
    }
}
