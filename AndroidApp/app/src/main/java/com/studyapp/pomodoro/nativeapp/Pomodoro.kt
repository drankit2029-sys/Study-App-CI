package com.studyapp.pomodoro.nativeapp

enum class Phase {
    Focus,
    ShortBreak,
    LongBreak,
}

data class SessionSettings(
    val focus: Int = 25,
    val short: Int = 5,
    val long: Int = 15,
) {
    fun minutesFor(phase: Phase): Int = when (phase) {
        Phase.Focus -> focus
        Phase.ShortBreak -> short
        Phase.LongBreak -> long
    }
}

data class SettingBound(
    val min: Int,
    val max: Int,
    val step: Int,
)

data class CompletionInfo(
    val finished: Phase,
    val next: Phase,
    val completedFocus: Int,
)

const val SessionsPerCycle = 4

val DefaultSettings = SessionSettings()

val SettingBounds = mapOf(
    Phase.Focus to SettingBound(5, 60, 5),
    Phase.ShortBreak to SettingBound(5, 30, 5),
    Phase.LongBreak to SettingBound(10, 60, 5),
)

fun secondsFor(phase: Phase, settings: SessionSettings): Int = settings.minutesFor(phase) * 60

fun millisFor(phase: Phase, settings: SessionSettings): Long = secondsFor(phase, settings) * 1000L

fun defaultDurationMillisMap(settings: SessionSettings = DefaultSettings): Map<Phase, Long> =
    Phase.entries.associateWith { phase -> millisFor(phase, settings) }

fun formatClock(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

val Phase.heading: String
    get() = when (this) {
        Phase.Focus -> "Focus session"
        Phase.ShortBreak -> "Short break"
        Phase.LongBreak -> "Long break"
    }

val Phase.shortLabel: String
    get() = when (this) {
        Phase.Focus -> "Focus"
        Phase.ShortBreak -> "Short"
        Phase.LongBreak -> "Long"
    }
