package com.studyapp.decaycharge.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.backup.BackupManager
import com.studyapp.decaycharge.data.prefs.UserPreferences
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.domain.AppClock
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.model.CryoFreezeStatus
import com.studyapp.decaycharge.work.BackgroundScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val activeFreeze: CryoFreezeStatus? = null,
    val daysUntilExam: Long? = null,
    val message: String? = null,
    val busy: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val repository: TriageRepository,
    private val backupManager: BackupManager,
    private val scheduler: BackgroundScheduler,
    private val clock: AppClock,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.preferences,
        repository.content,
        message,
        busy,
    ) { prefs, content, currentMessage, isBusy ->
        SettingsUiState(
            preferences = prefs,
            activeFreeze = content.activeFreeze,
            daysUntilExam = prefs.examDateEpochMs?.let {
                val days = (it - clock.nowEpochMs()).toDouble() / DecayCalculator.MILLIS_PER_DAY
                days.coerceAtLeast(0.0).toLong()
            },
            message = currentMessage,
            busy = isBusy,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setExamDate(epochMs: Long?) {
        viewModelScope.launch { preferences.setExamDate(epochMs) }
    }

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setDndEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepScreenOn(enabled) }
    }

    fun setStrictFocus(enabled: Boolean) {
        viewModelScope.launch { preferences.setStrictFocusEnabled(enabled) }
    }

    fun setFullScreenAlerts(enabled: Boolean) {
        viewModelScope.launch { preferences.setFullScreenAlertsEnabled(enabled) }
    }

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDigestEnabled(enabled)
            if (enabled) {
                val prefs = preferences.preferences.first()
                scheduler.scheduleDigest(prefs.digestHour, prefs.digestMinute)
            }
        }
    }

    fun setDigestTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setDigestTime(hour, minute)
            scheduler.scheduleDigest(hour, minute)
        }
    }

    fun endFreeze() {
        viewModelScope.launch { repository.endCryoFreeze() }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = backupManager.export(uri)
            message.value = result.fold(
                onSuccess = { "Backup written: $it chapters." },
                onFailure = { "Backup failed: ${it.message ?: "unknown error"}" },
            )
            busy.value = false
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = backupManager.restore(uri)
            message.value = result.fold(
                onSuccess = {
                    val restored = preferences.preferences.first()
                    scheduler.scheduleWidgetRefresh()
                    scheduler.scheduleDigest(restored.digestHour, restored.digestMinute)
                    "Restored $it chapters."
                },
                onFailure = { "Restore failed: ${it.message ?: "unknown error"}" },
            )
            busy.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
