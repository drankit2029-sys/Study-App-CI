package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.model.CryoFreezeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ShellViewModel @Inject constructor(
    repository: TriageRepository,
) : ViewModel() {
    val activeFreeze: StateFlow<CryoFreezeStatus?> = repository.content
        .map { it.activeFreeze }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
