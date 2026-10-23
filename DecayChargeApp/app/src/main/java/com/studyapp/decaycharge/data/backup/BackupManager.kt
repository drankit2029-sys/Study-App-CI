package com.studyapp.decaycharge.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.studyapp.decaycharge.data.local.ChapterEntity
import com.studyapp.decaycharge.data.local.CryoFreezeEntity
import com.studyapp.decaycharge.data.local.DecayChargeDatabase
import com.studyapp.decaycharge.data.local.StudySessionEntity
import com.studyapp.decaycharge.data.local.SubjectEntity
import com.studyapp.decaycharge.data.prefs.UserPreferencesRepository
import com.studyapp.decaycharge.domain.ActiveChargeController
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtEpochMs: Long,
    val subjects: List<BackupSubject>,
    val chapters: List<BackupChapter>,
    val sessions: List<BackupSession>,
    val freezes: List<BackupFreeze>,
    val preferences: BackupPreferences,
)

@Serializable
data class BackupSubject(
    val id: Long,
    val name: String,
    val colorHex: String,
    val examWeight: Float,
    val sortOrder: Int,
    val createdAtEpochMs: Long,
)

@Serializable
data class BackupChapter(
    val id: Long,
    val subjectId: Long,
    val unitName: String?,
    val title: String,
    val status: String,
    val difficultyWeight: Float,
    val targetMinutes: Int,
    val lastChargedAtEpochMs: Long?,
    val lastBatteryLevel: Float,
    val stabilityDays: Float,
    val reviewCount: Int,
    val totalMinutesStudied: Int,
    val breadcrumbNote: String?,
)

@Serializable
data class BackupSession(
    val id: Long,
    val chapterId: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val durationMinutes: Int,
    val mode: String,
    val batteryBefore: Float,
    val batteryAfter: Float,
    val stabilityBefore: Float,
    val stabilityAfter: Float,
    val recallRating: Int,
    val sessionNote: String?,
)

@Serializable
data class BackupFreeze(
    val id: Long,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val reasonNote: String?,
)

