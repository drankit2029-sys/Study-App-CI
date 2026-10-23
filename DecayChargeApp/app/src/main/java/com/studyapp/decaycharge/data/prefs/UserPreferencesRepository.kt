package com.studyapp.decaycharge.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.decayChargePreferences: DataStore<Preferences> by preferencesDataStore(
    name = "decaycharge_preferences",
)

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val activeTimerSessionJson: String? = null,
    val examDateEpochMs: Long? = null,
    val dndEnabled: Boolean = false,
    val cryoFreezeActive: Boolean = false,
    val dailyBudgetMinutes: Int = 120,
    val keepScreenOn: Boolean = true,
    val strictFocusEnabled: Boolean = false,
    val fullScreenAlertsEnabled: Boolean = false,
    val digestEnabled: Boolean = true,
    val digestHour: Int = 8,
    val digestMinute: Int = 0,
    val dndPreviousFilter: Int? = null,
    val dndSuppressed: Boolean = false,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val activeTimerSession = stringPreferencesKey("active_timer_session")
        val examDate = longPreferencesKey("exam_date_epoch_ms")
        val dndEnabled = booleanPreferencesKey("dnd_enabled")
        val cryoFreezeActive = booleanPreferencesKey("cryo_freeze_active")
        val dailyBudgetMinutes = intPreferencesKey("daily_budget_minutes")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val strictFocusEnabled = booleanPreferencesKey("strict_focus_enabled")
        val fullScreenAlertsEnabled = booleanPreferencesKey("full_screen_alerts_enabled")
        val digestEnabled = booleanPreferencesKey("digest_enabled")
        val digestHour = intPreferencesKey("digest_hour")
        val digestMinute = intPreferencesKey("digest_minute")
        val dndPreviousFilter = intPreferencesKey("dnd_previous_filter")
        val dndSuppressed = booleanPreferencesKey("dnd_suppressed")
    }

    val preferences: Flow<UserPreferences> = context.decayChargePreferences.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            UserPreferences(
                onboardingCompleted = values[Keys.onboardingCompleted] ?: false,
                activeTimerSessionJson = values[Keys.activeTimerSession],
                examDateEpochMs = values[Keys.examDate],
                dndEnabled = values[Keys.dndEnabled] ?: false,
                cryoFreezeActive = values[Keys.cryoFreezeActive] ?: false,
                dailyBudgetMinutes = values[Keys.dailyBudgetMinutes] ?: 120,
                keepScreenOn = values[Keys.keepScreenOn] ?: true,
                strictFocusEnabled = values[Keys.strictFocusEnabled] ?: false,
                fullScreenAlertsEnabled = values[Keys.fullScreenAlertsEnabled] ?: false,
                digestEnabled = values[Keys.digestEnabled] ?: true,
                digestHour = values[Keys.digestHour] ?: 8,
                digestMinute = values[Keys.digestMinute] ?: 0,
                dndPreviousFilter = values[Keys.dndPreviousFilter],
                dndSuppressed = values[Keys.dndSuppressed] ?: false,
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.decayChargePreferences.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setActiveTimerSession(sessionJson: String?) {
        context.decayChargePreferences.edit { values ->
            if (sessionJson == null) values.remove(Keys.activeTimerSession)
            else values[Keys.activeTimerSession] = sessionJson
        }
    }

    suspend fun setExamDate(epochMs: Long?) {
        context.decayChargePreferences.edit { values ->
            if (epochMs == null) values.remove(Keys.examDate)
            else values[Keys.examDate] = epochMs
        }
    }

    suspend fun setDndEnabled(enabled: Boolean) {
        context.decayChargePreferences.edit { it[Keys.dndEnabled] = enabled }
    }

    suspend fun setCryoFreezeActive(active: Boolean) {
        context.decayChargePreferences.edit { it[Keys.cryoFreezeActive] = active }
    }

    suspend fun setDailyBudgetMinutes(minutes: Int) {
        context.decayChargePreferences.edit { it[Keys.dailyBudgetMinutes] = minutes.coerceIn(30, 480) }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.decayChargePreferences.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setStrictFocusEnabled(enabled: Boolean) {
        context.decayChargePreferences.edit { it[Keys.strictFocusEnabled] = enabled }
    }

    suspend fun setFullScreenAlertsEnabled(enabled: Boolean) {
        context.decayChargePreferences.edit { it[Keys.fullScreenAlertsEnabled] = enabled }
    }

    suspend fun setDigestEnabled(enabled: Boolean) {
        context.decayChargePreferences.edit { it[Keys.digestEnabled] = enabled }
    }

    suspend fun setDigestTime(hour: Int, minute: Int) {
        context.decayChargePreferences.edit {
            it[Keys.digestHour] = hour.coerceIn(0, 23)
            it[Keys.digestMinute] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDndPreviousFilter(filter: Int?) {
        context.decayChargePreferences.edit { values ->
            if (filter == null) values.remove(Keys.dndPreviousFilter)
            else values[Keys.dndPreviousFilter] = filter
        }
    }

    suspend fun setDndSuppressed(suppressed: Boolean) {
        context.decayChargePreferences.edit { it[Keys.dndSuppressed] = suppressed }
    }

    suspend fun restorePreferences(
        onboardingCompleted: Boolean,
        examDateEpochMs: Long?,
        dndEnabled: Boolean,
        dailyBudgetMinutes: Int,
        keepScreenOn: Boolean,
        strictFocusEnabled: Boolean,
        fullScreenAlertsEnabled: Boolean,
        digestEnabled: Boolean,
        digestHour: Int,
        digestMinute: Int,
    ) {
        context.decayChargePreferences.edit { values ->
            values[Keys.onboardingCompleted] = onboardingCompleted
            if (examDateEpochMs == null) values.remove(Keys.examDate)
            else values[Keys.examDate] = examDateEpochMs
            values[Keys.dndEnabled] = dndEnabled
            values[Keys.dailyBudgetMinutes] = dailyBudgetMinutes.coerceIn(30, 480)
            values[Keys.keepScreenOn] = keepScreenOn
            values[Keys.strictFocusEnabled] = strictFocusEnabled
            values[Keys.fullScreenAlertsEnabled] = fullScreenAlertsEnabled
            values[Keys.digestEnabled] = digestEnabled
            values[Keys.digestHour] = digestHour.coerceIn(0, 23)
            values[Keys.digestMinute] = digestMinute.coerceIn(0, 59)
        }
    }
}
