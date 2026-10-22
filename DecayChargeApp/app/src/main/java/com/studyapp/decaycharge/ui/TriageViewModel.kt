package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.StudyRepository
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Subject
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.model.ZoneFilter
import com.studyapp.decaycharge.model.toSubjectSummaries
import com.studyapp.decaycharge.model.weightedReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TriageUiState(
    val readiness: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val summaries: List<SubjectSummary> = emptyList(),
    val filter: ZoneFilter = ZoneFilter.ALL,
    val redCount: Int = 0,
    val amberCount: Int = 0,
    val greenCount: Int = 0,
    val lockedCount: Int = 0,
    val activeCount: Int = 0,
    val isLoading: Boolean = true,
) {
    val nextCharge: Chapter?
        get() = chapters.filter { it.zone == Zone.RED }.minByOrNull { it.battery }
            ?: chapters.filter { it.zone == Zone.AMBER }.minByOrNull { it.battery }
}

@HiltViewModel
class TriageViewModel @Inject constructor(
    repository: StudyRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(ZoneFilter.ALL)

    val uiState: StateFlow<TriageUiState> = combine(
        repository.subjects,
        repository.chapters,
        filter,
    ) { subjects, chapters, selectedFilter ->
        TriageUiState(
            readiness = chapters.weightedReadiness(),
            chapters = chapters,
            subjects = subjects,
            summaries = chapters.toSubjectSummaries(subjects),
            filter = selectedFilter,
            redCount = chapters.count { it.zone == Zone.RED },
            amberCount = chapters.count { it.zone == Zone.AMBER },
            greenCount = chapters.count { it.zone == Zone.GREEN },
            lockedCount = chapters.count { it.zone == Zone.LOCKED },
            activeCount = chapters.count { it.status == ChapterStatus.ACTIVE },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TriageUiState(),
    )

    fun setFilter(value: ZoneFilter) {
        filter.value = value
    }
}
