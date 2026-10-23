package com.studyapp.decaycharge.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.notification.NotificationHelper
import com.studyapp.decaycharge.widget.RedZoneGlanceWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TriageRepository,
    private val preferences: UserPreferencesRepository,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = preferences.preferences.first()
        if (!prefs.digestEnabled) return Result.success()
        val content = withTimeoutOrNull(5_000) {
            repository.content.first { it.subjects.isNotEmpty() }
        } ?: return Result.success()
        val red = content.chapters
            .filter { it.status == ChapterStatus.ACTIVE && it.zone == Zone.RED }
            .sortedBy { it.battery }
        if (red.isEmpty()) return Result.success()
        notifications.showDailyDigest(
            redCount = red.size,
            lowestTitle = red.first().title,
            lowestBattery = red.first().battery,
            chapterId = red.first().id,
        )
        return Result.success()
    }
}

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { RedZoneGlanceWidget().updateAll(applicationContext) }
        return Result.success()
    }
}
