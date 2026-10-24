package com.studyapp.pomodoro.nativeapp

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
