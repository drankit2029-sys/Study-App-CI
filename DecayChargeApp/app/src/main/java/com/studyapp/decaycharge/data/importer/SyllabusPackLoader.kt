package com.studyapp.decaycharge.data.importer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class SyllabusPackLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun loadPacks(): List<SyllabusPack> = withContext(Dispatchers.IO) {
        val assetNames = runCatching {
            context.assets.list(ASSET_DIRECTORY)?.toList().orEmpty()
        }.getOrDefault(emptyList())
        assetNames
            .filter { it.endsWith(".json", ignoreCase = true) }
            .sorted()
            .mapNotNull { fileName -> runCatching { loadPack(fileName) }.getOrNull() }
            .sortedWith(compareBy({ it.category }, { it.title }))
    }

    private fun loadPack(fileName: String): SyllabusPack {
        val raw = context.assets.open("$ASSET_DIRECTORY/$fileName").bufferedReader().use {
            it.readText()
        }
        val dto = json.decodeFromString<SyllabusPackDto>(raw)
        val subjects = dto.subjects.mapIndexed { index, subject ->
            DraftSubject(
                name = subject.name.trim(),
                colorHex = subject.colorHex.ifBlank { SubjectPalette.colorAt(index) },
                examWeight = subject.examWeight,
                chapters = subject.chapters.mapNotNull { chapter ->
                    val title = chapter.title.trim()
                    if (title.isEmpty()) {
                        null
                    } else {
                        DraftChapter(
                            title = title,
                            unitName = chapter.unit?.trim()?.ifBlank { null },
                            difficulty = ChapterDifficulty.fromToken(chapter.difficulty),
                            targetMinutes = chapter.targetMinutes.coerceIn(5, 240),
                        )
                    }
                },
            )
        }.filter { it.name.isNotBlank() && it.chapters.isNotEmpty() }
        return SyllabusPack(
            id = dto.id.ifBlank { fileName.removeSuffix(".json") },
            title = dto.title.ifBlank { fileName.removeSuffix(".json") },
            subtitle = dto.subtitle,
            category = dto.category,
            subjects = subjects,
        )
    }

    private companion object {
        const val ASSET_DIRECTORY = "syllabuses"
    }
}
