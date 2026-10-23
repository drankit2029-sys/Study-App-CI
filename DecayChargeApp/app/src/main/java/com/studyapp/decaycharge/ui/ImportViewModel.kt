package com.studyapp.decaycharge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyapp.decaycharge.data.importer.ChapterDifficulty
import com.studyapp.decaycharge.data.importer.DraftChapter
import com.studyapp.decaycharge.data.importer.DraftSubject
import com.studyapp.decaycharge.data.importer.SubjectPalette
import com.studyapp.decaycharge.data.importer.SyllabusCsv
import com.studyapp.decaycharge.data.importer.SyllabusDraft
import com.studyapp.decaycharge.data.importer.SyllabusPack
import com.studyapp.decaycharge.data.importer.SyllabusPackLoader
import com.studyapp.decaycharge.data.importer.SyllabusTextParser
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportSource(val label: String) {
    LIBRARY("Library"),
    PASTE("Paste"),
    CSV("CSV"),
    CUSTOM("Custom"),
}

data class ImportUiState(
    val source: ImportSource = ImportSource.LIBRARY,
    val packs: List<SyllabusPack> = emptyList(),
    val isLoadingPacks: Boolean = true,
    val pasteText: String = "",
    val csvFileName: String = "",
    val customSubject: String = "",
    val customUnit: String = "",
    val customChapter: String = "",
    val customDifficulty: ChapterDifficulty = ChapterDifficulty.MEDIUM,
    val customTargetMinutes: Int = 45,
    val draft: SyllabusDraft? = null,
    val isPreviewing: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: TriageRepository,
    private val packLoader: SyllabusPackLoader,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _imported = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val imported: SharedFlow<Int> = _imported.asSharedFlow()

    init {
        viewModelScope.launch {
            val packs = runCatching { packLoader.loadPacks() }.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    packs = packs,
                    isLoadingPacks = false,
                    error = if (packs.isEmpty()) {
                        "No offline packs were found. Paste a syllabus or import a CSV instead."
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun setSource(source: ImportSource) {
        _uiState.update { state ->
            val clearCustom = state.draft?.title == CUSTOM_TITLE && source != ImportSource.CUSTOM
            state.copy(
                source = source,
                draft = if (clearCustom) null else state.draft,
                isPreviewing = false,
                error = null,
            )
        }
    }

    fun setPasteText(value: String) {
        _uiState.update { it.copy(pasteText = value, error = null) }
    }

    fun openPack(pack: SyllabusPack) {
        val draft = pack.toDraft()
        _uiState.update {
            it.copy(draft = draft, isPreviewing = true, error = null)
        }
    }

    fun previewPaste() {
        val text = _uiState.value.pasteText
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Paste some syllabus text first.") }
            return
        }
        val draft = SyllabusTextParser.parse(text)
        if (draft.chapterCount == 0) {
            _uiState.update {
                it.copy(error = "No chapters were found. Use 'Subject: Chapter' or indented lines.")
            }
            return
        }
        _uiState.update { it.copy(draft = draft, isPreviewing = true, error = null) }
    }

    fun loadCsvText(text: String, fileName: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "That CSV file is empty.") }
            return
        }
        val draft = SyllabusCsv.parse(text, title = fileName.ifBlank { "Imported CSV" })
        if (draft.chapterCount == 0) {
            _uiState.update {
                it.copy(
                    csvFileName = fileName,
                    error = "No chapters were found. Check the Subject, Unit, Chapter columns.",
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                source = ImportSource.CSV,
                csvFileName = fileName,
                draft = draft,
                isPreviewing = true,
                error = null,
            )
        }
    }

    fun setCustomSubject(value: String) {
        _uiState.update { it.copy(customSubject = value, error = null) }
    }

    fun setCustomUnit(value: String) {
        _uiState.update { it.copy(customUnit = value) }
    }

    fun setCustomChapter(value: String) {
        _uiState.update { it.copy(customChapter = value, error = null) }
    }

    fun setCustomDifficulty(value: ChapterDifficulty) {
        _uiState.update { it.copy(customDifficulty = value) }
    }

    fun setCustomTargetMinutes(value: Int) {
        _uiState.update { it.copy(customTargetMinutes = value.coerceIn(5, 240)) }
    }

    fun addCustomChapter() {
        val state = _uiState.value
        val subjectName = state.customSubject.trim()
        val chapterTitle = state.customChapter.trim()
        if (subjectName.isBlank()) {
            _uiState.update { it.copy(error = "Add a subject name first.") }
            return
        }
        if (chapterTitle.isBlank()) {
            _uiState.update { it.copy(error = "Add a chapter title first.") }
            return
        }

        val existing = state.draft?.takeIf { it.title == CUSTOM_TITLE }
            ?: SyllabusDraft(CUSTOM_TITLE, emptyList())
        val subjectIndex = existing.subjects.indexOfFirst {
            it.name.equals(subjectName, ignoreCase = true)
        }
        val subjects = existing.subjects.toMutableList()
        val chapter = DraftChapter(
            title = chapterTitle,
            unitName = state.customUnit.trim().ifBlank { null },
            difficulty = state.customDifficulty,
            targetMinutes = state.customTargetMinutes,
        )
        if (subjectIndex >= 0) {
            val subject = subjects[subjectIndex]
            subjects[subjectIndex] = subject.copy(chapters = subject.chapters + chapter)
        } else {
            subjects += DraftSubject(
                name = subjectName,
                colorHex = SubjectPalette.colorAt(subjects.size),
                examWeight = 1f,
                chapters = listOf(chapter),
            )
        }
        _uiState.update {
            it.copy(
                source = ImportSource.CUSTOM,
                draft = existing.copy(subjects = subjects),
                isPreviewing = false,
                customChapter = "",
                error = null,
            )
        }
    }

    fun showPreview() {
        val draft = _uiState.value.draft
        if (draft == null || draft.chapterCount == 0) {
            _uiState.update { it.copy(error = "Add at least one chapter before reviewing.") }
            return
        }
        _uiState.update { it.copy(isPreviewing = true, error = null) }
    }

    fun backFromPreview() {
        _uiState.update { it.copy(isPreviewing = false, error = null) }
    }

    fun clearDraft() {
        _uiState.update {
            it.copy(
                draft = null,
                isPreviewing = false,
                csvFileName = "",
                error = null,
            )
        }
    }

    fun toggleChapter(subjectIndex: Int, chapterIndex: Int) {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(draft = draft.toggleChapter(subjectIndex, chapterIndex)) }
    }

    fun setAllSelected(selected: Boolean) {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(draft = draft.setAllSelected(selected)) }
    }

    fun applyDifficulty(difficulty: ChapterDifficulty) {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(draft = draft.applyDifficulty(difficulty)) }
    }

    fun applyTarget(minutes: Int) {
        val draft = _uiState.value.draft ?: return
        _uiState.update { it.copy(draft = draft.applyTarget(minutes)) }
    }

    fun importSelected() {
        val state = _uiState.value
        if (state.isImporting) return
        val selected = state.draft?.selectedOnly()
        if (selected == null || selected.subjects.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one chapter to import.") }
            return
        }
        _uiState.update { it.copy(isImporting = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { repository.importSyllabus(selected) }
            result.onSuccess { count ->
                preferences.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(isImporting = false, draft = null, isPreviewing = false)
                }
                _imported.emit(count)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "The syllabus could not be imported. Please try again.",
                    )
                }
            }
        }
    }

    private companion object {
        const val CUSTOM_TITLE = "Custom syllabus"
    }
}
