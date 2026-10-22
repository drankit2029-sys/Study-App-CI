package com.studyapp.decaycharge.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.StudyRepository
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.domain.ActiveChargeState
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChargeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChargerUiState(
    val chapter: Chapter? = null,
    val mode: ChargeMode = ChargeMode.DEEP,
    val note: String = "",
    val active: ActiveChargeState? = null,
    val showBackSheet: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val selectedRating: Int? = null,
) {
    val isForThisChapter: Boolean
        get() = active != null && chapter != null && active.chapterId == chapter.id

    val displayBattery: Float
        get() = if (isForThisChapter) active?.projectedBattery ?: chapter?.battery ?: 0f
        else chapter?.battery ?: 0f

    val isRunning: Boolean
        get() = isForThisChapter && active?.isRunning == true

    val isComplete: Boolean
        get() = isForThisChapter && active?.isComplete == true
}

private data class ChargerControls(
    val mode: ChargeMode = ChargeMode.DEEP,
    val note: String = "",
    val showBackSheet: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val selectedRating: Int? = null,
)

@HiltViewModel
class ChargerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: StudyRepository,
    private val controller: ActiveChargeController,
) : ViewModel() {
    private val chapterId: Long = checkNotNull(savedStateHandle["chapterId"])
    private val chapter: Chapter? = repository.chapter(chapterId)
    private val controls = MutableStateFlow(
        ChargerControls(note = chapter?.note.orEmpty()),
    )
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val snackbarMessages: SharedFlow<String> = messages

    val uiState: StateFlow<ChargerUiState> = combine(
        controls,
        controller.state,
    ) { state, active ->
        ChargerUiState(
            chapter = chapter,
            mode = if (active?.chapterId == chapterId) active.mode else state.mode,
            note = state.note,
            active = active,
            showBackSheet = state.showBackSheet,
            showDiscardDialog = state.showDiscardDialog,
            selectedRating = state.selectedRating,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChargerUiState(
            chapter = chapter,
            note = chapter?.note.orEmpty(),
        ),
    )

    fun setMode(mode: ChargeMode) {
        if (uiState.value.isForThisChapter) return
        controls.value = controls.value.copy(mode = mode)
    }

    fun setNote(note: String) {
        controls.value = controls.value.copy(note = note)
    }

    fun toggleTimer() {
        val current = uiState.value
        val selectedChapter = current.chapter ?: return
        val active = current.active
        if (active == null || active.chapterId != selectedChapter.id || active.isComplete) {
            controls.value = controls.value.copy(
                showBackSheet = false,
                showDiscardDialog = false,
                selectedRating = null,
            )
            controller.start(selectedChapter, controls.value.mode)
        } else {
            controller.togglePause()
        }
    }

    fun requestBack(): Boolean {
        val current = uiState.value
        if (current.isComplete) {
            messages.tryEmit(
                if (current.selectedRating == null) {
                    "Select Hard, Medium, or Easy to save your battery charge."
                } else {
                    "Save this charge to return to your syllabus."
                },
            )
            return false
        }
        if (current.isRunning) {
            controls.value = controls.value.copy(showBackSheet = true)
            return false
        }
        return true
    }

    fun keepChargingInBackground() {
        controls.value = controls.value.copy(showBackSheet = false)
    }

    fun showDiscardConfirmation() {
        controls.value = controls.value.copy(
            showBackSheet = false,
            showDiscardDialog = true,
        )
    }

    fun dismissDiscardConfirmation() {
        controls.value = controls.value.copy(showDiscardDialog = false)
    }

    fun discardSession() {
        controller.clear()
        controls.value = controls.value.copy(
            showDiscardDialog = false,
            showBackSheet = false,
            selectedRating = null,
        )
    }

    fun endEarly() {
        val active = uiState.value.active ?: return
        if (active.elapsedSeconds < 5 * 60) {
            messages.tryEmit("Five minutes of charge are needed before ending early.")
            return
        }
        controller.finishEarly()
        controls.value = controls.value.copy(showBackSheet = false)
    }

    fun dismissBackSheet() {
        controls.value = controls.value.copy(showBackSheet = false)
    }

    fun selectRating(rating: Int) {
        controls.value = controls.value.copy(selectedRating = rating)
    }

    fun saveCompletedCharge(): Boolean {
        if (uiState.value.selectedRating == null) {
            messages.tryEmit("Choose a recall rating before saving.")
            return false
        }
        controller.clear()
        controls.value = controls.value.copy(selectedRating = null)
        return true
    }
}
