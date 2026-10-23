package com.studyapp.decaycharge.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BackgroundScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    suspend fun scheduleAll() {
        val prefs = preferences.preferences.first()
        scheduleWidgetRefresh()
        scheduleDigest(prefs.digestHour, prefs.digestMinute, replace = false)
    }

    fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(3, TimeUnit.HOURS)
            .setInitialDelay(3, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleDigest(hour: Int? = null, minute: Int? = null, replace: Boolean = true) {
        val now = ZonedDateTime.now()
        val targetHour = hour ?: 8
        val targetMinute = minute ?: 0
        var next = now
            .withHour(targetHour.coerceIn(0, 23))
            .withMinute(targetMinute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DIGEST_WORK,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val WIDGET_WORK = "decaycharge_widget_refresh"
        const val DIGEST_WORK = "decaycharge_daily_digest"
    }
}
