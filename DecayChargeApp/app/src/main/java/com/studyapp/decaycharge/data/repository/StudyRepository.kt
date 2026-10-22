package com.studyapp.decaycharge.data.repository

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.DemoSyllabus
import com.studyapp.decaycharge.model.Subject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface StudyRepository {
    val subjects: Flow<List<Subject>>
    val chapters: Flow<List<Chapter>>

    fun subject(subjectId: Long): Subject?
    fun chapter(chapterId: Long): Chapter?
}

@Singleton
class OfflineStudyRepository @Inject constructor() : StudyRepository {
    override val subjects: Flow<List<Subject>> = flowOf(DemoSyllabus.subjects)
    override val chapters: Flow<List<Chapter>> = flowOf(DemoSyllabus.chapters)

    override fun subject(subjectId: Long): Subject? =
        DemoSyllabus.subjects.firstOrNull { it.id == subjectId }

    override fun chapter(chapterId: Long): Chapter? =
        DemoSyllabus.chapters.firstOrNull { it.id == chapterId }
}
