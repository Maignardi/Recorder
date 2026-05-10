package com.example.recorder.di

import com.example.recorder.data.repository.FolderRepositoryImpl
import com.example.recorder.data.repository.RecordingRepositoryImpl
import com.example.recorder.domain.repository.FolderRepository
import com.example.recorder.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository
}
