package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Subject
import com.studyapp.decaycharge.model.StudySessionRecord
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.toSubjectSummaries
import com.studyapp.decaycharge.model.weightedReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val readiness: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val summaries: List<SubjectSummary> = emptyList(),
    val averageStabilityDays: Float = 0f,
    val totalReviewCount: Int = 0,
    val isLoading: Boolean = true,
    val sessions: List<StudySessionRecord> = emptyList(),
    val sessionFilter: Int? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: TriageRepository,
) : ViewModel() {
    private val sessionFilter = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.content,
        repository.sessions,
        sessionFilter,
    ) { content, sessions, ratingFilter ->
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
            sessions = sessions.filter { ratingFilter == null || it.recallRating == ratingFilter },
            sessionFilter = ratingFilter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState(),
    )

    fun setSessionFilter(rating: Int?) {
        sessionFilter.value = rating
    }
}