@Serializable
data class BackupPreferences(
    val onboardingCompleted: Boolean = true,
    val examDateEpochMs: Long? = null,
    val dndEnabled: Boolean = false,
    val dailyBudgetMinutes: Int = 120,
    val keepScreenOn: Boolean = true,
    val strictFocusEnabled: Boolean = false,
    val fullScreenAlertsEnabled: Boolean = false,
    val digestEnabled: Boolean = true,
    val digestHour: Int = 8,
    val digestMinute: Int = 0,
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: DecayChargeDatabase,
    private val preferences: UserPreferencesRepository,
    private val widgetUpdater: WidgetUpdater,
    private val activeChargeController: ActiveChargeController,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = database.withTransaction {
                val prefs = preferences.preferences.first()
                BackupPayload(
                    exportedAtEpochMs = System.currentTimeMillis(),
                    subjects = database.subjectDao().getAll().map { it.toBackup() },
                    chapters = database.chapterDao().getAll().map { it.toBackup() },
                    sessions = database.studySessionDao().getAll().map { it.toBackup() },
                    freezes = database.cryoFreezeDao().getAll().map { it.toBackup() },
                    preferences = BackupPreferences(
                        onboardingCompleted = prefs.onboardingCompleted,
                        examDateEpochMs = prefs.examDateEpochMs,
                        dndEnabled = prefs.dndEnabled,
                        dailyBudgetMinutes = prefs.dailyBudgetMinutes,
                        keepScreenOn = prefs.keepScreenOn,
                        strictFocusEnabled = prefs.strictFocusEnabled,
                        fullScreenAlertsEnabled = prefs.fullScreenAlertsEnabled,
                        digestEnabled = prefs.digestEnabled,
                        digestHour = prefs.digestHour,
                        digestMinute = prefs.digestMinute,
                    ),
                )
            }
            val bytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("Could not open the selected backup file.")
            stream.use { it.write(bytes) }
            payload.chapters.size
        }
    }

    suspend fun restore(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not read the selected backup file.")
            val payload = json.decodeFromString<BackupPayload>(raw)
            require(payload.version == 1) { "This backup was created by a newer app version." }
            val chapterIds = payload.chapters.mapTo(mutableSetOf()) { it.id }
            database.withTransaction {
                database.studySessionDao().clearAll()
                database.chapterDao().clearAll()
                database.subjectDao().clearAll()
                database.cryoFreezeDao().clearAll()
                database.subjectDao().upsertAll(payload.subjects.map { it.toEntity() })
                database.chapterDao().upsertAll(payload.chapters.map { it.toEntity() })
                database.studySessionDao().insertAll(
                    payload.sessions
                        .filter { it.chapterId in chapterIds }
                        .map { it.toEntity() },
                )
                payload.freezes.forEach { database.cryoFreezeDao().upsert(it.toEntity()) }
            }
            val prefs = payload.preferences
            activeChargeController.clear()
            preferences.restorePreferences(
                onboardingCompleted = true,
                examDateEpochMs = prefs.examDateEpochMs,
                dndEnabled = prefs.dndEnabled,
                dailyBudgetMinutes = prefs.dailyBudgetMinutes,
                keepScreenOn = prefs.keepScreenOn,
                strictFocusEnabled = prefs.strictFocusEnabled,
                fullScreenAlertsEnabled = prefs.fullScreenAlertsEnabled,
                digestEnabled = prefs.digestEnabled,
                digestHour = prefs.digestHour,
                digestMinute = prefs.digestMinute,
            )
            widgetUpdater.refresh()
            payload.chapters.size
        }
    }

    private fun SubjectEntity.toBackup() = BackupSubject(
        id, name, colorHex, examWeight, sortOrder, createdAtEpochMs,
    )

    private fun BackupSubject.toEntity() = SubjectEntity(
        id = id,
        name = name,
        colorHex = colorHex,
        examWeight = examWeight,
        sortOrder = sortOrder,
        createdAtEpochMs = createdAtEpochMs,
    )

    private fun ChapterEntity.toBackup() = BackupChapter(
        id, subjectId, unitName, title, status.name, difficultyWeight, targetMinutes,
        lastChargedAtEpochMs, lastBatteryLevel, stabilityDays, reviewCount,
        totalMinutesStudied, breadcrumbNote,
    )

    private fun BackupChapter.toEntity() = ChapterEntity(
        id = id,
        subjectId = subjectId,
        unitName = unitName,
        title = title,
        status = runCatching { ChapterStatus.valueOf(status) }.getOrDefault(ChapterStatus.LOCKED),
        difficultyWeight = difficultyWeight,
        targetMinutes = targetMinutes,
        lastChargedAtEpochMs = lastChargedAtEpochMs,
        lastBatteryLevel = lastBatteryLevel,
        stabilityDays = stabilityDays,
        reviewCount = reviewCount,
        totalMinutesStudied = totalMinutesStudied,
        breadcrumbNote = breadcrumbNote,
    )

    private fun StudySessionEntity.toBackup() = BackupSession(
        id, chapterId, startedAtEpochMs, endedAtEpochMs, durationMinutes, mode.name,
        batteryBefore, batteryAfter, stabilityBefore, stabilityAfter, recallRating, sessionNote,
    )

    private fun BackupSession.toEntity() = StudySessionEntity(
        id = id,
        chapterId = chapterId,
        startedAtEpochMs = startedAtEpochMs,
        endedAtEpochMs = endedAtEpochMs,
        durationMinutes = durationMinutes,
        mode = runCatching { ChargeMode.valueOf(mode) }.getOrDefault(ChargeMode.DEEP_CHARGE),
        batteryBefore = batteryBefore,
        batteryAfter = batteryAfter,
        stabilityBefore = stabilityBefore,
        stabilityAfter = stabilityAfter,
        recallRating = recallRating,
        sessionNote = sessionNote,
    )

    private fun CryoFreezeEntity.toBackup() = BackupFreeze(
        id, startEpochMs, endEpochMs, reasonNote,
    )

    private fun BackupFreeze.toEntity() = CryoFreezeEntity(
        id = id,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        reasonNote = reasonNote,
    )
}
