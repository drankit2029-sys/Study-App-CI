package com.studyapp.decaycharge.data.repository

import androidx.room.withTransaction
import com.studyapp.decaycharge.data.prefs.UserPreferences
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.data.local.ChapterDao
import com.studyapp.decaycharge.data.local.ChapterEntity
import com.studyapp.decaycharge.data.local.CryoFreezeDao
import com.studyapp.decaycharge.data.local.CryoFreezeEntity
import com.studyapp.decaycharge.data.local.DecayChargeDatabase
import com.studyapp.decaycharge.data.local.StudySessionDao
import com.studyapp.decaycharge.data.local.StudySessionEntity
import com.studyapp.decaycharge.data.local.SubjectDao
import com.studyapp.decaycharge.data.local.SubjectEntity
import com.studyapp.decaycharge.data.importer.SyllabusCsv
import com.studyapp.decaycharge.data.importer.SyllabusDraft
import com.studyapp.decaycharge.domain.AppClock
import com.studyapp.decaycharge.domain.CryoFreezeInterval
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.model.CryoFreezeStatus
import com.studyapp.decaycharge.model.StudySessionRecord
import com.studyapp.decaycharge.model.Subject
import com.studyapp.decaycharge.widget.WidgetUpdater
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
    val activeFreeze: CryoFreezeStatus? = null,
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
    val sessions: StateFlow<List<StudySessionRecord>>
    fun chapter(chapterId: Long): Flow<Chapter?>
    suspend fun saveCompletedCharge(charge: CompletedCharge): Boolean
    suspend fun importSyllabus(draft: SyllabusDraft): Int
    suspend fun updateChapterStatus(chapterIds: Set<Long>, status: ChapterStatus)
    suspend fun updateChapterTarget(chapterIds: Set<Long>, targetMinutes: Int)
    suspend fun exportCsv(): String
    suspend fun purgeLegacyDemoData(): Boolean
    suspend fun startCryoFreeze(hours: Int?, note: String?)
    suspend fun extendCryoFreeze(hours: Int)
    suspend fun endCryoFreeze()
}

