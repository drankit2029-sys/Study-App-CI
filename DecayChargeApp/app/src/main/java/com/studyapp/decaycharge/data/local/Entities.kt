package com.studyapp.decaycharge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.ChargeMode

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    @ColumnInfo(defaultValue = "1.0") val examWeight: Float = 1f,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index("status")],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val unitName: String?,
    val title: String,
    val status: ChapterStatus,
    val difficultyWeight: Float = 1f,
    @ColumnInfo(defaultValue = "45") val targetMinutes: Int = 45,
    val lastChargedAtEpochMs: Long? = null,
    @ColumnInfo(defaultValue = "0.0") val lastBatteryLevel: Float = 0f,
    @ColumnInfo(defaultValue = "3.0") val stabilityDays: Float = 3f,
    @ColumnInfo(defaultValue = "0") val reviewCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val totalMinutesStudied: Int = 0,
    val breadcrumbNote: String? = null,
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chapterId"), Index("startedAtEpochMs")],
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val durationMinutes: Int,
    val mode: ChargeMode,
    val batteryBefore: Float,
    val batteryAfter: Float,
    val stabilityBefore: Float,
    val stabilityAfter: Float,
    val recallRating: Int,
    val sessionNote: String?,
)

@Entity(
    tableName = "cryo_freezes",
    indices = [Index("startEpochMs")],
)
data class CryoFreezeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMs: Long,
    val endEpochMs: Long? = null,
    val reasonNote: String? = null,
)

class LocalConverters {
    @TypeConverter
    fun chapterStatusToString(value: ChapterStatus): String = value.name

    @TypeConverter
    fun stringToChapterStatus(value: String): ChapterStatus =
        runCatching { ChapterStatus.valueOf(value) }.getOrDefault(ChapterStatus.LOCKED)

    @TypeConverter
    fun chargeModeToString(value: ChargeMode): String = value.name

    @TypeConverter
    fun stringToChargeMode(value: String): ChargeMode =
        runCatching { ChargeMode.valueOf(value) }.getOrDefault(ChargeMode.DEEP_CHARGE)
}
