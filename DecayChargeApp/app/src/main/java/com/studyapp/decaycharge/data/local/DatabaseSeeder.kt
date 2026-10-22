package com.studyapp.decaycharge.data.local

import androidx.room.withTransaction
import com.studyapp.decaycharge.domain.AppClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: DecayChargeDatabase,
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val clock: AppClock,
) {
    suspend fun seedIfNeeded(): Boolean {
        var seeded = false
        database.withTransaction {
            if (subjectDao.count() > 0) return@withTransaction
            val now = clock.nowEpochMs()
            subjectDao.upsertAll(SampleSyllabus.subjects(now))
            chapterDao.upsertAll(SampleSyllabus.chapters(now))
            seeded = true
        }
        return seeded
    }
}
