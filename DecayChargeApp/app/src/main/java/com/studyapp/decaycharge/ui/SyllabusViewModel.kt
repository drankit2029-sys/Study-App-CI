package com.studyapp.decaycharge.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.SyllabusFilter
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
import kotlinx.coroutines.launch

data class SyllabusUiState(
    val subjects: List<SubjectSummary> = emptyList(),
    val query: String = "",
    val filter: SyllabusFilter = SyllabusFilter.ALL,
    val selectedSubjectId: Long? = null,
    val expandedSubjectIds: Set<Long> = emptySet(),
    val selectedChapterIds: Set<Long> = emptySet(),
    val totalSubjects: Int = 0,
    val totalChapters: Int = 0,
    val visibleChapters: Int = 0,
    val isLoading: Boolean = true,
) {
    val hasNarrowing: Boolean
        get() = query.isNotBlank() || filter != SyllabusFilter.ALL

    val selectedSummary: SubjectSummary?
        get() = subjects.firstOrNull { it.subject.id == selectedSubjectId } ?: subjects.firstOrNull()

    val selectionCount: Int
        get() = selectedChapterIds.size

    val allVisibleSelected: Boolean
        get() {
            val visibleIds = subjects.flatMap { summary -> summary.chapters.map { it.id } }
            return visibleIds.isNotEmpty() && visibleIds.all { it in selectedChapterIds }
        }
}

private data class SearchCriteria(
    val query: String,
    val filter: SyllabusFilter,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SyllabusViewModel @Inject constructor(
    private val repository: TriageRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filter = savedStateHandle
        .getStateFlow(KEY_FILTER, SyllabusFilter.ALL.name)
        .map { stored ->
            SyllabusFilter.entries.firstOrNull { it.name == stored } ?: SyllabusFilter.ALL
        }
    private val selectedSubjectId = MutableStateFlow<Long?>(savedStateHandle[KEY_SUBJECT])
    private val expandedSubjectIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedChapterIds = MutableStateFlow<Set<Long>>(emptySet())

    private val criteria = combine(query.debounce(150), filter) { search, selectedFilter ->
        SearchCriteria(search.trim(), selectedFilter)
    }

    val uiState: StateFlow<SyllabusUiState> = combine(
        repository.content,
        criteria,
        selectedSubjectId,
        expandedSubjectIds,
        selectedChapterIds,
    ) { content, search, selected, expanded, selectedChapters ->
        val subjects = content.subjects
        val chapters = content.chapters
        val term = search.query.lowercase()
        val matching = chapters.filter { chapter ->
            search.filter.accepts(chapter) && (
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
                val active = subjectChapters.filter { it.status == ChapterStatus.ACTIVE }
                SubjectSummary(
                    subject = subject,
                    chapters = subjectChapters,
                    averageBattery = if (active.isEmpty()) 0f
                    else active.map { it.battery }.average().toFloat(),
                    redCount = subjectChapters.count { it.zone == com.studyapp.decaycharge.model.Zone.RED },
                    amberCount = subjectChapters.count { it.zone == com.studyapp.decaycharge.model.Zone.AMBER },
                    greenCount = subjectChapters.count { it.zone == com.studyapp.decaycharge.model.Zone.GREEN },
                    lockedCount = subjectChapters.count { it.zone == com.studyapp.decaycharge.model.Zone.LOCKED },
                )
            }
            .sortedBy { it.subject.name }

        SyllabusUiState(
            subjects = summaries,
            query = search.query,
            filter = search.filter,
            selectedSubjectId = selected ?: summaries.firstOrNull()?.subject?.id,
            expandedSubjectIds = expanded,
            selectedChapterIds = selectedChapters,
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

    fun setFilter(value: SyllabusFilter) {
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

    fun toggleChapterSelection(chapterId: Long) {
        selectedChapterIds.value = selectedChapterIds.value.let { current ->
            if (chapterId in current) current - chapterId else current + chapterId
        }
    }

    fun clearSelection() {
        selectedChapterIds.value = emptySet()
    }

    fun toggleSelectAllVisible() {
        val current = uiState.value
        val visibleIds = current.subjects.flatMap { summary -> summary.chapters.map { it.id } }.toSet()
        selectedChapterIds.value = if (visibleIds.isNotEmpty() && visibleIds.all { it in current.selectedChapterIds }) {
            current.selectedChapterIds - visibleIds
        } else {
            current.selectedChapterIds + visibleIds
        }
    }

    fun unlockSelected() = updateSelectedStatus(ChapterStatus.ACTIVE)

    fun archiveSelected() = updateSelectedStatus(ChapterStatus.ARCHIVED)

    fun setSelectedTarget(minutes: Int) {
        val ids = selectedChapterIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateChapterTarget(ids, minutes)
            selectedChapterIds.value = emptySet()
        }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(repository.exportCsv())
        }
    }

    fun clearNarrowing() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[KEY_FILTER] = SyllabusFilter.ALL.name
    }

    private fun updateSelectedStatus(status: ChapterStatus) {
        val ids = selectedChapterIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateChapterStatus(ids, status)
            selectedChapterIds.value = emptySet()
        }
    }

    private companion object {
        const val KEY_QUERY = "syllabus_query"
        const val KEY_FILTER = "syllabus_filter"
        const val KEY_SUBJECT = "syllabus_subject"
    }
}