@Singleton
class OfflineTriageRepository @Inject constructor(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val cryoFreezeDao: CryoFreezeDao,
    private val studySessionDao: StudySessionDao,
    private val database: DecayChargeDatabase,
    private val preferences: UserPreferencesRepository,
    private val clock: AppClock,
    private val widgetUpdater: WidgetUpdater,
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
        preferences.preferences,
    ) { subjectEntities, chapterEntities, freezeEntities, nowEpochMs, prefs ->
        buildContent(subjectEntities, chapterEntities, freezeEntities, nowEpochMs, prefs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudyContent(),
        )

    override val sessions: StateFlow<List<StudySessionRecord>> = combine(
        studySessionDao.observeAll(),
        content,
    ) { sessionEntities, snapshot ->
        val chaptersById = snapshot.chapters.associateBy { it.id }
        sessionEntities.map { entity ->
            val chapter = chaptersById[entity.chapterId]
            StudySessionRecord(
                id = entity.id,
                chapterId = entity.chapterId,
                chapterTitle = chapter?.title ?: "Archived chapter",
                subjectName = chapter?.subjectName ?: "",
                startedAtEpochMs = entity.startedAtEpochMs,
                endedAtEpochMs = entity.endedAtEpochMs,
                durationMinutes = entity.durationMinutes,
                mode = entity.mode,
                batteryBefore = entity.batteryBefore,
                batteryAfter = entity.batteryAfter,
                stabilityBefore = entity.stabilityBefore,
                stabilityAfter = entity.stabilityAfter,
                recallRating = entity.recallRating,
                note = entity.sessionNote,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
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
                    breadcrumbNote = charge.note?.takeIf { it.isNotBlank() }
                        ?: chapter.breadcrumbNote,
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
        if (saved) runCatching { widgetUpdater.refresh() }
        return saved
    }


    override suspend fun importSyllabus(draft: SyllabusDraft): Int {
        val selected = draft.selectedOnly()
        if (selected.subjects.isEmpty()) return 0
        var imported = 0
        database.withTransaction {
            val existingSubjects = subjectDao.getAll()
                .associateByTo(mutableMapOf()) { it.name.trim().lowercase() }
            val existingChapters = chapterDao.getAll()
                .mapTo(mutableSetOf()) {
                    Triple(it.subjectId, it.unitName?.trim()?.lowercase(), it.title.trim().lowercase())
                }
            var nextSortOrder = (existingSubjects.values.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val now = clock.nowEpochMs()

            selected.subjects.forEach { subject ->
                val subjectKey = subject.name.trim().lowercase()
                val existingSubject = existingSubjects[subjectKey]
                val subjectId = if (existingSubject != null) {
                    existingSubject.id
                } else {
                    val entity = SubjectEntity(
                        name = subject.name.trim(),
                        colorHex = subject.colorHex,
                        examWeight = subject.examWeight.coerceIn(0.1f, 10f),
                        sortOrder = nextSortOrder,
                        createdAtEpochMs = now,
                    )
                    val id = subjectDao.upsert(entity)
                    existingSubjects[subjectKey] = entity.copy(id = id)
                    nextSortOrder += 1
                    id
                }

                subject.chapters.forEach { chapter ->
                    val chapterKey = Triple(
                        subjectId,
                        chapter.unitName?.trim()?.lowercase(),
                        chapter.title.trim().lowercase(),
                    )
                    if (chapterKey !in existingChapters) {
                        chapterDao.upsert(
                            ChapterEntity(
                                subjectId = subjectId,
                                unitName = chapter.unitName?.trim()?.ifBlank { null },
                                title = chapter.title.trim(),
                                status = ChapterStatus.ACTIVE,
                                difficultyWeight = chapter.difficulty.weight,
                                targetMinutes = chapter.targetMinutes.coerceIn(5, 240),
                                lastChargedAtEpochMs = null,
                                lastBatteryLevel = 0f,
                                stabilityDays = 3f,
                                reviewCount = 0,
                                totalMinutesStudied = 0,
                                breadcrumbNote = null,
                            ),
                        )
                        existingChapters += chapterKey
                        imported += 1
                    }
                }
            }
        }
        if (imported > 0) runCatching { widgetUpdater.refresh() }
        return imported
    }

    override suspend fun updateChapterStatus(chapterIds: Set<Long>, status: ChapterStatus) {
        if (chapterIds.isEmpty()) return
        chapterDao.updateStatus(chapterIds.toList(), status)
        runCatching { widgetUpdater.refresh() }
    }

    override suspend fun updateChapterTarget(chapterIds: Set<Long>, targetMinutes: Int) {
        if (chapterIds.isEmpty()) return
        chapterDao.updateTargetMinutes(chapterIds.toList(), targetMinutes.coerceIn(5, 240))
    }

    override suspend fun exportCsv(): String {
        val snapshot = content.value
        return SyllabusCsv.encode(snapshot.subjects, snapshot.chapters)
    }

    override suspend fun startCryoFreeze(hours: Int?, note: String?) {
        val now = clock.nowEpochMs()
        database.withTransaction {
            val active = cryoFreezeDao.getAll().firstOrNull {
                it.endEpochMs == null || it.endEpochMs > now
            }
            if (active != null) {
                cryoFreezeDao.upsert(active.copy(endEpochMs = now))
            }
            cryoFreezeDao.upsert(
                CryoFreezeEntity(
                    startEpochMs = now,
                    endEpochMs = hours?.let { now + it.coerceAtLeast(1) * 3_600_000L },
                    reasonNote = note?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
        preferences.setCryoFreezeActive(true)
        runCatching { widgetUpdater.refresh() }
    }

    override suspend fun extendCryoFreeze(hours: Int) {
        val now = clock.nowEpochMs()
        database.withTransaction {
            val active = cryoFreezeDao.getAll().firstOrNull {
                it.endEpochMs == null || it.endEpochMs > now
            } ?: return@withTransaction
            if (active.endEpochMs == null) return@withTransaction
            cryoFreezeDao.upsert(
                active.copy(endEpochMs = active.endEpochMs + hours.coerceAtLeast(1) * 3_600_000L),
            )
        }
        preferences.setCryoFreezeActive(true)
        runCatching { widgetUpdater.refresh() }
    }

    override suspend fun endCryoFreeze() {
        val now = clock.nowEpochMs()
        database.withTransaction {
            val active = cryoFreezeDao.getAll().firstOrNull {
                it.endEpochMs == null || it.endEpochMs > now
            }
            if (active != null) {
                cryoFreezeDao.upsert(active.copy(endEpochMs = now))
            }
        }
        preferences.setCryoFreezeActive(false)
        runCatching { widgetUpdater.refresh() }
    }

    override suspend fun purgeLegacyDemoData(): Boolean {
        var purged = false
        database.withTransaction {
            val subjects = subjectDao.getAll()
            val chapters = chapterDao.getAll()
            val matchesSubjects = subjects.size == LEGACY_DEMO_SUBJECTS.size &&
                subjects.all { LEGACY_DEMO_SUBJECTS[it.id] == it.name }
            val matchesChapters = chapters.size == LEGACY_DEMO_CHAPTERS.size &&
                chapters.all { LEGACY_DEMO_CHAPTERS[it.id] == it.title }
            if (matchesSubjects && matchesChapters) {
                chapterDao.deleteByIds(chapters.map { it.id })
                subjectDao.deleteByIds(subjects.map { it.id })
                purged = true
            }
        }
        return purged
    }

    private fun buildContent(
        subjectEntities: List<SubjectEntity>,
        chapterEntities: List<ChapterEntity>,
        freezeEntities: List<CryoFreezeEntity>,
        nowEpochMs: Long,
        prefs: UserPreferences,
    ): StudyContent {
        val subjects = subjectEntities.map { it.toDomain() }
        val subjectsById = subjects.associateBy { it.id }
        val freezes = freezeEntities.map {
            CryoFreezeInterval(startEpochMs = it.startEpochMs, endEpochMs = it.endEpochMs)
        }
        val activeFreeze = freezeEntities
            .filter { it.startEpochMs <= nowEpochMs && (it.endEpochMs == null || it.endEpochMs > nowEpochMs) }
            .maxByOrNull { it.startEpochMs }
            ?.let {
                CryoFreezeStatus(
                    startEpochMs = it.startEpochMs,
                    endEpochMs = it.endEpochMs,
                    isActive = true,
                )
            }
        val daysUntilExam = prefs.examDateEpochMs?.let {
            (it - nowEpochMs).toDouble() / DecayCalculator.MILLIS_PER_DAY
        }?.toFloat()
        val chapters = chapterEntities.mapNotNull { entity ->
            val subject = subjectsById[entity.subjectId] ?: return@mapNotNull null
            val crunch = DecayCalculator.examCrunchFactor(daysUntilExam, subject.examWeight)
            val decay = DecayCalculator.snapshot(
                lastChargedAtEpochMs = entity.lastChargedAtEpochMs,
                nowEpochMs = nowEpochMs,
                freezes = freezes,
                lastBatteryLevel = entity.lastBatteryLevel,
                status = entity.status,
                stabilityDays = entity.stabilityDays,
                difficultyWeight = entity.difficultyWeight,
                examCrunchFactor = crunch,
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
                subjectExamWeight = subject.examWeight,
            )
        }
        return StudyContent(
            subjects = subjects,
            chapters = chapters,
            activeFreeze = activeFreeze,
        )
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

        val LEGACY_DEMO_SUBJECTS = mapOf(
            1L to "Physiology",
            2L to "Pathology",
            3L to "Pharmacology",
            4L to "Anatomy",
            5L to "Biochemistry",
        )

        val LEGACY_DEMO_CHAPTERS = mapOf(
            1L to "Renal Regulation",
            2L to "Coronary Circulation",
            3L to "Cardiac Cycle",
            4L to "Acute Inflammation",
            5L to "Neoplasia Classification",
            6L to "Cell Injury",
            7L to "Beta Blockers",
            8L to "Antimicrobial Spectrum",
            9L to "Pharmacokinetics",
            10L to "Brachial Plexus",
            11L to "Cranial Nerves",
            12L to "Glycolysis Regulation",
            13L to "Cardiovascular Pathology",
        )
    }
}
