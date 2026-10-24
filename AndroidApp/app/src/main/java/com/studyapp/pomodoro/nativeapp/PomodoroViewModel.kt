package com.studyapp.pomodoro.nativeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = PomodoroTimerEngine.getInstance(application)

    val uiState: StateFlow<PomodoroUiState> = engine.state

    fun toggle() = engine.toggle()

    fun reset() = engine.reset()

    fun selectPhase(phase: Phase) = engine.selectPhase(phase)

    fun updateSetting(phase: Phase, value: Int) = engine.updateSetting(phase, value)

    fun resetDailySessions() = engine.resetDailySessions()

    fun updateAlarmEnabled(enabled: Boolean) = engine.updateAlarmEnabled(enabled)

    fun updateAlarmSound(sound: String) = engine.updateAlarmSound(sound)

    fun updateAlarmVibration(enabled: Boolean) = engine.updateAlarmVibration(enabled)

    fun updateAlarmDuration(seconds: Int) = engine.updateAlarmDuration(seconds)

    fun previewAlarm() = engine.previewAlarm()

    fun stopAlarm() = engine.stopAlarm()

    fun dismissCompletion() = engine.dismissCompletion()

    fun startNext() = engine.startNext()
}
