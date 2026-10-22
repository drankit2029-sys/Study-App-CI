package com.studyapp.decaycharge.domain

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface AppClock {
    fun nowEpochMs(): Long
    fun elapsedRealtimeMs(): Long
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}
