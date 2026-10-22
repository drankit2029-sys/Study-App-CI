package com.studyapp.decaycharge.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
}
