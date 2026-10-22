package com.studyapp.decaycharge.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY sortOrder, name")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(subject: SubjectEntity): Long

    @Upsert
    suspend fun upsertAll(subjects: List<SubjectEntity>)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters ORDER BY lastBatteryLevel ASC")
    fun observeAll(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun observe(chapterId: Long): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getById(chapterId: Long): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(chapter: ChapterEntity): Long

    @Upsert
    suspend fun upsertAll(chapters: List<ChapterEntity>)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Insert
    suspend fun insert(session: StudySessionEntity): Long
}

@Dao
interface CryoFreezeDao {
    @Query("SELECT * FROM cryo_freezes ORDER BY startEpochMs DESC")
    fun observeAll(): Flow<List<CryoFreezeEntity>>

    @Query("SELECT * FROM cryo_freezes WHERE endEpochMs IS NULL ORDER BY startEpochMs DESC LIMIT 1")
    suspend fun getActive(): CryoFreezeEntity?

    @Upsert
    suspend fun upsert(freeze: CryoFreezeEntity): Long
}

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        StudySessionEntity::class,
        CryoFreezeEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LocalConverters::class)
abstract class DecayChargeDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun cryoFreezeDao(): CryoFreezeDao
}
