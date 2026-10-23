package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.domain.DailyPlanner
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.domain.PlannedCharge
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.CryoFreezeStatus
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
import kotlinx.coroutines.launch

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
    val dailyBudgetMinutes: Int = 120,
    val plan: List<PlannedCharge> = emptyList(),
    val activeFreeze: CryoFreezeStatus? = null,
    val examDateEpochMs: Long? = null,
    val daysUntilExam: Int? = null,
) {
    val nextCharge: Chapter?
        get() = chapters.filter { it.zone == Zone.RED }.maxByOrNull { it.urgencyScore }
            ?: chapters.filter { it.zone == Zone.AMBER }.maxByOrNull { it.urgencyScore }
}

@HiltViewModel
class TriageViewModel @Inject constructor(
    private val repository: TriageRepository,
    private val preferences: UserPreferencesRepository,
    private val clock: com.studyapp.decaycharge.domain.AppClock,
) : ViewModel() {
    private val filter = MutableStateFlow(ZoneFilter.ALL)

    val uiState: StateFlow<TriageUiState> = combine(
        repository.content,
        filter,
        preferences.preferences,
    ) { content, selectedFilter, prefs ->
        val subjects = content.subjects
        val chapters = content.chapters.filter { it.status != ChapterStatus.ARCHIVED }
        TriageUiState(
            readiness = chapters.weightedReadiness(subjects),
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
            dailyBudgetMinutes = prefs.dailyBudgetMinutes,
            plan = DailyPlanner.build(chapters, prefs.dailyBudgetMinutes),
            activeFreeze = content.activeFreeze,
            examDateEpochMs = prefs.examDateEpochMs,
            daysUntilExam = prefs.examDateEpochMs?.let {
                ((it - clock.nowEpochMs()).toDouble() / DecayCalculator.MILLIS_PER_DAY)
                    .toInt()
                    .coerceAtLeast(0)
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TriageUiState(),
    )

    fun setFilter(value: ZoneFilter) {
        filter.value = value
    }

    fun setDailyBudget(minutes: Int) {
        viewModelScope.launch { preferences.setDailyBudgetMinutes(minutes) }
    }

    fun startFreeze(hours: Int?, note: String? = null) {
        viewModelScope.launch { repository.startCryoFreeze(hours, note) }
    }

    fun unlockChapter(chapterId: Long) {
        viewModelScope.launch {
            repository.updateChapterStatus(setOf(chapterId), ChapterStatus.ACTIVE)
        }
    }

    fun extendFreeze(hours: Int) {
        viewModelScope.launch { repository.extendCryoFreeze(hours) }
    }

    fun endFreeze() {
        viewModelScope.launch { repository.endCryoFreeze() }
    }
}
