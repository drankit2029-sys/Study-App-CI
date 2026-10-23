package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    repository: TriageRepository,
) : ViewModel() {
    val needsOnboarding: StateFlow<Boolean?> = flow {
        runCatching { repository.purgeLegacyDemoData() }
        emitAll(preferences.preferences.map { !it.onboardingCompleted })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}
