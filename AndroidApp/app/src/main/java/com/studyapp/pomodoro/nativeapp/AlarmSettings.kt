package com.studyapp.pomodoro.nativeapp

const val DEFAULT_ALARM_SOUND = "default"
const val SILENT_ALARM_SOUND = "silent"

val AlarmDurationOptions = listOf(10, 20, 30, 60)

data class AlarmSettings(
    val enabled: Boolean = true,
    val sound: String = DEFAULT_ALARM_SOUND,
    val vibration: Boolean = true,
    val durationSeconds: Int = 20,
) {
    val safeDurationSeconds: Int
        get() = durationSeconds.coerceIn(5, 120)
}
