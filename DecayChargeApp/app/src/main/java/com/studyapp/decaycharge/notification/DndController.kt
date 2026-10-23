package com.studyapp.decaycharge.notification

import android.app.NotificationManager
import android.content.Context
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DndController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean = manager.isNotificationPolicyAccessGranted

    suspend fun suppress() {
        val prefs = preferences.preferences.first()
        if (!prefs.dndEnabled || !hasAccess()) return
        if (prefs.dndSuppressed) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            return
        }
        preferences.setDndPreviousFilter(manager.currentInterruptionFilter)
        preferences.setDndSuppressed(true)
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    suspend fun restore() {
        val prefs = preferences.preferences.first()
        if (!prefs.dndSuppressed) return
        val previous = prefs.dndPreviousFilter
            ?.takeIf { it in NotificationManager.INTERRUPTION_FILTER_ALL..NotificationManager.INTERRUPTION_FILTER_ALARMS }
            ?: NotificationManager.INTERRUPTION_FILTER_ALL
        if (hasAccess()) manager.setInterruptionFilter(previous)
        preferences.setDndSuppressed(false)
        preferences.setDndPreviousFilter(null)
    }

    suspend fun reapplyIfSuppressed() {
        if (!preferences.preferences.first().dndSuppressed) return
        if (hasAccess()) {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }
}
