package com.studyapp.decaycharge.data.importer

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.Subject

object SyllabusTextParser {
    fun parse(text: String, title: String = "Pasted syllabus"): SyllabusDraft {
        val subjects = linkedMapOf<String, MutableSubject>()
        var currentSubject: MutableSubject? = null
        var currentUnit: String? = null

        text.lineSequence().forEach { rawLine ->
            if (rawLine.isBlank()) return@forEach
            val trimmedStart = rawLine.trimStart()
            if (trimmedStart.startsWith("#") || trimmedStart.startsWith("//")) return@forEach

            val indent = rawLine.takeWhile { it == ' ' || it == '\t' }.sumOf {
                if (it == '\t') 4 else 1
            }
            var content = trimmedStart.trim()
            val bullet = content.startsWith("-") || content.startsWith("*") || content.startsWith("•")
            if (bullet) {
                content = content.drop(1).trim()
            }
            if (content.isBlank()) return@forEach

            val metadata = extractMetadata(content)
            val clean = metadata.title
            if (clean.isBlank()) return@forEach
            val colonIndex = clean.indexOf(':')
            val hasPair = colonIndex in 1 until clean.lastIndex

            when {
                bullet && hasPair -> {
                    val subject = currentSubject ?: ensureSubject(subjects, "Imported")
                        .also { currentSubject = it }
                    val unit = clean.substring(0, colonIndex).trim().ifBlank { currentUnit }
                    currentUnit = unit
                    subject.chapters += metadata.chapter(
                        title = clean.substring(colonIndex + 1).trim(),
                        unitName = unit,
                    )
                }

                bullet -> {
                    val subject = currentSubject ?: ensureSubject(subjects, "Imported")
                        .also { currentSubject = it }
                    subject.chapters += metadata.chapter(
                        title = clean,
                        unitName = currentUnit,
                    )
                }

                indent == 0 && hasPair -> {
                    val subjectName = clean.substring(0, colonIndex).trim()
                    val chapterTitle = clean.substring(colonIndex + 1).trim()
                    val subject = ensureSubject(subjects, subjectName)
                    currentSubject = subject
                    currentUnit = null
                    subject.chapters += metadata.chapter(title = chapterTitle, unitName = null)
                }

                indent == 0 -> {
                    val subjectName = clean.removeSuffix(":").trim()
                    if (subjectName.isNotBlank()) {
                        currentSubject = ensureSubject(subjects, subjectName)
                        currentUnit = null
                    }
                }

                hasPair -> {
                    val subject = currentSubject ?: ensureSubject(subjects, "Imported")
                        .also { currentSubject = it }
                    val unit = clean.substring(0, colonIndex).trim().ifBlank { currentUnit }
                    currentUnit = unit
                    subject.chapters += metadata.chapter(
                        title = clean.substring(colonIndex + 1).trim(),
                        unitName = unit,
                    )
                }

                else -> {
                    currentUnit = clean.removePrefix("Unit:").trim().ifBlank { null }
                }
            }
        }

        val built = subjects.values
            .filter { it.chapters.isNotEmpty() }
            .mapIndexed { index, subject ->
                DraftSubject(
                    name = subject.name,
                    colorHex = SubjectPalette.colorAt(index),
                    examWeight = 1f,
                    chapters = subject.chapters,
                )
            }
        return SyllabusDraft(title = title, subjects = built)
    }

    private fun ensureSubject(
        subjects: MutableMap<String, MutableSubject>,
        rawName: String,
    ): MutableSubject {
        val name = rawName.trim().ifBlank { "Imported" }
        val key = name.lowercase()
        return subjects.getOrPut(key) { MutableSubject(name) }
    }

    private fun extractMetadata(raw: String): ParsedChapter {
        val match = METADATA_REGEX.find(raw) ?: return ParsedChapter(raw.trim())
        val metadata = match.groupValues[1]
        val lower = metadata.lowercase()
        val hasMetadata = listOf(
            "difficulty",
            "target",
            "heavy",
            "light",
            "medium",
            "easy",
            "hard",
        ).any(lower::contains) || TARGET_ONLY_REGEX.containsMatchIn(lower)
        if (!hasMetadata) return ParsedChapter(raw.trim())

        val title = raw.substring(0, match.range.first).trim()
        val difficultyToken = DIFFICULTY_REGEX.find(metadata)?.groupValues?.getOrNull(1)
            ?: DIFFICULTY_BARE_REGEX.find(metadata)?.value
        val targetMatch = TARGET_REGEX.find(metadata)
            ?: TARGET_ONLY_REGEX.find(metadata)
        val targetMinutes = targetMatch?.let { parsedTarget(it) } ?: 45
        return ParsedChapter(
            title = title,
            difficulty = difficultyToken?.let(ChapterDifficulty::fromToken)
                ?: ChapterDifficulty.MEDIUM,
            targetMinutes = targetMinutes,
        )
    }

    private fun parsedTarget(match: MatchResult): Int {
        val amount = match.groupValues[1].toIntOrNull() ?: 45
        val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
        val minutes = if (unit.startsWith("h")) amount * 60 else amount
        return minutes.coerceIn(5, 240)
    }

