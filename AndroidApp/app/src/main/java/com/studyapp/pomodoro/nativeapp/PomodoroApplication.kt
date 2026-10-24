package com.studyapp.pomodoro.nativeapp

import android.app.Application

class PomodoroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PomodoroNotifications.createChannels(this)
        PomodoroTimerEngine.initialize(this)
    }
}
