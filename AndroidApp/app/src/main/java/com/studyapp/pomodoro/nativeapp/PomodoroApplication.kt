package com.studyapp.pomodoro.nativeapp

import android.app.Application
import com.google.android.gms.ads.MobileAds

class PomodoroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        PomodoroNotifications.createChannels(this)
        PomodoroTimerEngine.initialize(this)
    }
}
