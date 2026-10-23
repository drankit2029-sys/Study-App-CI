package com.studyapp.decaycharge.di

import android.content.Context
import androidx.room.Room
import com.studyapp.decaycharge.data.local.ChapterDao
import com.studyapp.decaycharge.data.local.CryoFreezeDao
import com.studyapp.decaycharge.data.local.DecayChargeDatabase
import com.studyapp.decaycharge.data.local.StudySessionDao
import com.studyapp.decaycharge.data.local.SubjectDao
import com.studyapp.decaycharge.data.repository.OfflineTriageRepository
import com.studyapp.decaycharge.data.repository.TriageRepository
import com.studyapp.decaycharge.domain.AppClock
import com.studyapp.decaycharge.domain.SystemAppClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    @Singleton
    abstract fun bindAppClock(clock: SystemAppClock): AppClock

    @Binds
    @Singleton
    abstract fun bindTriageRepository(repository: OfflineTriageRepository): TriageRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DecayChargeDatabase =
        Room.databaseBuilder(
            context,
            DecayChargeDatabase::class.java,
            "decaycharge.db",
        ).build()

    @Provides
    fun provideSubjectDao(database: DecayChargeDatabase): SubjectDao = database.subjectDao()

    @Provides
    fun provideChapterDao(database: DecayChargeDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideStudySessionDao(database: DecayChargeDatabase): StudySessionDao =
        database.studySessionDao()

    @Provides
    fun provideCryoFreezeDao(database: DecayChargeDatabase): CryoFreezeDao =
        database.cryoFreezeDao()
}


@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun triageRepository(): TriageRepository
}