    private data class MutableSubject(
        val name: String,
        val chapters: MutableList<DraftChapter> = mutableListOf(),
    )

    private data class ParsedChapter(
        val title: String,
        val difficulty: ChapterDifficulty = ChapterDifficulty.MEDIUM,
        val targetMinutes: Int = 45,
    ) {
        fun chapter(title: String, unitName: String?): DraftChapter = DraftChapter(
            title = title,
            unitName = unitName?.trim()?.ifBlank { null },
            difficulty = difficulty,
            targetMinutes = targetMinutes,
        )
    }

    private val METADATA_REGEX = Regex("""\s*[\(\[]([^)\]]+)[\)\]]\s*$""")
    private val DIFFICULTY_REGEX = Regex(
        """(?i)(?:difficulty|weight)\s*[:=]\s*([a-z]+)""",
    )
    private val DIFFICULTY_BARE_REGEX = Regex("""(?i)\b(light|medium|heavy|easy|hard)\b""")
    private val TARGET_REGEX = Regex(
        """(?i)(?:target|time|duration)\s*[:=]\s*(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)?""",
    )
    private val TARGET_ONLY_REGEX = Regex("""(?i)\b(\d+)\s*(m|min|mins|minutes|h|hr|hrs|hours)\b""")
}

object SyllabusCsv {
    fun parse(text: String, title: String = "Imported CSV"): SyllabusDraft {
        val rows = parseRows(text)
        if (rows.isEmpty()) return SyllabusDraft(title, emptyList())

        val header = rows.first().map { it.removePrefix("\uFEFF").trim().lowercase() }
        val hasHeader = header.contains("subject") && header.contains("chapter")
        val subjectIndex = if (hasHeader) header.indexOf("subject") else 0
        val unitIndex = if (hasHeader) header.indexOf("unit") else 1
        val chapterIndex = if (hasHeader) header.indexOf("chapter") else 2
        val difficultyIndex = if (hasHeader) header.indexOf("difficulty") else 3
        val targetIndex = if (hasHeader) {
            header.indexOfFirst { it == "target" || it == "targetminutes" || it == "minutes" }
        } else {
            4
        }
        val dataRows = if (hasHeader) rows.drop(1) else rows

        val subjects = linkedMapOf<String, MutableSubject>()
        dataRows.forEach { row ->
            val subjectName = row.getOrNull(subjectIndex)?.trim().orEmpty()
            val chapterTitle = row.getOrNull(chapterIndex)?.trim().orEmpty()
            if (subjectName.isBlank() || chapterTitle.isBlank()) return@forEach
            val subject = subjects.getOrPut(subjectName.lowercase()) { MutableSubject(subjectName) }
            subject.chapters += DraftChapter(
                title = chapterTitle,
                unitName = row.getOrNull(unitIndex)?.trim()?.ifBlank { null },
                difficulty = ChapterDifficulty.fromToken(
                    row.getOrNull(difficultyIndex).orEmpty(),
                ),
                targetMinutes = parseTarget(row.getOrNull(targetIndex)),
            )
        }

        return SyllabusDraft(
            title = title,
            subjects = subjects.values.mapIndexed { index, subject ->
                DraftSubject(
                    name = subject.name,
                    colorHex = SubjectPalette.colorAt(index),
                    chapters = subject.chapters,
                )
            },
        )
    }

    fun encode(subjects: List<Subject>, chapters: List<Chapter>): String = buildString {
        appendLine("Subject,Unit,Chapter,Difficulty,TargetMinutes")
        val subjectsById = subjects.associateBy { it.id }
        chapters
            .filter { subjectsById.containsKey(it.subjectId) }
            .sortedWith(compareBy({ it.subjectName.lowercase() }, { it.unitName.orEmpty() }, { it.title }))
            .forEach { chapter ->
                appendLine(
                    listOf(
                        chapter.subjectName,
                        chapter.unitName,
                        chapter.title,
                        ChapterDifficulty.fromWeight(chapter.difficultyWeight).label,
                        chapter.targetMinutes.toString(),
                    ).joinToString(",") { escape(it) },
                )
            }
    }

    private fun parseTarget(raw: String?): Int {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return 45
        val number = Regex("""\d+""").find(value)?.value?.toIntOrNull() ?: return 45
        return if (value.lowercase().endsWith("h") || value.lowercase().contains("hour")) {
            (number * 60).coerceIn(5, 240)
        } else {
            number.coerceIn(5, 240)
        }
    }

    private fun parseRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val current = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }

                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    current += cell.toString()
                    cell.clear()
                }

                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') {
                        index++
                    }
                    current += cell.toString()
                    cell.clear()
                    if (current.any { it.isNotBlank() }) rows += current.toList()
                    current.clear()
                }

                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || current.isNotEmpty()) {
            current += cell.toString()
            if (current.any { it.isNotBlank() }) rows += current.toList()
        }
        return rows
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private data class MutableSubject(
        val name: String,
        val chapters: MutableList<DraftChapter> = mutableListOf(),
    )
}
