package com.studyapp.decaycharge.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.StudyRepository
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.model.ZoneFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SyllabusUiState(
    val subjects: List<SubjectSummary> = emptyList(),
    val query: String = "",
    val filter: ZoneFilter = ZoneFilter.ALL,
    val selectedSubjectId: Long? = null,
    val expandedSubjectIds: Set<Long> = emptySet(),
    val totalSubjects: Int = 0,
    val totalChapters: Int = 0,
    val visibleChapters: Int = 0,
    val isLoading: Boolean = true,
) {
    val hasNarrowing: Boolean
        get() = query.isNotBlank() || filter != ZoneFilter.ALL

    val selectedSummary: SubjectSummary?
        get() = subjects.firstOrNull { it.subject.id == selectedSubjectId } ?: subjects.firstOrNull()
}

private data class SearchCriteria(
    val query: String,
    val filter: ZoneFilter,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SyllabusViewModel @Inject constructor(
    repository: StudyRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filter = savedStateHandle
        .getStateFlow(KEY_FILTER, ZoneFilter.ALL.name)
        .map { stored -> ZoneFilter.entries.firstOrNull { it.name == stored } ?: ZoneFilter.ALL }
    private val selectedSubjectId = MutableStateFlow<Long?>(savedStateHandle[KEY_SUBJECT])
    private val expandedSubjectIds = MutableStateFlow<Set<Long>>(emptySet())

    private val criteria = combine(query.debounce(150), filter) { search, selectedFilter ->
        SearchCriteria(search.trim(), selectedFilter)
    }

    val uiState: StateFlow<SyllabusUiState> = combine(
        repository.subjects,
        repository.chapters,
        criteria,
        selectedSubjectId,
        expandedSubjectIds,
    ) { subjects, chapters, search, selected, expanded ->
        val term = search.query.lowercase()
        val matching = chapters.filter { chapter ->
            search.filter.accepts(chapter.zone) && (
                term.isBlank() ||
                    chapter.title.lowercase().contains(term) ||
                    chapter.unitName.lowercase().contains(term) ||
                    chapter.subjectName.lowercase().contains(term)
                )
        }
        val summaries = matching
            .groupBy { it.subjectId }
            .mapNotNull { (_, subjectChapters) ->
                val subject = subjects.firstOrNull { it.id == subjectChapters.first().subjectId }
                    ?: return@mapNotNull null
                val active = subjectChapters.filter { it.zone != Zone.LOCKED }
                SubjectSummary(
                    subject = subject,
                    chapters = subjectChapters,
                    averageBattery = if (active.isEmpty()) 0f
                    else active.map { it.battery }.average().toFloat(),
                    redCount = subjectChapters.count { it.zone == Zone.RED },
                    amberCount = subjectChapters.count { it.zone == Zone.AMBER },
                    greenCount = subjectChapters.count { it.zone == Zone.GREEN },
                    lockedCount = subjectChapters.count { it.zone == Zone.LOCKED },
                )
            }
            .sortedBy { it.subject.name }

        SyllabusUiState(
            subjects = summaries,
            query = search.query,
            filter = search.filter,
            selectedSubjectId = selected ?: summaries.firstOrNull()?.subject?.id,
            expandedSubjectIds = expanded,
            totalSubjects = subjects.size,
            totalChapters = chapters.size,
            visibleChapters = matching.size,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyllabusUiState(),
    )

    fun setQuery(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun setFilter(value: ZoneFilter) {
        savedStateHandle[KEY_FILTER] = value.name
    }

    fun selectSubject(subjectId: Long) {
        selectedSubjectId.value = subjectId
        savedStateHandle[KEY_SUBJECT] = subjectId
    }

    fun toggleExpanded(subjectId: Long) {
        expandedSubjectIds.value = expandedSubjectIds.value.let { current ->
            if (subjectId in current) current - subjectId else current + subjectId
        }
    }

    fun clearNarrowing() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_FILTER] = ZoneFilter.ALL.name
    }

    private companion object {
        const val KEY_QUERY = "syllabus_query"
        const val KEY_FILTER = "syllabus_filter"
        const val KEY_SUBJECT = "syllabus_subject"
    }
}
