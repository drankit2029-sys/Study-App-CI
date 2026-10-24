package com.studyapp.pomodoro.nativeapp

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroDayTest {

    @Test
    fun countsResetWhenTheDayChanges() {
        val record = DailyRhythmRecord(
            dayEpoch = 10L,
            completedFocus = 3,
            focusedMinutes = 75,
        )

        val reset = record.resetIfNeeded(11L)

        assertEquals(11L, reset.dayEpoch)
        assertEquals(0, reset.completedFocus)
        assertEquals(0, reset.focusedMinutes)
    }

    @Test
    fun countsStayWhenTheDayIsTheSame() {
        val record = DailyRhythmRecord(
            dayEpoch = 10L,
            completedFocus = 2,
            focusedMinutes = 50,
        )

        assertEquals(record, record.resetIfNeeded(10L))
    }

    @Test
    fun aSessionFinishingAfterMidnightStartsFromZero() {
        val beforeMidnight = DailyRhythmRecord(
            dayEpoch = 10L,
            completedFocus = 3,
            focusedMinutes = 75,
        )

        val reset = beforeMidnight.resetIfNeeded(11L)
        val afterFinish = reset.copy(
            completedFocus = reset.completedFocus + 1,
            focusedMinutes = reset.focusedMinutes + 25,
        )

        assertEquals(1, afterFinish.completedFocus)
        assertEquals(25, afterFinish.focusedMinutes)
    }

    @Test
    fun midnightDelayTargetsTheNextLocalMidnight() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 10, 24, 23, 59, 30, 0, zone)

        assertEquals(30_000L, nextMidnightDelayMillis(now))
    }

    @Test
    fun midnightDelayHandlesTheStartOfTheDay() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 10, 24, 0, 0, 0, 0, zone)

        assertEquals(86_400_000L, nextMidnightDelayMillis(now))
    }
}
