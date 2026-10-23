package com.studyapp.decaycharge.data.importer

import kotlinx.serialization.Serializable

enum class ChapterDifficulty(val label: String, val weight: Float) {
    LIGHT("Light", 0.8f),
    MEDIUM("Medium", 1f),
    HEAVY("Heavy", 1.2f);

    companion object {
        fun fromToken(token: String): ChapterDifficulty = when (token.trim().lowercase()) {
            "light", "easy", "low", "0.8" -> LIGHT
            "heavy", "hard", "high", "1.2" -> HEAVY
            else -> MEDIUM
        }

        fun fromWeight(weight: Float): ChapterDifficulty = when {
            weight >= 1.15f -> HEAVY
            weight >= 0.95f -> MEDIUM
            else -> LIGHT
        }
    }
}

data class DraftChapter(
    val title: String,
    val unitName: String? = null,
    val difficulty: ChapterDifficulty = ChapterDifficulty.MEDIUM,
    val targetMinutes: Int = 45,
    val selected: Boolean = true,
)

data class DraftSubject(
    val name: String,
    val colorHex: String,
    val examWeight: Float = 1f,
    val chapters: List<DraftChapter> = emptyList(),
)

data class SyllabusDraft(
    val title: String,
    val subjects: List<DraftSubject>,
) {
    val subjectCount: Int
        get() = subjects.count { subject -> subject.chapters.any { it.selected } }

    val chapterCount: Int
        get() = subjects.sumOf { subject -> subject.chapters.count { it.selected } }

    val totalChapterCount: Int
        get() = subjects.sumOf { it.chapters.size }

    fun toggleChapter(subjectIndex: Int, chapterIndex: Int): SyllabusDraft = copy(
        subjects = subjects.mapIndexed { currentSubjectIndex, subject ->
            if (currentSubjectIndex != subjectIndex) {
                subject
            } else {
                subject.copy(
                    chapters = subject.chapters.mapIndexed { currentChapterIndex, chapter ->
                        if (currentChapterIndex != chapterIndex) {
                            chapter
                        } else {
                            chapter.copy(selected = !chapter.selected)
                        }
                    },
                )
            }
        },
    )

    fun setAllSelected(selected: Boolean): SyllabusDraft = copy(
        subjects = subjects.map { subject ->
            subject.copy(chapters = subject.chapters.map { it.copy(selected = selected) })
        },
    )

    fun applyDifficulty(difficulty: ChapterDifficulty): SyllabusDraft = copy(
        subjects = subjects.map { subject ->
            subject.copy(
                chapters = subject.chapters.map { chapter ->
                    if (chapter.selected) chapter.copy(difficulty = difficulty) else chapter
                },
            )
        },
    )

    fun applyTarget(targetMinutes: Int): SyllabusDraft = copy(
        subjects = subjects.map { subject ->
            subject.copy(
                chapters = subject.chapters.map { chapter ->
                    if (chapter.selected) chapter.copy(targetMinutes = targetMinutes) else chapter
                },
            )
        },
    )

    fun selectedOnly(): SyllabusDraft = copy(
        subjects = subjects.mapNotNull { subject ->
            val selected = subject.chapters.filter { it.selected }
            if (selected.isEmpty()) null else subject.copy(chapters = selected)
        },
    )
}

data class SyllabusPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val subjects: List<DraftSubject>,
) {
    val subjectCount: Int
        get() = subjects.size

    val chapterCount: Int
        get() = subjects.sumOf { it.chapters.size }

    fun toDraft(): SyllabusDraft = SyllabusDraft(
        title = title,
        subjects = subjects,
    )
}

@Serializable
internal data class SyllabusPackDto(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: String = "",
    val subjects: List<SubjectDto> = emptyList(),
)

@Serializable
internal data class SubjectDto(
    val name: String,
    val colorHex: String = "",
    val examWeight: Float = 1f,
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
internal data class ChapterDto(
    val title: String,
    val unit: String? = null,
    val difficulty: String = "MEDIUM",
    val targetMinutes: Int = 45,
)

object SubjectPalette {
    val colors: List<String> = listOf(
        "#0F5132",
        "#A65D50",
        "#B4864C",
        "#64776C",
        "#80627A",
        "#5D8490",
        "#7A6A4F",
        "#3F6F64",
    )

    fun colorAt(index: Int): String = colors[index.mod(colors.size)]

    private fun Int.mod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
}
