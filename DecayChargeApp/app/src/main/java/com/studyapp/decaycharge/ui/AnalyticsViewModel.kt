package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Subject
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.toSubjectSummaries
import com.studyapp.decaycharge.model.weightedReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val readiness: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val summaries: List<SubjectSummary> = emptyList(),
    val averageStabilityDays: Float = 0f,
    val totalReviewCount: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: TriageRepository,
) : ViewModel() {
    val uiState: StateFlow<AnalyticsUiState> = repository.content
        .map { content ->
            val subjects = content.subjects
            val chapters = content.chapters.filter { it.status != ChapterStatus.ARCHIVED }
            val active = chapters.filter { it.status == ChapterStatus.ACTIVE }
            AnalyticsUiState(
                readiness = chapters.weightedReadiness(subjects),
                chapters = chapters,
                subjects = subjects,
                summaries = chapters.toSubjectSummaries(
                    subjects.filter { subject -> chapters.any { it.subjectId == subject.id } },
                ),
                averageStabilityDays = if (active.isEmpty()) 0f
                else active.map { it.stabilityDays }.average().toFloat(),
                totalReviewCount = chapters.sumOf { it.reviewCount },
                isLoading = false,
            )
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )
}
