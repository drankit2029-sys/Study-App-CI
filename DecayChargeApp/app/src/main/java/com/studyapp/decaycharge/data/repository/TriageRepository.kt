package com.studyapp.decaycharge.data.repository

import androidx.room.withTransaction
import com.studyapp.decaycharge.data.local.ChapterDao
import com.studyapp.decaycharge.data.local.CryoFreezeDao
import com.studyapp.decaycharge.data.local.DecayChargeDatabase
import com.studyapp.decaycharge.data.local.StudySessionDao
import com.studyapp.decaycharge.data.local.StudySessionEntity
import com.studyapp.decaycharge.data.local.SubjectDao
import com.studyapp.decaycharge.data.local.SubjectEntity
import com.studyapp.decaycharge.domain.AppClock
import com.studyapp.decaycharge.domain.CryoFreezeInterval
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.model.Subject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

data class StudyContent(
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
)

data class CompletedCharge(
    val chapterId: Long,
    val mode: ChargeMode,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val durationMinutes: Int,
    val batteryBefore: Float,
    val batteryAfter: Float,
    val stabilityBefore: Float,
    val stabilityAfter: Float,
    val recallRating: Int,
    val note: String?,
)

interface TriageRepository {
    val content: StateFlow<StudyContent>
    fun chapter(chapterId: Long): Flow<Chapter?>
    suspend fun saveCompletedCharge(charge: CompletedCharge): Boolean
}

@Singleton
class OfflineTriageRepository @Inject constructor(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val cryoFreezeDao: CryoFreezeDao,
    private val studySessionDao: StudySessionDao,
    private val database: DecayChargeDatabase,
    private val clock: AppClock,
) : TriageRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val ticker = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.nowEpochMs())
            delay(TICKER_INTERVAL_MS)
        }
    }

    override val content: StateFlow<StudyContent> = combine(
        subjectDao.observeAll(),
        chapterDao.observeAll(),
        cryoFreezeDao.observeAll(),
        ticker,
    ) { subjectEntities, chapterEntities, freezeEntities, nowEpochMs ->
        val subjects = subjectEntities.map { it.toDomain() }
        val subjectsById = subjects.associateBy { it.id }
        val freezes = freezeEntities.map {
            CryoFreezeInterval(startEpochMs = it.startEpochMs, endEpochMs = it.endEpochMs)
        }
        val chapters = chapterEntities.mapNotNull { entity ->
            val subject = subjectsById[entity.subjectId] ?: return@mapNotNull null
            val decay = DecayCalculator.snapshot(
                lastChargedAtEpochMs = entity.lastChargedAtEpochMs,
                nowEpochMs = nowEpochMs,
                freezes = freezes,
                lastBatteryLevel = entity.lastBatteryLevel,
                status = entity.status,
                stabilityDays = entity.stabilityDays,
                difficultyWeight = entity.difficultyWeight,
            )
            Chapter(
                id = entity.id,
                subjectId = subject.id,
                subjectName = subject.name,
                subjectShortName = subject.shortName,
                subjectColorHex = subject.colorHex,
                unitName = entity.unitName.orEmpty(),
                title = entity.title,
                status = entity.status,
                battery = decay.battery,
                stabilityDays = entity.stabilityDays,
                daysSinceCharge = decay.daysSinceCharge,
                drainRatePerDay = decay.drainRatePerDay,
                difficultyWeight = entity.difficultyWeight,
                targetMinutes = entity.targetMinutes,
                reviewCount = entity.reviewCount,
                note = entity.breadcrumbNote,
            )
        }
        StudyContent(subjects = subjects, chapters = chapters)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudyContent(),
        )

    override fun chapter(chapterId: Long): Flow<Chapter?> = content
        .map { snapshot -> snapshot.chapters.firstOrNull { it.id == chapterId } }
        .distinctUntilChanged()

    override suspend fun saveCompletedCharge(charge: CompletedCharge): Boolean {
        var saved = false
        database.withTransaction {
            val chapter = chapterDao.getById(charge.chapterId) ?: return@withTransaction
            chapterDao.upsert(
                chapter.copy(
                    lastChargedAtEpochMs = charge.endedAtEpochMs,
                    lastBatteryLevel = charge.batteryAfter.coerceIn(0f, 100f),
                    stabilityDays = charge.stabilityAfter,
                    reviewCount = chapter.reviewCount + 1,
                    totalMinutesStudied = chapter.totalMinutesStudied + charge.durationMinutes,
                    breadcrumbNote = charge.note?.takeIf { it.isNotBlank() },
                ),
            )
            studySessionDao.insert(
                StudySessionEntity(
                    chapterId = charge.chapterId,
                    startedAtEpochMs = charge.startedAtEpochMs,
                    endedAtEpochMs = charge.endedAtEpochMs,
                    durationMinutes = charge.durationMinutes,
                    mode = charge.mode,
                    batteryBefore = charge.batteryBefore,
                    batteryAfter = charge.batteryAfter,
                    stabilityBefore = charge.stabilityBefore,
                    stabilityAfter = charge.stabilityAfter,
                    recallRating = charge.recallRating,
                    sessionNote = charge.note?.takeIf { it.isNotBlank() },
                ),
            )
            saved = true
        }
        return saved
    }

    private fun SubjectEntity.toDomain() = Subject(
        id = id,
        name = name,
        shortName = shortName(name),
        colorHex = colorHex.toColorLong(),
        examWeight = examWeight,
    )

    private fun shortName(name: String): String {
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size > 1) {
            return words.take(3).joinToString("") { word -> word.take(1).uppercase() }
        }
        return name.trim().take(3).uppercase()
    }

    private fun String.toColorLong(): Long {
        val raw = trim().removePrefix("#")
        val parsed = raw.toLongOrNull(16) ?: DEFAULT_SUBJECT_COLOR
        return if (raw.length <= 6) parsed or 0xFF000000 else parsed
    }

    private companion object {
        const val TICKER_INTERVAL_MS = 60_000L
        const val DEFAULT_SUBJECT_COLOR = 0xFF0F5132
    }
}
