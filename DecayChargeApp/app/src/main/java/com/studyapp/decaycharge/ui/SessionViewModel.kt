package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.domain.ActiveChargeState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val controller: ActiveChargeController,
) : ViewModel() {
    val activeSession: StateFlow<ActiveChargeState?> = controller.state

    fun togglePause() {
        controller.togglePause()
    }

    fun discard() {
        controller.clear()
    }
}
