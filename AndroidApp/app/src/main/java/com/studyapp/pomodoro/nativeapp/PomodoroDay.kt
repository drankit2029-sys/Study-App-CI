package com.studyapp.pomodoro.nativeapp

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class DailyRhythmRecord(
    val dayEpoch: Long,
    val completedFocus: Int,
    val focusedMinutes: Int,
) {
    fun resetIfNeeded(todayEpoch: Long): DailyRhythmRecord =
        if (dayEpoch == todayEpoch) {
            this
        } else {
            copy(
                dayEpoch = todayEpoch,
                completedFocus = 0,
                focusedMinutes = 0,
            )
        }
}

fun currentEpochDay(): Long = LocalDate.now().toEpochDay()

fun nextMidnightDelayMillis(now: ZonedDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
}
